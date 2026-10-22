package com.emmanuelfinance.transaction.transaction.query;

import com.emmanuelfinance.transaction.transaction.dtos.TransactionFiltersDTO;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// monta a consulta do mongo a partir dos filtros opcionais. cada filtro preenchido vira uma condição,
// e todas precisam valer ao mesmo tempo
public class TransactionSpecification {

    public static Query withFilter(TransactionFiltersDTO filters, UUID userId, boolean onlyDeleted) {
        List<Criteria> criteriaList = new ArrayList<>();

        // sempre filtra pelo dono, e por ativas (ou só excluídas, na lista de excluídas)
        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("deleted").is(onlyDeleted));

        if (filters != null) {
            if (filters.accountId() != null) {
                criteriaList.add(Criteria.where("accountId").is(filters.accountId()));
            }

            if (filters.categoryId() != null) {
                criteriaList.add(Criteria.where("categoryId").is(filters.categoryId()));
            }

            if (filters.creditCardId() != null) {
                criteriaList.add(Criteria.where("creditCardId").is(filters.creditCardId()));
            }

            // valor mínimo e máximo valem para o próprio valor (maior ou igual, menor ou igual)
            if (filters.greaterValueThan() != null) {
                criteriaList.add(Criteria.where("amount").gte(filters.greaterValueThan()));
            }

            if (filters.valueLessThan() != null) {
                criteriaList.add(Criteria.where("amount").lte(filters.valueLessThan()));
            }

            if (filters.scheduled() != null) {
                criteriaList.add(Criteria.where("scheduled").is(filters.scheduled()));
            }

            // a data filtrada é um dia: pega do primeiro ao último instante dele
            if (filters.date() != null) {
                LocalDateTime startOfDay = filters.date().atStartOfDay();
                LocalDateTime endOfDay = filters.date().atTime(LocalTime.MAX);

                criteriaList.add(Criteria.where("date").gte(startOfDay).lte(endOfDay));
            }

            if (filters.status() != null) {
                criteriaList.add(Criteria.where("status").is(filters.status()));
            }

            if (filters.type() != null) {
                criteriaList.add(Criteria.where("type").is(filters.type()));
            }
        }

        // junta tudo: todas as condições precisam valer ao mesmo tempo
        return new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }
}
