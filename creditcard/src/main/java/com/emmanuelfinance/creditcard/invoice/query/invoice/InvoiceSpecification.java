package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceSpecification {

    public static Query withFilter(InvoiceFiltersDTO filters, UUID userId) {
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("deleted").is(false));

        if (filters.creditCardId() != null) {
            criteriaList.add(Criteria.where("creditCardId").is(filters.creditCardId()));
        }

        if (filters.month() != null) {
            criteriaList.add(Criteria.where("month").is(filters.month()));
        }

        if (filters.year() != null) {
            criteriaList.add(Criteria.where("year").is(filters.year()));
        }

        if (filters.status() != null) {
            criteriaList.add(Criteria.where("status").is(filters.status()));
        }

        return new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }
}
