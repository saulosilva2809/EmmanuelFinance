package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.mongo.BaseDocument;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "credit_card")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class CreditCardDocument extends BaseDocument {

    private UUID userId;
    private UUID accountId;
    private String name;
    private BanksEnum bank;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal availableLimit;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal creditLimit;

    private Integer closingDay;
    private Integer dueDay;
}
