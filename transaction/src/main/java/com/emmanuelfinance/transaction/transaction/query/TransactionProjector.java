package com.emmanuelfinance.transaction.transaction.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProjector implements BaseProjector<TransactionDocument> {

    private final TransactionDocumentRepository transactionDocumentRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "transaction_summary", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "transaction_view", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "transaction_list", allEntries = true)
    })
    public void project(TransactionDocument snapshot) {
        transactionDocumentRepository.save(snapshot);
        log.info("Transação {} sincronizada no Mongo", snapshot.getId());
    }
}
