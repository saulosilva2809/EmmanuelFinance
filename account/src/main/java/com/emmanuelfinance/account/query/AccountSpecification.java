package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;
import java.util.regex.Pattern;

public class AccountSpecification {

    public static Query withFilter(AccountFiltersDTO filter, UUID userId, boolean onlyDeleted) {
        Query query = new Query();

        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("deleted").is(onlyDeleted));

        if (filter.name() != null && !filter.name().isBlank()) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(filter.name()), "i"));
        }

        if (filter.type() != null) {
            query.addCriteria(Criteria.where("type").is(filter.type()));
        }

        return query;
    }
}
