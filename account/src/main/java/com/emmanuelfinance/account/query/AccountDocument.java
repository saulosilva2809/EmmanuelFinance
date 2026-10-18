package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.enums.BanksEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "accounts")
@CompoundIndex(name = "user_deleted_created_idx", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
public class AccountDocument {

    @Id
    private UUID id;

    private UUID userId;
    private String name;
    private TypeEnum type;
    private BanksEnum bank;
    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
