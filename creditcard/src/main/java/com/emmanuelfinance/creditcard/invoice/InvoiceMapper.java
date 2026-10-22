package com.emmanuelfinance.creditcard.invoice;

import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class InvoiceMapper {

    @Mapping(target = "id", source = "document.id")
    public abstract ResponseInvoiceDTO toResponseDTO(InvoiceDocument document, CreditCardSummaryDTO creditCard);

    public abstract ResponseInvoiceSummaryDTO toSummaryDTO(InvoiceDocument document);

    public abstract InvoiceDocument toDocument(Invoice entity);

    public abstract InvoiceItemDocument toItemDocument(InvoiceItem entity);
}
