package com.emmanuelfinance.category.kafka;

import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryEventListener {

    private final CacheManager cacheManager;

    @KafkaListener(topics = "account-events", groupId = "category-service-group")
    public void handleAccountDeleted(AccountEventDTO event) {
        if (StatusEventEnum.DELETED.equals(event.status()) || StatusEventEnum.RESTORE.equals(event.status())) {

            Cache accountsCache = cacheManager.getCache("accounts");
            if (accountsCache != null) {
                accountsCache.evict(event.accountId());
                log.info("Cache da conta {} evictado com sucesso no Category Service apos exclusao", event.accountId());
            }
        }
    }
}
