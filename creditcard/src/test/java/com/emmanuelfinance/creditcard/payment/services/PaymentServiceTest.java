package com.emmanuelfinance.creditcard.payment.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.creditcard.payment.dto.PaymentTotalDTO;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentDomainException;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentErrorCode;
import com.emmanuelfinance.creditcard.payment.kafka.PaymentProducer;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private InvoiceSelector invoiceSelector;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private AccountClientCacheService accountClientCacheService;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CreditCardSelector creditCardSelector;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @InjectMocks
    private PaymentService paymentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private CreditCard card;
    private PaymentTotalDTO paymentDTO;

    @BeforeEach
    void setUp() {
        card = CreditCardTestDataBuilder.createEntity(CreditCardTestDataBuilder.createCardDTO(), userId, false);
        card.setCreditLimit(new BigDecimal("10000.00"));
        card.setAvailableLimit(new BigDecimal("5000.00"));
        paymentDTO = new PaymentTotalDTO(accountId);
    }

    private Invoice invoice(UUID owner, UUID cardId, String total, InvoiceStatusEnum status) {
        return InvoiceTestDataBuilder.invoiceEntity(owner, cardId, 10, 2026, new BigDecimal(total), status, false);
    }

    private void stubBalance(String balance) {
        when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(
                new AccountSummaryInternalDTO(accountId, "Conta", BanksEnum.C6_BANK, new BigDecimal(balance), false)
        );
    }

    private void stubCardAndInvoice(Invoice invoice) {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(creditCardSelector.getCreditCardById(card.getId(), userId)).thenReturn(card);
        when(invoiceSelector.getByIdExcludingDeleted(invoice.getId())).thenReturn(invoice);
    }

    private void assertNothingWasChargedOrPublished() {
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(creditCardRepository, never()).save(any(CreditCard.class));
        verifyNoInteractions(paymentProducer);
    }

    @Nested
    @DisplayName("Pagamento total com sucesso")
    class SuccessTests {

        @Test
        @DisplayName("Deve marcar a fatura como paga, devolver o limite e publicar o evento com os dados da fatura")
        void shouldPayInvoiceAndPublishEvent() {
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);
            stubBalance("1000.00");

            paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO);

            assertEquals(InvoiceStatusEnum.PAID, invoice.getStatus());
            assertEquals(new BigDecimal("300.00"), invoice.getAmountPaid());
            assertEquals(new BigDecimal("5300.00"), card.getAvailableLimit());

            ArgumentCaptor<InvoicePaymentDTO> captor = ArgumentCaptor.forClass(InvoicePaymentDTO.class);
            InOrder inOrder = inOrder(invoiceRepository, paymentProducer);
            inOrder.verify(invoiceRepository).save(invoice);
            inOrder.verify(paymentProducer).publishInvoicePaymentCreated(captor.capture());

            InvoicePaymentDTO event = captor.getValue();
            assertEquals(accountId, event.accountId());
            assertEquals(userId, event.userId());
            assertEquals(card.getId(), event.creditCardId());
            assertEquals(card.getName(), event.creditCardName());
            assertEquals(invoice.getId(), event.invoiceId());
            assertEquals(10, event.month());
            assertEquals(2026, event.year());
            assertEquals(new BigDecimal("300.00"), event.totalPaid());
            verify(creditCardRepository, times(1)).save(card);
            verify(accountOwnershipValidator, times(1)).validate(accountId);
        }

        @Test
        @DisplayName("Deve aceitar saldo exatamente igual ao total da fatura")
        void shouldAcceptBalanceEqualToTotal() {
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.CLOSED);
            stubCardAndInvoice(invoice);
            stubBalance("300.00");

            assertDoesNotThrow(() -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertEquals(InvoiceStatusEnum.PAID, invoice.getStatus());
        }

        @Test
        @DisplayName("Não deve passar o limite disponível do limite total do cartão")
        void shouldCapAvailableLimitAtCreditLimit() {
            card.setAvailableLimit(new BigDecimal("9900.00"));
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);
            stubBalance("1000.00");

            paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO);

            assertEquals(new BigDecimal("10000.00"), card.getAvailableLimit());
        }
    }

    @Nested
    @DisplayName("Validações do pagamento")
    class ValidationTests {

        @Test
        @DisplayName("Deve lançar INSUFFICIENT_BALANCE (422) e não alterar nada quando o saldo não cobrir a fatura")
        void shouldThrowWhenBalanceIsInsufficient() {
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);
            stubBalance("299.99");

            PaymentDomainException exception = assertThrows(PaymentDomainException.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertEquals(PaymentErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
            assertEquals(InvoiceStatusEnum.OPEN, invoice.getStatus());
            assertEquals(new BigDecimal("5000.00"), card.getAvailableLimit());
            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar INVOICE_NOT_FOUND quando a fatura for de outro usuário")
        void shouldThrowWhenInvoiceBelongsToAnotherUser() {
            Invoice invoice = invoice(UUID.randomUUID(), card.getId(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);

            InvoiceDomainException exception = assertThrows(InvoiceDomainException.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertEquals(InvoiceErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(accountClientCacheService, accountOwnershipValidator);
            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar INVOICE_NOT_FOUND quando a fatura for de outro cartão")
        void shouldThrowWhenInvoiceBelongsToAnotherCard() {
            Invoice invoice = invoice(userId, UUID.randomUUID(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);

            assertThrows(InvoiceDomainException.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar AccountNotFound e não consultar o saldo quando a conta não for do usuário")
        void shouldThrowWhenAccountDoesNotBelongToTheUser() {
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);
            doThrow(new AccountNotFound()).when(accountOwnershipValidator).validate(accountId);

            assertThrows(AccountNotFound.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            verifyNoInteractions(accountClientCacheService);
            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar INVOICE_ALREADY_PAID (409) quando a fatura já estiver paga")
        void shouldThrowWhenInvoiceIsAlreadyPaid() {
            Invoice invoice = invoice(userId, card.getId(), "300.00", InvoiceStatusEnum.PAID);
            stubCardAndInvoice(invoice);

            PaymentDomainException exception = assertThrows(PaymentDomainException.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertEquals(PaymentErrorCode.INVOICE_ALREADY_PAID, exception.getErrorCode());
            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar INVOICE_NOTHING_TO_PAY (422) quando o total da fatura for zero")
        void shouldThrowWhenInvoiceHasNothingToPay() {
            Invoice invoice = invoice(userId, card.getId(), "0.00", InvoiceStatusEnum.OPEN);
            stubCardAndInvoice(invoice);

            PaymentDomainException exception = assertThrows(PaymentDomainException.class,
                    () -> paymentService.totalInvoicePayment(card.getId(), invoice.getId(), paymentDTO));

            assertEquals(PaymentErrorCode.INVOICE_NOTHING_TO_PAY, exception.getErrorCode());
            assertNothingWasChargedOrPublished();
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound e não buscar a fatura quando o cartão não for do usuário")
        void shouldThrowWhenCardDoesNotBelongToTheUser() {
            UUID cardId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardSelector.getCreditCardById(cardId, userId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class,
                    () -> paymentService.totalInvoicePayment(cardId, UUID.randomUUID(), paymentDTO));

            verifyNoInteractions(invoiceSelector, accountClientCacheService, paymentProducer);
        }
    }
}
