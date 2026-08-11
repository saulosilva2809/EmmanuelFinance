package com.emmanuelfinance.category;

import com.emmanuelfinance.shared.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.kafka.account.enums.StatusEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryEventListener {

    private final CategoryRepository categoryRepository;

    @Transactional
    @KafkaListener(topics = "account-events", groupId = "category-service-group")
    public void handleAccountDeleted(AccountEventDTO event) {
        if (StatusEventEnum.DELETED.equals(event.status())) {
            categoryRepository.deleteAllByAccountIdAndUserId(event.accountId(), event.userId());
            log.info("Categorias da conta {} removidas apos exclusao da conta", event.accountId());
        }
    }
}
