package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@NoArgsConstructor
public class OutboxEvent extends BaseOutboxEvent {

    @Column(name = "type", nullable = false)
    private String type;

    public void setType(OutboxEventType type) {
        this.type = type.name();
    }

    @Override
    public OutboxEventType getType() {
        return new StoredType(type);
    }

    private record StoredType(String name) implements OutboxEventType {

        @Override
        public String toString() {
            return name;
        }
    }
}
