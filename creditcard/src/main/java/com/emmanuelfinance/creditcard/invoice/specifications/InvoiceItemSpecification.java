package com.emmanuelfinance.creditcard.invoice.specifications;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceItemSpecification {

    public static Specification<InvoiceItem> withFilter(UUID userId, UUID invoiceId) {
        return (root, query, builder) -> {
            List<Predicate> predicateList = new ArrayList<>();
            predicateList.add(builder.equal(root.get("userId"), userId));
            predicateList.add(builder.equal(root.get("invoiceId"), invoiceId));

            return builder.and(predicateList.toArray(new Predicate[0]));
        };
    }
}
