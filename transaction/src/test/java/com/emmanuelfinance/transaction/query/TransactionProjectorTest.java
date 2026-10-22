package com.emmanuelfinance.transaction.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionProjectorTest {

    @Mock
    private TransactionDocumentRepository transactionDocumentRepository;

    @InjectMocks
    private TransactionProjector transactionProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        TransactionDocument snapshot = new TransactionDocument();
        snapshot.setId(UUID.randomUUID());

        transactionProjector.project(snapshot);

        verify(transactionDocumentRepository).save(snapshot);
    }
}
