package com.emmanuelfinance.creditcard.payment.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
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

        if (!belongsToUserAndCard) {
            throw new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND);
        }

        if (InvoiceStatusEnum.PAID.equals(invoice.getStatus())) {
            throw new PaymentDomainException(PaymentErrorCode.INVOICE_ALREADY_PAID);
        }

        if (invoice.getTotalAmount() == null || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.INVOICE_NOTHING_TO_PAY);
        }
    }

    private void giveLimitBack(CreditCard creditCard, BigDecimal amount) {
        BigDecimal currentLimit = creditCard.getAvailableLimit() != null ? creditCard.getAvailableLimit() : BigDecimal.ZERO;
        BigDecimal newLimit = currentLimit.add(amount);

        if (creditCard.getCreditLimit() != null && newLimit.compareTo(creditCard.getCreditLimit()) > 0) {
            newLimit = creditCard.getCreditLimit();
        }

        creditCard.setAvailableLimit(newLimit);
        creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(creditCard);
    }

    @Transactional
    public void totalInvoicePayment(UUID creditCardId, UUID invoiceId, PaymentTotalDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        CreditCard creditCard = creditCardSelector.getCreditCardById(creditCardId, userId);
        Invoice invoice = invoiceSelector.getByIdExcludingDeleted(invoiceId);
        validateInvoice(invoice, userId, creditCardId);

        accountOwnershipValidator.validate(data.accountId());
        AccountSummaryInternalDTO accountSummary = accountClientCacheService.getInternalAccountById(data.accountId());

        BigDecimal totalAmount = invoice.getTotalAmount();
        if (accountSummary.currentBalance().compareTo(totalAmount) < 0) {
            throw new PaymentDomainException(PaymentErrorCode.INSUFFICIENT_BALANCE);
        }

        invoice.setAmountPaid(totalAmount);
        invoice.setStatus(InvoiceStatusEnum.PAID);

        invoiceRepository.saveAndFlush(invoice);
        outboxService.invoiceChanged(invoice);
        giveLimitBack(creditCard, totalAmount);

        outboxService.invoicePayment(new InvoicePaymentDTO(
                data.accountId(),
                userId,
                invoice.getCreditCardId(),
                creditCard.getName(),
                invoice.getId(),
                invoice.getMonth(),
                invoice.getYear(),
                totalAmount
        ));
    }
}
