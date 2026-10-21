package com.emmanuelfinance.creditcard.creditcard.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreditCardProjectorTest {

    @Mock
    private CreditCardDocumentRepository creditCardDocumentRepository;

    @InjectMocks
    private CreditCardProjector creditCardProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        CreditCardDocument snapshot = new CreditCardDocument();
        snapshot.setId(UUID.randomUUID());

        creditCardProjector.project(snapshot);

        verify(creditCardDocumentRepository).save(snapshot);
    }

    @Test
    void shouldSaveASnapshotMarkedAsDeletedWithoutAnyExtraStep() {
        CreditCardDocument snapshot = new CreditCardDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setDeleted(true);

        creditCardProjector.project(snapshot);

        verify(creditCardDocumentRepository).save(snapshot);
    }
}
