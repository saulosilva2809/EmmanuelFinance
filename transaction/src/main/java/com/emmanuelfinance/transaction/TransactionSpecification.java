package com.emmanuelfinance.transaction;

import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> withFilter(TransactionFiltersDTO filters, UUID userId, boolean onlyDeleted) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            predicates.add(criteriaBuilder.equal(root.get("deleted"), onlyDeleted));

            if (filters == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            if (filters.accountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("accountId"), filters.accountId()));
            }

            if (filters.categoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), filters.categoryId()));
            }
            if (filters.creditCardId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("creditCardId"), filters.creditCardId()));
            }

            if (filters.greaterValueThan() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), filters.greaterValueThan()));
            }

            if (filters.valueLessThan() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), filters.valueLessThan()));
            }

            if (filters.scheduled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("scheduled"), filters.scheduled()));
            }
            if (filters.date() != null) {
                LocalDateTime startOfDay = filters.date().atStartOfDay();
                LocalDateTime endOfDay = filters.date().atTime(LocalTime.MAX);

                predicates.add(
                        criteriaBuilder.between(root.get("date"), startOfDay, endOfDay)
                );
            }

            if (filters.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filters.status()));
            }

            if (filters.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), filters.type()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}