package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;

public class InvoiceItemSpecification {

    public static Query withFilter(UUID userId, UUID invoiceId) {
        return new Query(new Criteria().andOperator(
                Criteria.where("userId").is(userId),
                Criteria.where("invoiceId").is(invoiceId),
                Criteria.where("deleted").is(false)
        ));
    }
}
