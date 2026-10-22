package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CreditCardSpecification {

    public static Query withFilter(CreditCardFiltersDTO filters, UUID userId, boolean onlyDeleted) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("deleted").is(onlyDeleted));

        if (filters.accountId() != null) {
            criteriaList.add(Criteria.where("accountId").is(filters.accountId()));
        }

        if (filters.name() != null && !filters.name().isBlank()) {
            criteriaList.add(Criteria.where("name").regex(Pattern.quote(filters.name()), "i"));
        }

        if (filters.bank() != null) {
            criteriaList.add(Criteria.where("bank").is(filters.bank()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return query;
    }
}
