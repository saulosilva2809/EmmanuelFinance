package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceItemProjectorTest {

    @Mock
    private InvoiceItemDocumentRepository invoiceItemDocumentRepository;

    @InjectMocks
    private InvoiceItemProjector invoiceItemProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        InvoiceItemDocument snapshot = new InvoiceItemDocument();
        snapshot.setId(UUID.randomUUID());

        invoiceItemProjector.project(snapshot);

        verify(invoiceItemDocumentRepository).save(snapshot);
    }
}
