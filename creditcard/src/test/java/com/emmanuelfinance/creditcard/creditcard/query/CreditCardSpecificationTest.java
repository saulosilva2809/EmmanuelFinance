package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardSpecificationTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldAlwaysFilterByUserAndDeletedState() {
        List<Document> conditions = conditions(new CreditCardFiltersDTO(null, null, null), false);

        assertEquals(2, conditions.size());
        assertEquals(userId, valueOf(conditions, "userId"));
        assertEquals(false, valueOf(conditions, "deleted"));
    }

    @Test
    void shouldFilterOnlyDeletedCardsWhenAskedTo() {
        List<Document> conditions = conditions(new CreditCardFiltersDTO(null, null, null), true);

        assertEquals(true, valueOf(conditions, "deleted"));
    }

    @Test
    void shouldFilterByAccountAndBank() {
        UUID accountId = UUID.randomUUID();

        List<Document> conditions = conditions(new CreditCardFiltersDTO(accountId, null, BanksEnum.NUBANK), false);

        assertEquals(accountId, valueOf(conditions, "accountId"));
        assertEquals(BanksEnum.NUBANK, valueOf(conditions, "bank"));
    }

    @Test
    void shouldSearchTheNameIgnoringCase() {
        List<Document> conditions = conditions(new CreditCardFiltersDTO(null, "nubank", null), false);

        Pattern pattern = (Pattern) valueOf(conditions, "name");
        assertTrue(pattern.matcher("Meu cartão NUBANK").find());
    }

    @Test
    void shouldIgnoreABlankName() {
        List<Document> conditions = conditions(new CreditCardFiltersDTO(null, "   ", null), false);

        assertNull(valueOf(conditions, "name"));
    }

    @SuppressWarnings("unchecked")
    private List<Document> conditions(CreditCardFiltersDTO filters, boolean onlyDeleted) {
        Document query = CreditCardSpecification.withFilter(filters, userId, onlyDeleted).getQueryObject();
        return (List<Document>) query.get("$and");
    }

    private Object valueOf(List<Document> conditions, String field) {
        return conditions.stream()
                .filter(condition -> condition.containsKey(field))
                .map(condition -> condition.get(field))
                .findFirst()
                .orElse(null);
    }
}
