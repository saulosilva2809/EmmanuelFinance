package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionSelectorTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private TransactionSelector transactionSelector;

    @BeforeEach
    void setUp() {
        transactionSelector = new TransactionSelector(transactionRepository, securityUtils);
    }

    private Transaction save(Transaction transaction) {
        return transactionRepository.saveAndFlush(transaction);
    }

    @Nested
    @DisplayName("Cenários do getTransactionById")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Deve buscar a transação do usuário logado que não esteja excluída")
        void shouldReturnTransactionOfCurrentUser() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(userId));

            Transaction result = transactionSelector.getTransactionById(saved.getId());

            assertEquals(saved.getId(), result.getId());
            assertEquals(userId, result.getUserId());
            assertFalse(result.isDeleted());
            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar TransactionNotFound quando a transação estiver excluída")
        void shouldThrowWhenDeleted() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(userId, true));

            assertThrows(TransactionNotFound.class, () -> transactionSelector.getTransactionById(saved.getId()));
        }

        @Test
        @DisplayName("Deve lançar TransactionNotFound quando a transação for de outro usuário")
        void shouldThrowWhenBelongsToAnotherUser() {
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(UUID.randomUUID()));

            assertThrows(TransactionNotFound.class, () -> transactionSelector.getTransactionById(saved.getId()));
        }

        @Test
        @DisplayName("Deve lançar TransactionNotFound quando o ID não existir")
        void shouldThrowWhenDoesNotExist() {
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());

            assertThrows(TransactionNotFound.class, () -> transactionSelector.getTransactionById(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Cenários do getTransactionByIdIncluingDeleted")
    class GetTransactionByIdIncludingDeletedTests {

        @Test
        @DisplayName("Deve buscar a transação excluída do usuário logado")
        void shouldReturnDeletedTransactionOfCurrentUser() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(userId, true));

            Transaction result = transactionSelector.getTransactionByIdIncluingDeleted(saved.getId());

            assertEquals(saved.getId(), result.getId());
            assertTrue(result.isDeleted());
        }

        @Test
        @DisplayName("Deve buscar também a transação não excluída do usuário logado")
        void shouldReturnNonDeletedTransactionOfCurrentUser() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(userId, false));

            assertEquals(saved.getId(), transactionSelector.getTransactionByIdIncluingDeleted(saved.getId()).getId());
        }

        @Test
        @DisplayName("Deve lançar TransactionNotFound quando a transação excluída for de outro usuário")
        void shouldThrowWhenBelongsToAnotherUser() {
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(UUID.randomUUID(), true));

            assertThrows(TransactionNotFound.class,
                    () -> transactionSelector.getTransactionByIdIncluingDeleted(saved.getId()));
        }
    }

    @Nested
    @DisplayName("Cenários do getTransactionByIdInternal")
    class GetTransactionByIdInternalTests {

        @Test
        @DisplayName("Deve buscar a transação sem filtrar por usuário")
        void shouldReturnRegardlessOfUser() {
            Transaction saved = save(TransactionTestDataBuilder.transactionEntity(UUID.randomUUID()));

            Transaction result = transactionSelector.getTransactionByIdInternal(saved.getId());

            assertEquals(saved.getId(), result.getId());
            verify(securityUtils, never()).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar TransactionNotFound quando o ID não existir")
        void shouldThrowWhenDoesNotExist() {
            assertThrows(TransactionNotFound.class,
                    () -> transactionSelector.getTransactionByIdInternal(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Cenários dos pendentes por data")
    class PendingTransactionsTests {

        private final LocalDateTime cutoff = LocalDateTime.now();
        private final UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("Deve retornar apenas as PENDING com data depois do corte")
        void shouldReturnPendingAfter() {
            Transaction future = save(TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.plusDays(1)));
            save(TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.minusDays(1)));
            Transaction paidFuture = TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.plusDays(2));
            paidFuture.setStatus(StatusTransactionEnum.PAID);
            save(paidFuture);

            List<Transaction> result = transactionSelector.getPendingTransactionsAfter(cutoff);

            assertEquals(1, result.size());
            assertEquals(future.getId(), result.get(0).getId());
        }

        @Test
        @DisplayName("Deve retornar apenas as PENDING com data antes do corte")
        void shouldReturnPendingBefore() {
            save(TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.plusDays(1)));
            Transaction past = save(TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.minusDays(1)));
            Transaction paidPast = TransactionTestDataBuilder.pendingTransactionEntity(userId, cutoff.minusDays(2));
            paidPast.setStatus(StatusTransactionEnum.PAID);
            save(paidPast);

            List<Transaction> result = transactionSelector.getPendingTransactionsBefore(cutoff);

            assertEquals(1, result.size());
            assertEquals(past.getId(), result.get(0).getId());
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver pendentes")
        void shouldReturnEmptyList() {
            assertTrue(transactionSelector.getPendingTransactionsAfter(cutoff).isEmpty());
            assertTrue(transactionSelector.getPendingTransactionsBefore(cutoff).isEmpty());
        }
    }
}
