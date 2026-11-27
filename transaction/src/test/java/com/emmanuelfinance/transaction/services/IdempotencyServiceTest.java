package com.emmanuelfinance.transaction.services;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import com.emmanuelfinance.transaction.transaction.dtos.IdempotencyDataDTO;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionErrorCode;
import com.emmanuelfinance.transaction.transaction.services.IdempotencyService;
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
    @DisplayName("Cenários do generateKey")
    class GenerateKeyFromDataTests {

        private final UUID userId = UUID.randomUUID();
        private final UUID accountId = UUID.randomUUID();
        private final LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);

        @Test
        @DisplayName("Deve gerar um hash SHA-256 hexadecimal de 64 caracteres")
        void shouldGenerateSha256Hex() {
            String key = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).build());

            assertTrue(key.matches("[0-9a-f]{64}"));
        }

        @Test
        @DisplayName("Deve gerar a mesma chave para os mesmos dados")
        void shouldBeDeterministic() {
            String key1 = idempotencyService.generateKey(
                    IdempotencyDataDTO.builder().userId(userId).accountId(accountId).amount(BigDecimal.TEN).date(date).build());
            String key2 = idempotencyService.generateKey(
                    IdempotencyDataDTO.builder().userId(userId).accountId(accountId).amount(BigDecimal.TEN).date(date).build());

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve usar só os dados informados: um dado a mais ou a menos muda a chave")
        void shouldUseOnlyTheInformedData() {
            String onlyUser = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).build());
            String userAndAmount = idempotencyService.generateKey(
                    IdempotencyDataDTO.builder().userId(userId).amount(BigDecimal.TEN).build());

            assertNotEquals(onlyUser, userAndAmount);
        }

        // o mesmo id em campos diferentes precisa gerar chaves diferentes, senão uma transferência de A para B
        // seria igual a uma de B para A, ou a um gasto na conta A
        @Test
        @DisplayName("Deve diferenciar o mesmo id em campos diferentes")
        void shouldKeepTheNameOfEachField() {
            String asAccount = idempotencyService.generateKey(IdempotencyDataDTO.builder().accountId(accountId).build());
            String asDestination = idempotencyService.generateKey(IdempotencyDataDTO.builder().destinationAccountId(accountId).build());
            String asCard = idempotencyService.generateKey(IdempotencyDataDTO.builder().creditCardId(accountId).build());
            String asReference = idempotencyService.generateKey(IdempotencyDataDTO.builder().referenceId(accountId).build());

            assertEquals(4, java.util.Set.of(asAccount, asDestination, asCard, asReference).size());
        }

        @Test
        @DisplayName("Deve diferenciar a transferência de A para B da de B para A")
        void shouldDifferTheDirectionOfATransfer() {
            UUID other = UUID.randomUUID();

            String aToB = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(userId).accountId(accountId).destinationAccountId(other).amount(BigDecimal.TEN).build());
            String bToA = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(userId).accountId(other).destinationAccountId(accountId).amount(BigDecimal.TEN).build());

            assertNotEquals(aToB, bToA);
        }

        @Test
        @DisplayName("Deve ignorar caixa e espaços nas pontas da descrição")
        void shouldNormalizeTheDescription() {
            String key1 = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).description("Mercado").build());
            String key2 = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).description("  MERCADO ").build());

            assertEquals(key1, key2);
        }

        @Test
        @DisplayName("Deve tratar descrição em branco como se não tivesse sido informada")
        void shouldIgnoreABlankDescription() {
            String withoutDescription = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).build());
            String blankDescription = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).description("   ").build());

            assertEquals(withoutDescription, blankDescription);
        }

        @Test
        @DisplayName("Deve tratar 100 e 100.00 como o mesmo valor, e 100 e 101 como valores diferentes")
        void shouldIgnoreTheScaleOfTheAmount() {
            String ten = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).amount(new BigDecimal("10")).build());
            String tenWithCents = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).amount(new BigDecimal("10.00")).build());
            String eleven = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).amount(new BigDecimal("11")).build());

            assertEquals(ten, tenWithCents);
            assertNotEquals(ten, eleven);
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes quando a data mudar")
        void shouldChangeWhenTheDateChanges() {
            String base = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).date(date).build());

            assertNotEquals(base, idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).date(date.plusDays(1)).build()));
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes quando usuário, valor, data ou descrição mudarem")
        void shouldChangeWhenTheDataChanges() {
            IdempotencyDataDTO base = IdempotencyDataDTO.builder()
                    .userId(userId).accountId(accountId).amount(BigDecimal.TEN).date(date).description("Mercado").build();
            String baseKey = idempotencyService.generateKey(base);

            assertNotEquals(baseKey, idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(UUID.randomUUID()).accountId(accountId).amount(BigDecimal.TEN).date(date).description("Mercado").build()));
            assertNotEquals(baseKey, idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(userId).accountId(accountId).amount(BigDecimal.ONE).date(date).description("Mercado").build()));
            assertNotEquals(baseKey, idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(userId).accountId(accountId).amount(BigDecimal.TEN).date(date.plusDays(1)).description("Mercado").build()));
            assertNotEquals(baseKey, idempotencyService.generateKey(IdempotencyDataDTO.builder()
                    .userId(userId).accountId(accountId).amount(BigDecimal.TEN).date(date).description("Farmácia").build()));
        }

        @Test
        @DisplayName("Deve gerar chaves diferentes para contas diferentes")
        void shouldChangeWhenTheAccountChanges() {
            String key1 = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).accountId(UUID.randomUUID()).build());
            String key2 = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).accountId(UUID.randomUUID()).build());

            assertNotEquals(key1, key2);
        }

        // o kafka pode entregar o mesmo pagamento duas vezes, e a chave precisa ser a mesma; dois pagamentos parciais
        // da mesma fatura têm ids diferentes e não podem se confundir
        @Test
        @DisplayName("Deve gerar sempre a mesma chave para o mesmo usuário e a mesma referência, e chaves diferentes para outras")
        void shouldBeStablePerReference() {
            UUID paymentId = UUID.randomUUID();
            String key = idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).referenceId(paymentId).build());

            assertEquals(key, idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).referenceId(paymentId).build()));
            assertNotEquals(key, idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(userId).referenceId(UUID.randomUUID()).build()));
            assertNotEquals(key, idempotencyService.generateKey(IdempotencyDataDTO.builder().userId(UUID.randomUUID()).referenceId(paymentId).build()));
        }

        @Test
        @DisplayName("Deve lançar erro quando nenhum dado for informado")
        void shouldThrowWhenNothingIsInformed() {
            assertThrows(IllegalArgumentException.class,
                    () -> idempotencyService.generateKey(IdempotencyDataDTO.builder().build()));
        }

        @Test
        @DisplayName("Deve lançar erro quando o único dado informado for uma descrição em branco")
        void shouldThrowWhenTheOnlyDataIsABlankDescription() {
            assertThrows(IllegalArgumentException.class,
                    () -> idempotencyService.generateKey(IdempotencyDataDTO.builder().description("  ").build()));
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

    @Test
    @DisplayName("Compras com categoria ou cartão diferentes geram chaves diferentes")
    void shouldTellCategoriesAndCardsApart() {
        IdempotencyDataDTO base = IdempotencyDataDTO.builder().userId(UUID.randomUUID()).build();
        String category = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                .userId(base.userId()).categoryId(UUID.randomUUID()).build());
        String card = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                .userId(base.userId()).creditCardId(UUID.randomUUID()).build());

        assertNotEquals(idempotencyService.generateKey(base), category);
        assertNotEquals(category, card);
    }

    @Test
    @DisplayName("Deve soltar a trava só quando a operação não for gravada")
    void shouldReleaseTheLockOnlyWhenNotCommitted() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            idempotencyService.validateAndLock("chave");
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);

            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            verify(redisTemplate, never()).delete(anyString());

            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(redisTemplate).delete("idempotency:transaction:chave");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
