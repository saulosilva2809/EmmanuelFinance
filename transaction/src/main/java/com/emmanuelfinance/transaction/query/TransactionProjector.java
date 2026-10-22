package com.emmanuelfinance.transaction.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProjector implements BaseProjector<TransactionDocument> {

    private final TransactionDocumentRepository transactionDocumentRepository;

    @Override
    public void project(TransactionDocument snapshot) {
        transactionDocumentRepository.save(snapshot);
        log.info("Transação {} sincronizada no Mongo", snapshot.getId());
    }
}
