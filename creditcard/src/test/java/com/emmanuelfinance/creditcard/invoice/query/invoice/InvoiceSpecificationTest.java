package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceSpecificationTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldAlwaysFilterByUserAndNotDeleted() {
        List<Document> conditions = conditions(new InvoiceFiltersDTO(null, null, null, null));

        assertEquals(2, conditions.size());
        assertEquals(userId, valueOf(conditions, "userId"));
        assertEquals(false, valueOf(conditions, "deleted"));
    }

    @Test
    void shouldFilterByCardMonthYearAndStatus() {
        UUID creditCardId = UUID.randomUUID();

        List<Document> conditions = conditions(new InvoiceFiltersDTO(creditCardId, 10, 2026, InvoiceStatusEnum.OPEN));

        assertEquals(creditCardId, valueOf(conditions, "creditCardId"));
        assertEquals(10, valueOf(conditions, "month"));
        assertEquals(2026, valueOf(conditions, "year"));
        assertEquals(InvoiceStatusEnum.OPEN, valueOf(conditions, "status"));
    }

    @SuppressWarnings("unchecked")
    private List<Document> conditions(InvoiceFiltersDTO filters) {
        Query query = InvoiceSpecification.withFilter(filters, userId);
        return (List<Document>) query.getQueryObject().get("$and");
    }

    private Object valueOf(List<Document> conditions, String field) {
        return conditions.stream()
                .filter(condition -> condition.containsKey(field))
                .findFirst()
                .orElseThrow()
                .get(field);
    }
}
