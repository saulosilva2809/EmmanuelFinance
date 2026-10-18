package com.emmanuelfinance.account.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class AccountProjector {

    private final AccountDocumentRepository accountDocumentRepository;

    public void project(AccountDocument snapshot) {
        accountDocumentRepository.save(snapshot);
        log.info("Conta {} sincronizada no Mongo", snapshot.getId());
    }
}
