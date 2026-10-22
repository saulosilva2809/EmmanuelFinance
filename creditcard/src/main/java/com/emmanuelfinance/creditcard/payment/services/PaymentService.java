package com.emmanuelfinance.creditcard.payment.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.creditcard.payment.dto.PaymentPartialDTO;
import com.emmanuelfinance.creditcard.payment.dto.PaymentTotalDTO;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentDomainException;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentErrorCode;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InvoiceSelector invoiceSelector;
    private final InvoiceRepository invoiceRepository;
    private final AccountClientCacheService accountClientCacheService;
    private final AccountOwnershipValidator accountOwnershipValidator;
    private final SecurityUtils securityUtils;
    private final CreditCardSelector creditCardSelector;
    private final CreditCardRepository creditCardRepository;
    private final OutboxService outboxService;

    private void validateInvoice(Invoice invoice, UUID userId, UUID creditCardId) {
        boolean belongsToUserAndCard = userId.equals(invoice.getUserId())
                && creditCardId.equals(invoice.getCreditCardId());

        // se o userId não é o mesmo de invoice ou creditCard
        if (!belongsToUserAndCard) {
            throw new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND);
        }

        // se já esta paga
        if (InvoiceStatusEnum.PAID.equals(invoice.getStatus())) {
            throw new PaymentDomainException(PaymentErrorCode.INVOICE_ALREADY_PAID);
        }

        // se o valor é 0
        if (invoice.getTotalAmount() == null || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVOICE_NOTHING_TO_PAY);
        }
    }

    private void giveLimitBack(CreditCard creditCard, BigDecimal amount) {
        // limite atual
        BigDecimal currentLimit = creditCard.getAvailableLimit() != null ? creditCard.getAvailableLimit() : BigDecimal.ZERO;
        // novo limite = limite atual + amount (amount pode ser o valor total da fatura ou um valor parcial)
        BigDecimal newLimit = currentLimit.add(amount);

        // verifica se o novo limite seria maior que o limite máximo do cartão
        if (creditCard.getCreditLimit() != null && newLimit.compareTo(creditCard.getCreditLimit()) > 0) {
            // atribui o novo limite ao limite do cartão
            newLimit = creditCard.getCreditLimit();
        }

        creditCard.setAvailableLimit(newLimit);
        creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(creditCard);
    }

    private void baseInvoicePayment(
            UUID creditCardId,
            Invoice invoice,
            UUID accountId,
            BigDecimal amount
    ) {
        UUID userId = securityUtils.getCurrentUserId();

        CreditCard creditCard = creditCardSelector.getCreditCardById(creditCardId, userId);

        // valida invoice
        validateInvoice(invoice, userId, creditCardId);

        // valida se o usuário fazendo a request é o dono da conta
        accountOwnershipValidator.validate(accountId);
        AccountSummaryInternalDTO accountSummary = accountClientCacheService.getInternalAccountById(accountId);

        // verifica se a conta tem saldo para fazer o pagamento da fatura
        if (accountSummary.currentBalance().compareTo(amount) < 0) {
            throw new PaymentDomainException(PaymentErrorCode.INSUFFICIENT_BALANCE);
        }

        // partial ou total
        invoice.setAmountPaid(amount);

        // se o valor total da fatura é igual o valor pago
        if (invoice.getTotalAmount().compareTo(invoice.getAmountPaid()) == 0) {
            // marca a fatura como paga
            invoice.setStatus(InvoiceStatusEnum.PAID);
        } else {
            // marca a fatura como aberta
            invoice.setStatus(InvoiceStatusEnum.OPEN);
        }

        invoiceRepository.saveAndFlush(invoice);
        outboxService.invoiceChanged(invoice);
        giveLimitBack(creditCard, amount);

        outboxService.invoicePayment(new InvoicePaymentDTO(
                accountId,
                userId,
                invoice.getCreditCardId(),
                creditCard.getName(),
                invoice.getId(),
                invoice.getMonth(),
                invoice.getYear(),
                amount
        ));
    }

    @Transactional
    public void totalInvoicePayment(UUID creditCardId, UUID invoiceId, PaymentTotalDTO data) {
        Invoice invoice = invoiceSelector.getByIdExcludingDeleted(invoiceId);

        baseInvoicePayment(creditCardId, invoice, data.accountId(), invoice.getTotalAmount());
    }

    @Transactional
    public void partialInvoicePayment(UUID creditCardId, UUID invoiceId, PaymentPartialDTO data) {
        Invoice invoice = invoiceSelector.getByIdExcludingDeleted(invoiceId);

        if (data.amount().compareTo(invoice.getTotalAmount()) > 0) {
            throw new PaymentDomainException(PaymentErrorCode.DATA_INVALID_VALUE);
        }

        baseInvoicePayment(creditCardId, invoice, data.accountId(), data.amount());
    }
}
