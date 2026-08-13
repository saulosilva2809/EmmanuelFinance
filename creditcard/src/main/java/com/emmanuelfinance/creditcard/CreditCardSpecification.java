package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreditCardSpecification {

    public static Specification<CreditCard> withFilter(CreditCardFiltersDTO filters, UUID userId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (filters.accountId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("accountId"),
                                filters.accountId()
                        )
                );
            }

            if (filters.name() != null && !filters.name().isBlank()) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")),
                                "%" + filters.name().toLowerCase() + "%"
                        )
                );
            }

            if (filters.bank() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("bank"),
                                filters.bank()
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
