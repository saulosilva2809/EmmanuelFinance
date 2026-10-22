package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.mongo.BaseDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "invoice")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceDocument extends BaseDocument {

    private UUID userId;
    private UUID creditCardId;
    private Integer month;
    private Integer year;
    private Integer dueDate;
    private Integer closingDate;
    private InvoiceStatusEnum status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amountPaid = BigDecimal.ZERO;
}
