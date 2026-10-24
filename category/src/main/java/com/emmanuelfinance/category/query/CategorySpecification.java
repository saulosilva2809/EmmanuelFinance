package com.emmanuelfinance.category.query;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;
import java.util.regex.Pattern;

public class CategorySpecification {

    public static Query withFilter(CategoryFiltersDTO filters, UUID userId, boolean onlyDeleted) {
        Query query = new Query();

        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("deleted").is(onlyDeleted));

        if (filters.name() != null && !filters.name().isBlank()) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(filters.name()), "i"));

        }

        if (filters.type() != null) {
            query.addCriteria(Criteria.where("type").is(filters.type()));
        }

        return query;
    }
}
