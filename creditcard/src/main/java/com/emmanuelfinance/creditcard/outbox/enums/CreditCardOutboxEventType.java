package com.emmanuelfinance.creditcard.outbox.enums;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;

public enum CreditCardOutboxEventType implements OutboxEventType {

    // pagamento de fatura: vai para o Kafka
    INVOICE_PAYMENT,

    // foto completa do cartão: vai para o Mongo
    CREDIT_CARD_CHANGED
}
