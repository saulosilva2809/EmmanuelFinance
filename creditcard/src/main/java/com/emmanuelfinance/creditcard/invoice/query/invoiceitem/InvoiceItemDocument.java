package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import com.emmanuelfinance.shared.mongo.BaseDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "invoice_item")
@CompoundIndex(name = "user_invoice_deleted_idx", def = "{'userId': 1, 'invoiceId': 1, 'deleted': 1}")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItemDocument extends BaseDocument {

    private UUID userId;
    private UUID invoiceId;
    private UUID transactionId;
    private Integer installmentNumber;
    private Integer totalInstallments;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;
}
