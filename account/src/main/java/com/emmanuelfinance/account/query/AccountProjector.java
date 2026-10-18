package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.dto.events.AccountChangedEventDTO;
import com.emmanuelfinance.account.dto.events.AccountDeletedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Mantém o MongoDB atualizado a partir do que acontece no Postgres.
// Só roda DEPOIS do commit, então o Mongo nunca recebe dados de uma transação que deu rollback.
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountProjector {

    private final AccountDocumentRepository accountDocumentRepository;
    private final AccountMapper accountMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountChanged(AccountChangedEventDTO event) {
        AccountDocument document = accountMapper.toDocument(event.account());
        accountDocumentRepository.save(document);
        log.info("Conta {} sincronizada no Mongo", document.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountDeleted(AccountDeletedEventDTO event) {
        accountDocumentRepository.findById(event.accountId()).ifPresent(document -> {
            document.setDeleted(true);
            accountDocumentRepository.save(document);
            log.info("Conta {} marcada como excluída no Mongo", document.getId());
        });
    }
}
