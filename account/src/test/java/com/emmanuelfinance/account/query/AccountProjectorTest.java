package com.emmanuelfinance.account.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountProjectorTest {

    @Mock
    private AccountDocumentRepository accountDocumentRepository;

    @InjectMocks
    private AccountProjector accountProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        AccountDocument snapshot = new AccountDocument();
        snapshot.setId(UUID.randomUUID());

        accountProjector.project(snapshot);

        verify(accountDocumentRepository).save(snapshot);
    }

    @Test
    void shouldSaveASnapshotMarkedAsDeletedWithoutAnyExtraStep() {
        AccountDocument snapshot = new AccountDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setDeleted(true);

        accountProjector.project(snapshot);

        verify(accountDocumentRepository).save(snapshot);
    }
}
