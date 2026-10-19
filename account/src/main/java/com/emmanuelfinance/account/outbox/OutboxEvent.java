package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent extends BaseOutboxEvent {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AccountOutboxEventType type;

    @Override
    public OutboxEventType getType() {
        return this.type;
    }
}