package com.emmanuelfinance.transaction.query;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.mongo.BaseDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "transaction")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class TransactionDocument extends BaseDocument {

    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private UUID creditCardId;
    private UUID recurringId;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private Integer installmentsCount;
    private boolean scheduled;
    private LocalDateTime date;
    private StatusTransactionEnum status;
    private TypeEnum type;
}
