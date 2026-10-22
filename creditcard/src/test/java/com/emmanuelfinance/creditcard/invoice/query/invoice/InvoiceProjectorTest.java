package com.emmanuelfinance.creditcard.invoice.query.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceProjectorTest {

    @Mock
    private InvoiceDocumentRepository invoiceDocumentRepository;

    @InjectMocks
    private InvoiceProjector invoiceProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        InvoiceDocument snapshot = new InvoiceDocument();
        snapshot.setId(UUID.randomUUID());

        invoiceProjector.project(snapshot);

        verify(invoiceDocumentRepository).save(snapshot);
    }
}
