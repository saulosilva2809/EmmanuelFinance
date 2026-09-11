package com.emmanuelfinance.creditcard.invoice.specifications;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceSpecification {

    public static Specification<Invoice> withFilter(InvoiceFiltersDTO filters, UUID userId) {
        return (root, query, builder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            predicateList.add(builder.equal(root.get("userId"), userId));

            if (filters.creditCardId() != null) {
                predicateList.add(
                        builder.equal(root.get("creditCardId"), filters.creditCardId())
                );
            }

            if (filters.month() != null) {
                predicateList.add(
                        builder.equal(root.get("month"), filters.month())
                );
            }

            if (filters.year() != null) {
                predicateList.add(
                        builder.equal(root.get("year"), filters.year())
                );
            }

            if (filters.status() != null) {
                predicateList.add(
                        builder.equal(root.get("status"), filters.status())
                );
            }

            return builder.and(predicateList.toArray(new Predicate[0]));
        };
    }
}
