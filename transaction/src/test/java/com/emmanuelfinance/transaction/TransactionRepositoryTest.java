package com.emmanuelfinance.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("existsByIdempotencyKey deve retornar true quando já existir uma transação com a chave")
    void shouldReturnTrueWhenKeyExists() {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
        transaction.setIdempotencyKey("chave-existente");
        transactionRepository.saveAndFlush(transaction);

        assertTrue(transactionRepository.existsByIdempotencyKey("chave-existente"));
    }

    @Test
    @DisplayName("existsByIdempotencyKey deve retornar false quando a chave não existir")
    void shouldReturnFalseWhenKeyDoesNotExist() {
        assertFalse(transactionRepository.existsByIdempotencyKey("chave-inexistente"));
    }

    @Test
    @DisplayName("Deve persistir uma transação sem categoria (pagamento de fatura)")
    void shouldPersistTransactionWithoutCategory() {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
        transaction.setCategoryId(null);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        assertNull(transactionRepository.findById(saved.getId()).orElseThrow().getCategoryId());
    }
}
