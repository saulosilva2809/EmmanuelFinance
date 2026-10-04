package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardBalanceKafkaServiceTest {

    @Mock
    private CreditCardSelector creditCardSelector;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private CreditCardRepository creditCardRepository;

    @InjectMocks
    private CreditCardBalanceKafkaService creditCardBalanceKafkaService;

    private final UUID userId = UUID.randomUUID();

    private CreditCard card(String availableLimit) {
        CreditCard card = CreditCardTestDataBuilder.createEntity(CreditCardTestDataBuilder.createCardDTO(), userId, false);
        card.setAvailableLimit(new BigDecimal(availableLimit));
        return card;
    }

    private TransactionDeletedAndRestoreEvent deletedOrRestoreEvent(UUID cardId, String amount) {
        TransactionDeletedAndRestoreEvent base = InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(UUID.randomUUID());
        return new TransactionDeletedAndRestoreEvent(
                base.transactionId(), base.accountId(), cardId, userId, new BigDecimal(amount),
                base.installmentsCount(), base.type(), base.status(), base.date()
        );
    }

    @Nested
    @DisplayName("Cenários do processTransactionCreate")
    class CreateTests {

        @Test
        @DisplayName("Deve subtrair o valor do limite disponível, salvar e criar/atualizar a fatura")
        void shouldSubtractAmountAndCreateInvoice() {
            CreditCard card = card("1000.00");
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, card.getId(), new BigDecimal("300.00"), 1, LocalDateTime.now()
            );
            when(creditCardSelector.getCreditCardByIdInternal(card.getId())).thenReturn(card);

            creditCardBalanceKafkaService.processTransactionCreate(event);

            assertEquals(new BigDecimal("700.00"), card.getAvailableLimit());
            InOrder inOrder = inOrder(creditCardRepository, invoiceService);
            inOrder.verify(creditCardRepository).saveAndFlush(card);
            inOrder.verify(invoiceService).findOrCreate(event);
        }

        @Test
        @DisplayName("Deve propagar CreditCardNotFound e não criar fatura quando o cartão não existir")
        void shouldPropagateWhenCardDoesNotExist() {
            UUID cardId = UUID.randomUUID();
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, cardId, BigDecimal.TEN, 1, LocalDateTime.now()
            );
            when(creditCardSelector.getCreditCardByIdInternal(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardBalanceKafkaService.processTransactionCreate(event));

            verifyNoInteractions(creditCardRepository, invoiceService);
        }
    }

    @Nested
    @DisplayName("Cenários do processTransactionDeletion")
    class DeletionTests {

        @Test
        @DisplayName("Deve devolver o valor ao limite disponível, salvar e ajustar as faturas")
        void shouldGiveAmountBackAndDeleteInvoices() {
            CreditCard card = card("700.00");
            TransactionDeletedAndRestoreEvent event = deletedOrRestoreEvent(card.getId(), "300.00");
            when(creditCardSelector.getCreditCardByIdInternal(card.getId())).thenReturn(card);

            creditCardBalanceKafkaService.processTransactionDeletion(event);

            assertEquals(new BigDecimal("1000.00"), card.getAvailableLimit());
            InOrder inOrder = inOrder(creditCardRepository, invoiceService);
            inOrder.verify(creditCardRepository).saveAndFlush(card);
            inOrder.verify(invoiceService).delete(event);
        }
    }

    @Nested
    @DisplayName("Cenários do processTransactionRestore")
    class RestoreTests {

        @Test
        @DisplayName("Deve subtrair o valor do limite disponível, salvar e restaurar as faturas")
        void shouldSubtractAmountAndRestoreInvoices() {
            CreditCard card = card("1000.00");
            TransactionDeletedAndRestoreEvent event = deletedOrRestoreEvent(card.getId(), "300.00");
            when(creditCardSelector.getCreditCardByIdInternal(card.getId())).thenReturn(card);

            creditCardBalanceKafkaService.processTransactionRestore(event);

            assertEquals(new BigDecimal("700.00"), card.getAvailableLimit());
            InOrder inOrder = inOrder(creditCardRepository, invoiceService);
            inOrder.verify(creditCardRepository).saveAndFlush(card);
            inOrder.verify(invoiceService).restore(event);
        }

        @Test
        @DisplayName("Deve propagar o erro e não restaurar faturas quando o cartão não existir")
        void shouldPropagateWhenCardDoesNotExist() {
            UUID cardId = UUID.randomUUID();
            TransactionDeletedAndRestoreEvent event = deletedOrRestoreEvent(cardId, "10.00");
            when(creditCardSelector.getCreditCardByIdInternal(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardBalanceKafkaService.processTransactionRestore(event));

            verifyNoInteractions(creditCardRepository, invoiceService);
        }
    }
}
