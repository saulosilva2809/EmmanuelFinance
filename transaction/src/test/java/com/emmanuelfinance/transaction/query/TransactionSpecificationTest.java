package com.emmanuelfinance.transaction.query;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSpecificationTest {

    private final UUID userId = UUID.randomUUID();

    private TransactionFiltersDTO noFilters() {
        return new TransactionFiltersDTO(null, null, null, null, null, null, null, null, null);
    }

    @Test
    void shouldAlwaysFilterByUserAndDeletedState() {
        List<Document> conditions = conditions(noFilters(), false);

        assertEquals(2, conditions.size());
        assertEquals(userId, valueOf(conditions, "userId"));
        assertEquals(false, valueOf(conditions, "deleted"));
    }

    @Test
    void shouldFilterOnlyDeletedTransactionsWhenAskedTo() {
        assertEquals(true, valueOf(conditions(noFilters(), true), "deleted"));
    }

    @Test
    void shouldWorkWithoutFilters() {
        List<Document> conditions = conditions(null, false);

        assertEquals(2, conditions.size());
    }

    @Test
    void shouldFilterByAccountCategoryCardTypeStatusAndScheduled() {
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID creditCardId = UUID.randomUUID();
        TransactionFiltersDTO filters = new TransactionFiltersDTO(
                accountId, categoryId, creditCardId, null, null, true, null,
                StatusTransactionEnum.PENDING, TypeEnum.EXPENSE
        );

        List<Document> conditions = conditions(filters, false);

        assertEquals(accountId, valueOf(conditions, "accountId"));
        assertEquals(categoryId, valueOf(conditions, "categoryId"));
        assertEquals(creditCardId, valueOf(conditions, "creditCardId"));
        assertEquals(true, valueOf(conditions, "scheduled"));
        assertEquals(StatusTransactionEnum.PENDING, valueOf(conditions, "status"));
        assertEquals(TypeEnum.EXPENSE, valueOf(conditions, "type"));
    }

    @Test
    void shouldFilterByMinimumAndMaximumAmountIncludingTheLimits() {
        TransactionFiltersDTO filters = new TransactionFiltersDTO(
                null, null, null, new BigDecimal("50"), new BigDecimal("200"), null, null, null, null
        );

        List<Document> conditions = conditions(filters, false);

        Document minimum = conditionOf(conditions, "amount", "$gte");
        Document maximum = conditionOf(conditions, "amount", "$lte");
        assertNotNull(minimum);
        assertNotNull(maximum);
    }

    @Test
    void shouldFilterTheWholeDayOfTheDate() {
        LocalDate day = LocalDate.of(2026, 10, 5);
        TransactionFiltersDTO filters = new TransactionFiltersDTO(
                null, null, null, null, null, null, day, null, null
        );

        Document dateCondition = (Document) valueOf(conditions(filters, false), "date");

        assertEquals(day.atStartOfDay(), dateCondition.get("$gte"));
        assertEquals(day.atTime(LocalTime.MAX), dateCondition.get("$lte"));
    }

    @SuppressWarnings("unchecked")
    private List<Document> conditions(TransactionFiltersDTO filters, boolean onlyDeleted) {
        Query query = TransactionSpecification.withFilter(filters, userId, onlyDeleted);
        return (List<Document>) query.getQueryObject().get("$and");
    }

    private Object valueOf(List<Document> conditions, String field) {
        return conditions.stream()
                .filter(condition -> condition.containsKey(field))
                .map(condition -> condition.get(field))
                .findFirst()
                .orElse(null);
    }

    // o valor mínimo e o máximo ficam na mesma condição do campo, cada um com o seu operador
    private Document conditionOf(List<Document> conditions, String field, String operator) {
        return conditions.stream()
                .filter(condition -> condition.get(field) instanceof Document inner && inner.containsKey(operator))
                .findFirst()
                .orElse(null);
    }
}
