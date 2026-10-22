package com.emmanuelfinance.shared.outbox.enums;

public interface OutboxEventType {

    String name();

    // O tipo é gravado como texto e lido de volta sem o enum de origem, então a comparação é pelo nome.
    default boolean is(OutboxEventType other) {
        return name().equals(other.name());
    }
}
