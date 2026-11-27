package com.emmanuelfinance.transaction.transfer.query;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.mongo.BaseDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "transfer")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class TransferDocument extends BaseDocument {

    private UUID userId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private String description;
    private StatusTransactionEnum status;
}
