package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.entity.BaseEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE transfer SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class Transfer extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusTransactionEnum status;
}
