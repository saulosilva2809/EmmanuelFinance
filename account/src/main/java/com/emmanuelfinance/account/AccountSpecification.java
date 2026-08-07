package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountSpecification {

    public static Specification<Account> withFilter(AccountFiltersDTO filter, UUID userId   ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("userId"), userId));

            if (filter.name() != null && !filter.name().isBlank()) {
                predicates.add(
                        builder.like(
                                builder.lower(root.get("name")),
                                "%" + filter.name().toLowerCase() + "%"
                        )
                );
            }

            if (filter.type() != null) {
                predicates.add(
                        builder.equal(
                                root.get("type"),
                                filter.type()
                        )
                );
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

}