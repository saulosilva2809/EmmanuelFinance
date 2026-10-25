package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardProjector implements BaseProjector<CreditCardDocument> {

    private final CreditCardDocumentRepository creditCardDocumentRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "credit_card_summary", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "credit_card_internal_summary", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "credit_card_view", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "credit_card_list", allEntries = true),
            @CacheEvict(value = "invoice_list", allEntries = true),
            @CacheEvict(value = "transaction_view", allEntries = true),
            @CacheEvict(value = "transaction_list", allEntries = true)
    })
    public void project(CreditCardDocument snapshot) {
        creditCardDocumentRepository.save(snapshot);
        log.info("Cartão {} sincronizado no Mongo", snapshot.getId());
    }
}
