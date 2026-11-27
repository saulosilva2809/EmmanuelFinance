package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceItemSelector;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditCardInternalService {

    private final CreditCardSelector cardSelector;
    private final CreditCardRepository creditCardRepository;
    private final OutboxService outboxService;
    private final InvoiceItemSelector invoiceItemSelector;
    private final InvoiceRepository invoiceRepository;
    private final SecurityUtils securityUtils;

    public CreditCardSummaryDTO getCreditCardSummary(UUID creditCardId) {
        // o resumo só mostra dados, então um cartão excluído também precisa aparecer
        CreditCard creditCard = cardSelector.getCreditCardByIdIncludingDeleted(creditCardId);

        return new CreditCardSummaryDTO(
                creditCard.getId(),
                creditCard.getName(),
                creditCard.isDeleted()
        );
    }

    public CreditCardInternalSummaryDTO getCreditCardInternalSummary(UUID creditCardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(creditCardId);

        return new CreditCardInternalSummaryDTO(
                creditCard.getId(),
                creditCard.getAccountId(),
                creditCard.getAvailableLimit(),
                creditCard.getDueDay(),
                creditCard.getClosingDay()
        );
    }

    // a compra já está paga quando, sem ela, alguma fatura ficaria menor do que o que já foi pago
    public boolean isPurchaseAlreadyPaid(UUID transactionId) {
        UUID userId = securityUtils.getCurrentUserId();

        // quanto essa compra pesa em cada fatura
        Map<UUID, BigDecimal> amountByInvoice = invoiceItemSelector.findByTransactionId(transactionId).stream()
                .filter(item -> userId.equals(item.getUserId()) && !item.isDeleted())
                .collect(Collectors.toMap(InvoiceItem::getInvoiceId, InvoiceItem::getAmount, BigDecimal::add));

        return invoiceRepository.findAllById(amountByInvoice.keySet()).stream()
                .anyMatch(invoice -> invoice.getTotalAmount()
                        .subtract(amountByInvoice.get(invoice.getId()))
                        .compareTo(invoice.getAmountPaid()) < 0);
    }

    @Transactional
    public void deactivateCardsByAccountId(UUID accountId) {
        // os cartões que o usuário já tinha excluído ficam como estão
        List<CreditCard> cards = cardSelector.findByAccountId(accountId).stream()
                .filter(card -> !card.isDeleted())
                .toList();

        cards.forEach(card -> {
            card.setDeleted(true);
            card.setDeactivatedByAccount(true);
            creditCardRepository.saveAndFlush(card);
            outboxService.creditCardDeleted(card);
        });
    }

    // a conta voltou: só voltam os cartões que saíram por causa dela
    @Transactional
    public void restoreCardsByAccountId(UUID accountId) {
        cardSelector.findDeactivatedByAccountId(accountId).forEach(card -> {
            card.setDeleted(false);
            card.setDeactivatedByAccount(false);
            creditCardRepository.saveAndFlush(card);
            outboxService.creditCardChanged(card);
        });
    }
}
