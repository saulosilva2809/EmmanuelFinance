package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionTestDataBuilder {

    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("100.00");

    // ---------- CreateTransactionDTO ----------

    public static CreateTransactionDTO createDTO(
            UUID accountId,
            UUID categoryId,
            UUID creditCardId,
            BigDecimal amount,
            Integer installmentsCount,
            boolean scheduled,
            LocalDateTime date,
            TypeEnum type
    ) {
        return new CreateTransactionDTO(
                accountId,
                creditCardId,
                categoryId,
                "Compra no mercado",
                amount,
                installmentsCount,
                scheduled,
                date,
                type
        );
    }

    /** Despesa simples de conta: não agendada, sem data e sem cartão. */
    public static CreateTransactionDTO createDTO(UUID accountId, UUID categoryId) {
        return createDTO(accountId, categoryId, null, DEFAULT_AMOUNT, 1, false, null, TypeEnum.EXPENSE);
    }

    public static CreateTransactionDTO createDTO() {
        return createDTO(UUID.randomUUID(), UUID.randomUUID());
    }

    public static CreateTransactionDTO incomeDTO(UUID accountId, UUID categoryId) {
        return createDTO(accountId, categoryId, null, DEFAULT_AMOUNT, 1, false, null, TypeEnum.INCOME);
    }

    public static CreateTransactionDTO cardDTO(UUID accountId, UUID categoryId, UUID creditCardId, int installments) {
        return createDTO(accountId, categoryId, creditCardId, DEFAULT_AMOUNT, installments, false, null, TypeEnum.EXPENSE);
    }

    public static CreateTransactionDTO scheduledDTO(UUID accountId, UUID categoryId, LocalDateTime date) {
        return createDTO(accountId, categoryId, null, DEFAULT_AMOUNT, 1, true, date, TypeEnum.EXPENSE);
    }

    // ---------- UpdateTransactionDTO ----------

    public static UpdateTransactionDTO updateDTO(
            UUID accountId,
            UUID categoryId,
            String description,
            BigDecimal amount,
            Boolean scheduled,
            LocalDateTime date,
            TypeEnum type
    ) {
        return new UpdateTransactionDTO(accountId, categoryId, description, amount, scheduled, date, type);
    }

    /** Update vazio: nenhum campo informado. */
    public static UpdateTransactionDTO emptyUpdateDTO() {
        return updateDTO(null, null, null, null, null, null, null);
    }

    public static UpdateTransactionDTO updateDTO() {
        return updateDTO(null, null, "Descrição atualizada", new BigDecimal("150.00"), null, null, null);
    }

    // ---------- Transaction (entidade) ----------

    public static Transaction transactionEntity(
            UUID userId,
            UUID accountId,
            UUID categoryId,
            UUID creditCardId,
            BigDecimal amount,
            StatusTransactionEnum status,
            boolean scheduled,
            LocalDateTime date,
            TypeEnum type,
            boolean deleted
    ) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setAccountId(accountId);
        transaction.setCategoryId(categoryId);
        transaction.setCreditCardId(creditCardId);
        transaction.setDescription("Compra no mercado");
        transaction.setAmount(amount);
        transaction.setInstallmentsCount(1);
        transaction.setScheduled(scheduled);
        transaction.setDate(date);
        transaction.setStatus(status);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(null);
        transaction.setDeleted(deleted);
        return transaction;
    }

    /** Despesa paga de conta, não agendada, não excluída. */
    public static Transaction transactionEntity(UUID userId) {
        return transactionEntity(
                userId, UUID.randomUUID(), UUID.randomUUID(), null, DEFAULT_AMOUNT,
                StatusTransactionEnum.PAID, false, LocalDateTime.now(), TypeEnum.EXPENSE, false
        );
    }

    public static Transaction transactionEntity(UUID userId, boolean deleted) {
        Transaction transaction = transactionEntity(userId);
        transaction.setDeleted(deleted);
        return transaction;
    }

    public static Transaction cardTransactionEntity(UUID userId, UUID accountId, UUID creditCardId) {
        return transactionEntity(
                userId, accountId, UUID.randomUUID(), creditCardId, DEFAULT_AMOUNT,
                StatusTransactionEnum.PAID, false, LocalDateTime.now(), TypeEnum.EXPENSE, false
        );
    }

    public static Transaction pendingTransactionEntity(UUID userId, LocalDateTime date) {
        return transactionEntity(
                userId, UUID.randomUUID(), UUID.randomUUID(), null, DEFAULT_AMOUNT,
                StatusTransactionEnum.PENDING, true, date, TypeEnum.EXPENSE, false
        );
    }

    /** Aplica o que o mapper faria em {@code toEntity}: copia o DTO para a entidade, sem id/userId/status. */
    public static Transaction entityFromDTO(CreateTransactionDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(dto.accountId());
        transaction.setCategoryId(dto.categoryId());
        transaction.setCreditCardId(dto.creditCardId());
        transaction.setDescription(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setInstallmentsCount(dto.installmentsCount());
        transaction.setScheduled(dto.scheduled());
        transaction.setDate(dto.date());
        transaction.setType(dto.type());
        return transaction;
    }

    // ---------- DTOs de resposta e resumos ----------

    public static AccountSummaryDTO accountSummaryDTO(UUID accountId) {
        return new AccountSummaryDTO(accountId, "Conta Corrente", false);
    }

    public static CategorySummaryDTO categorySummaryDTO(UUID categoryId) {
        return new CategorySummaryDTO(categoryId, "Alimentação", false);
    }

    public static CreditCardSummaryDTO creditCardSummaryDTO(UUID creditCardId) {
        return new CreditCardSummaryDTO(creditCardId, "Cartão de Crédito C6", false);
    }

    public static CategoryInternalSummaryDTO categoryInternalSummaryDTO(UUID categoryId, TypeEnum type) {
        return new CategoryInternalSummaryDTO(categoryId, type);
    }

    public static CreditCardInternalSummaryDTO creditCardInternalSummaryDTO(
            UUID creditCardId,
            UUID accountId,
            BigDecimal availableLimit
    ) {
        return new CreditCardInternalSummaryDTO(creditCardId, accountId, availableLimit, 24, 17);
    }

    public static AccountSummaryInternalDTO accountSummaryInternalDTO(UUID accountId, BigDecimal currentBalance) {
        return new AccountSummaryInternalDTO(accountId, "Conta Corrente", BanksEnum.C6_BANK, currentBalance, false);
    }

    public static ResponseTransactionDTO responseDTO(Transaction transaction) {
        return new ResponseTransactionDTO(
                transaction.getId(),
                accountSummaryDTO(transaction.getAccountId()),
                transaction.getCreditCardId() != null ? creditCardSummaryDTO(transaction.getCreditCardId()) : null,
                categorySummaryDTO(transaction.getCategoryId()),
                transaction.getRecurringId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getInstallmentsCount(),
                transaction.isScheduled(),
                transaction.getDate(),
                transaction.getStatus(),
                transaction.getType(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.isDeleted()
        );
    }
}
