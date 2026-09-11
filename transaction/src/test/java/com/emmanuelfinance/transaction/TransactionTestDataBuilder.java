package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.services.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionTestDataBuilder {

    private final IdempotencyService idempotencyService;

    public static CreateTransactionDTO createTransactionDTO() {
        return new CreateTransactionDTO(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Salário",
                new BigDecimal(12000),
                null,
                false,
                null,
                TypeEnum.INCOME
        );
    }

//    public Transaction createEntity(CreateTransactionDTO inputDto, UUID userId, boolean deleted) {
//        Transaction transaction = new Transaction();
//        transaction.setId(UUID.randomUUID());
//        transaction.setIdempotencyKey(idempotencyService.generateIdempotencyKey(userId, inputDto));
//        transaction.setAccountId(inputDto.accountId());
//        transaction.setCategoryId(inputDto.categoryId());
//        transaction.setCreditCardId(inputDto.creditCardId());
//    }
}