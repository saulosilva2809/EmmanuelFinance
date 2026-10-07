package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.exceptions.TransactionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Nested
    @DisplayName("Cenários do generateIdempotencyKey")
    class GenerateKeyTests {

        private final UUID userId = UUID.randomUUID();
        private final UUID accountId = UUID.randomUUID();

        private CreateTransactionDTO dto(BigDecimal amount, LocalDateTime date, String description) {
            return new CreateTransactionDTO(
                    accountId, null, UUID.randomUUID(), description, amount, 1, date != null, date, TypeEnum.EXPENSE
            );
        }

        @Test
        @DisplayName("Deve gerar um hash SHA-256 hexadecimal de 64 caracteres")
        void shouldGenerateSha256Hex() {
            String key = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, "Mercado"));

            assertEquals(64, key.length());
            assertTrue(key.matches("[0-9a-f]{64}"));
        }

        @Test
        @DisplayName("Deve gerar a mesma chave para os mesmos dados")
        void shouldBeDeterministic() {
            String key1 = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, "Mercado"));
            String key2 = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, "Mercado"));

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve ignorar caixa e espaços nas pontas da descrição")
        void shouldNormalizeDescription() {
            String key1 = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, "Mercado"));
            String key2 = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, "  MERCADO "));

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve tratar descrição e data nulas sem erro")
        void shouldHandleNullDescriptionAndDate() {
            assertDoesNotThrow(() ->
                    idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, null, null))
            );
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes quando usuário, valor, data ou descrição mudarem")
        void shouldChangeWhenDataChanges() {
            LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);
            String base = idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, date, "Mercado"));

            assertNotEquals(base, idempotencyService.generateIdempotencyKey(UUID.randomUUID(), dto(BigDecimal.TEN, date, "Mercado")));
            assertNotEquals(base, idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.ONE, date, "Mercado")));
            assertNotEquals(base, idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, date.plusDays(1), "Mercado")));
            assertNotEquals(base, idempotencyService.generateIdempotencyKey(userId, dto(BigDecimal.TEN, date, "Farmácia")));
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes para contas diferentes")
        void shouldChangeWhenAccountChanges() {
            CreateTransactionDTO dto1 = TransactionTestDataBuilder.createDTO(UUID.randomUUID(), UUID.randomUUID());
            CreateTransactionDTO dto2 = TransactionTestDataBuilder.createDTO(UUID.randomUUID(), UUID.randomUUID());

            assertNotEquals(
                    idempotencyService.generateIdempotencyKey(userId, dto1),
                    idempotencyService.generateIdempotencyKey(userId, dto2)
            );
        }
    }

    @Nested
    @DisplayName("Cenários do validateAndLock")
    class ValidateAndLockTests {

        @Test
        @DisplayName("Deve travar a chave no Redis por 2 minutos na primeira requisição")
        void shouldLockOnFirstRequest() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), eq("LOCKED"), any(Duration.class))).thenReturn(true);

            assertDoesNotThrow(() -> idempotencyService.validateAndLock("abc123"));

            verify(valueOperations, times(1)).setIfAbsent(
                    "idempotency:transaction:abc123", "LOCKED", Duration.ofMinutes(2)
            );
        }

        @Test
        @DisplayName("Deve lançar TRANSACTION_ALREADY_EXISTS quando a chave já existir")
        void shouldThrowWhenKeyAlreadyExists() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            TransactionDomainException exception = assertThrows(
                    TransactionDomainException.class,
                    () -> idempotencyService.validateAndLock("abc123")
            );

            assertEquals(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS.getStatus(), exception.getStatus());
            assertEquals(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar TRANSACTION_ALREADY_EXISTS quando o Redis retornar null")
        void shouldThrowWhenRedisReturnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);

            assertThrows(TransactionDomainException.class, () -> idempotencyService.validateAndLock("abc123"));
        }
    }

    @Nested
    @DisplayName("Cenários do generateInvoicePaymentKey")
    class InvoicePaymentKeyTests {

        @Test
        @DisplayName("Deve gerar sempre a mesma chave para o mesmo usuário e a mesma fatura")
        void shouldBeStablePerInvoice() {
            UUID userId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();

            String key1 = idempotencyService.generateInvoicePaymentKey(userId, invoiceId);
            String key2 = idempotencyService.generateInvoicePaymentKey(userId, invoiceId);

            assertEquals(key1, key2);
            assertTrue(key1.matches("[0-9a-f]{64}"));
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes para faturas ou usuários diferentes")
        void shouldDifferPerInvoiceAndUser() {
            UUID userId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();
            String base = idempotencyService.generateInvoicePaymentKey(userId, invoiceId);

            assertNotEquals(base, idempotencyService.generateInvoicePaymentKey(userId, UUID.randomUUID()));
            assertNotEquals(base, idempotencyService.generateInvoicePaymentKey(UUID.randomUUID(), invoiceId));
        }
    }
}
