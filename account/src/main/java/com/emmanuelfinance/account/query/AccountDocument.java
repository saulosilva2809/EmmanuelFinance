package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.enums.TypeEnum;
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

@Document(collection = "accounts")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class AccountDocument extends BaseDocument {

    private UUID userId;
    private String name;
    private TypeEnum type;
    private BanksEnum bank;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal initialBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal currentBalance;
}
