package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceItemProjector implements BaseProjector<InvoiceItemDocument> {

    private final InvoiceItemDocumentRepository invoiceItemDocumentRepository;

    @Override
    public void project(InvoiceItemDocument snapshot) {
        invoiceItemDocumentRepository.save(snapshot);
        log.info("Item de fatura {} sincronizado no Mongo", snapshot.getId());
    }
}
