package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransferTestData {

    public static Transfer paidTransfer() {
        Transfer transfer = new Transfer();
        transfer.setId(UUID.randomUUID());
        transfer.setUserId(UUID.randomUUID());
        transfer.setIdempotencyKey("chave");
        transfer.setSourceAccountId(UUID.randomUUID());
        transfer.setDestinationAccountId(UUID.randomUUID());
        transfer.setAmount(new BigDecimal("100.00"));
        transfer.setDescription("Reserva");
        transfer.setStatus(StatusTransactionEnum.PAID);
        transfer.setCreatedAt(LocalDateTime.of(2026, 10, 5, 10, 0));
        return transfer;
    }
}
