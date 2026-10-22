package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceItemSpecificationTest {

    @Test
    void shouldFilterByUserInvoiceAndNotDeleted() {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        Query query = InvoiceItemSpecification.withFilter(userId, invoiceId);

        @SuppressWarnings("unchecked")
        List<Document> conditions = (List<Document>) query.getQueryObject().get("$and");
        assertEquals(3, conditions.size());
        assertEquals(userId, conditions.get(0).get("userId"));
        assertEquals(invoiceId, conditions.get(1).get("invoiceId"));
        assertEquals(false, conditions.get(2).get("deleted"));
    }
}
