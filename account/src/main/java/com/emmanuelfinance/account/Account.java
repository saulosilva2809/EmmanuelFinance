package com.emmanuelfinance.account;

import com.emmanuelfinance.shared.entity.BaseEntity;
import  com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.account.enums.TypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE account SET deleted = true, version = version + 1 WHERE id = ? AND version = ?")
@Filter(name = "deletedFilter")
public class Account extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeEnum type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BanksEnum bank;

    @Column(name = "initial_balance", nullable = false)
    private BigDecimal initialBalance;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
