package com.emmanuelfinance.transaction.transfer.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferProjector implements BaseProjector<TransferDocument> {

    private final TransferDocumentRepository transferDocumentRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "transfer_view", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "transfer_list", allEntries = true)
    })
    public void project(TransferDocument snapshot) {
        transferDocumentRepository.save(snapshot);
        log.debug("Transferência {} sincronizada no Mongo", snapshot.getId());
    }
}
