package com.emmanuelfinance.creditcard.invoice;

import com.emmanuelfinance.shared.entity.BaseEntity;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE invoice SET deleted = true WHERE id = ?")
@Filter(name = "deletedFilter")
public class Invoice extends BaseEntity {

    @Column(name = "credit_card_id", nullable = false)
    private UUID creditCardId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "due_date", nullable = false)
    private Integer dueDate;

    @Column(name = "closing_date", nullable = false)
    private Integer closingDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatusEnum status;
}
