package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceInternalTest {

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private CategoryClientCacheService categoryClientCacheService;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventsService transactionEventsService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private TransactionServiceInternal transactionServiceInternal;

    @Nested
    @DisplayName("Cenários do getSummaryDTO")
    class GetSummaryTests {

        @Test
        @DisplayName("Deve montar o resumo da transação com a categoria")
        void shouldBuildSummary() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
            CategorySummaryDTO category = TransactionTestDataBuilder.categorySummaryDTO(transaction.getCategoryId());

            when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);
            when(categoryClientCacheService.getCategorySummaryDTO(transaction.getCategoryId())).thenReturn(category);

            TransactionSummaryDTO result = transactionServiceInternal.getSummaryDTO(transaction.getId());

            assertEquals(category, result.category());
            assertEquals(transaction.getAmount(), result.amount());
            assertEquals(transaction.getStatus(), result.status());
            assertEquals(transaction.getType(), result.type());
        }

        @Test
        @DisplayName("Deve retornar o resumo sem categoria para transação sem categoria (pagamento de fatura)")
        void shouldBuildSummaryWithoutCategory() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
            transaction.setCategoryId(null);
            when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

            TransactionSummaryDTO result = transactionServiceInternal.getSummaryDTO(transaction.getId());

            assertNull(result.category());
            assertEquals(transaction.getAmount(), result.amount());
            verifyNoInteractions(categoryClientCacheService);
        }

        @Test
        @DisplayName("Deve propagar TransactionNotFound quando a transação não existir")
        void shouldPropagateTransactionNotFound() {
            UUID id = UUID.randomUUID();
            when(transactionSelector.getTransactionByIdInternal(id)).thenThrow(new TransactionNotFound());

            assertThrows(TransactionNotFound.class, () -> transactionServiceInternal.getSummaryDTO(id));

            verifyNoInteractions(categoryClientCacheService);
        }
    }

    @Nested
    @DisplayName("Cenários do createTransactionRelatedToInvoicePayment")
    class InvoicePaymentTransactionTests {

        private final UUID userId = UUID.randomUUID();
        private final UUID invoiceId = UUID.randomUUID();

        private InvoicePaymentDTO payment() {
            return new InvoicePaymentDTO(
                    UUID.randomUUID(), userId, UUID.randomUUID(), "Cartão C6",
                    invoiceId, 3, 2026, new BigDecimal("450.75")
            );
        }

        @Test
        @DisplayName("Deve criar a despesa paga com os dados do pagamento e publicar o evento")
        void shouldCreatePaidExpenseAndPublishEvent() {
            InvoicePaymentDTO payment = payment();
            when(idempotencyService.generateInvoicePaymentKey(userId, invoiceId)).thenReturn("chave-fatura");
            when(transactionRepository.existsByIdempotencyKey("chave-fatura")).thenReturn(false);
            when(transactionMapper.toEntity(any(CreateTransactionDTO.class))).thenAnswer(invocation ->
                    TransactionTestDataBuilder.entityFromDTO(invocation.getArgument(0))
            );

            transactionServiceInternal.createTransactionRelatedToInvoicePayment(payment);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            InOrder inOrder = inOrder(transactionRepository, transactionEventsService);
            inOrder.verify(transactionRepository).save(captor.capture());
            inOrder.verify(transactionEventsService).publishInvoicePaymentCreatedEvent(captor.getValue());

            Transaction saved = captor.getValue();
            assertEquals(userId, saved.getUserId());
            assertEquals(payment.accountId(), saved.getAccountId());
            assertEquals(payment.creditCardId(), saved.getCreditCardId());
            assertEquals(new BigDecimal("450.75"), saved.getAmount());
            assertEquals(TypeEnum.EXPENSE, saved.getType());
            assertEquals(StatusTransactionEnum.PAID, saved.getStatus());
            assertEquals("chave-fatura", saved.getIdempotencyKey());
            assertNull(saved.getCategoryId());
            assertFalse(saved.isScheduled());
        }

        @Test
        @DisplayName("Deve descrever a transação com o mês, o ano e o nome do cartão")
        void shouldBuildTheDescriptionWithMonthYearAndCardName() {
            when(idempotencyService.generateInvoicePaymentKey(userId, invoiceId)).thenReturn("chave-fatura");
            when(transactionMapper.toEntity(any(CreateTransactionDTO.class))).thenAnswer(invocation ->
                    TransactionTestDataBuilder.entityFromDTO(invocation.getArgument(0))
            );

            transactionServiceInternal.createTransactionRelatedToInvoicePayment(payment());

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            String description = captor.getValue().getDescription();
            assertTrue(description.contains("03/2026"), description);
            assertTrue(description.contains("Cartão C6"), description);
            assertFalse(description.contains("{}"), description);
        }

        @Test
        @DisplayName("Não deve criar outra transação quando o pagamento da fatura já foi processado")
        void shouldIgnoreAlreadyProcessedPayment() {
            when(idempotencyService.generateInvoicePaymentKey(userId, invoiceId)).thenReturn("chave-fatura");
            when(transactionRepository.existsByIdempotencyKey("chave-fatura")).thenReturn(true);

            transactionServiceInternal.createTransactionRelatedToInvoicePayment(payment());

            verify(transactionRepository, never()).save(any(Transaction.class));
            verifyNoInteractions(transactionEventsService, transactionMapper);
        }
    }
}
