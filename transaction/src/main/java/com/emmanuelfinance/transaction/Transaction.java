package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.entity.BaseEntity;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE transaction SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class Transaction extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "credit_card_id")
    private UUID creditCardId;

    @Column(name = "recurring_id")
    private UUID recurringId;

    @Column(name = "description")
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "installments_count")
    private Integer installmentsCount = 1;

    @Column(name = "scheduled", nullable = false)
    private boolean scheduled = false;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusTransactionEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private com.emmanuelfinance.shared.enums.TypeEnum type;
}