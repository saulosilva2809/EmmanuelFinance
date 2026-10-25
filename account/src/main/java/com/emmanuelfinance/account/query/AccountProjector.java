package com.emmanuelfinance.account.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountProjector implements BaseProjector<AccountDocument> {

    private final AccountDocumentRepository accountDocumentRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "accounts", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "accounts-internal", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "account_owner", key = "#snapshot.id + ':' + #snapshot.userId"),
            @CacheEvict(value = "account_view", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "account_list", allEntries = true)
    })
    public void project(AccountDocument snapshot) {
        accountDocumentRepository.save(snapshot);
        log.info("Conta {} sincronizada no Mongo", snapshot.getId());
    }
}
