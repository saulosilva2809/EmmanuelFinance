package com.emmanuelfinance.transaction.transfer.query;

import com.emmanuelfinance.transaction.transfer.dtos.TransferFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransferSpecification {

    public static Query withFilter(TransferFiltersDTO filters, UUID userId, boolean onlyDeleted) {
        List<Criteria> criteriaList = new ArrayList<>();

        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("deleted").is(onlyDeleted));

        if (filters != null) {
            if (filters.sourceAccountId() != null) {
                criteriaList.add(Criteria.where("sourceAccountId").is(filters.sourceAccountId()));
            }

            if (filters.destinationAccountId() != null) {
                criteriaList.add(Criteria.where("destinationAccountId").is(filters.destinationAccountId()));
            }

            if (filters.amountMin() != null) {
                criteriaList.add(Criteria.where("amount").gte(filters.amountMin()));
            }

            if (filters.amountMax() != null) {
                criteriaList.add(Criteria.where("amount").lte(filters.amountMax()));
            }

            // a data filtrada vale pelo dia inteiro
            if (filters.createdAt() != null) {
                LocalDate day = filters.createdAt().toLocalDate();

                criteriaList.add(Criteria.where("createdAt").gte(day.atStartOfDay()).lte(day.atTime(LocalTime.MAX)));
            }
        }
        
        return new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }
}
