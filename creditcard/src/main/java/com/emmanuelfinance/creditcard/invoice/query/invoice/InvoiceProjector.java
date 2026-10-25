package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceProjector implements BaseProjector<InvoiceDocument> {

    private final InvoiceDocumentRepository invoiceDocumentRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "invoice_list", allEntries = true),
            @CacheEvict(value = "invoice_item_list", allEntries = true)
    })
    public void project(InvoiceDocument snapshot) {
        invoiceDocumentRepository.save(snapshot);
        log.info("Fatura {} sincronizado no Mongo", snapshot.getId());
    }
}
