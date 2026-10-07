package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.services.TransactionServiceInternal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionListenerTest {

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionServiceInternal transactionServiceInternal;

    @InjectMocks
    private TransactionListener transactionListener;

    @Nested
    @DisplayName("Cenários do handleTransactionFailed")
    class TransactionFailedTests {

        @Test
        @DisplayName("Deve marcar a transação como FAILED e salvar")
        void shouldMarkTransactionAsFailed() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
            when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

            transactionListener.handleTransactionFailed(transaction.getId());

            assertEquals(StatusTransactionEnum.FAILED, transaction.getStatus());
            verify(transactionRepository, times(1)).save(transaction);
        }

        @Test
        @DisplayName("Deve propagar TransactionNotFound e não salvar quando a transação não existir")
        void shouldPropagateWhenTransactionDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(transactionSelector.getTransactionByIdInternal(id)).thenThrow(new TransactionNotFound());

            assertThrows(TransactionNotFound.class, () -> transactionListener.handleTransactionFailed(id));

            verifyNoInteractions(transactionRepository);
        }
    }

    @Nested
    @DisplayName("Cenários do handleCreditCardInvoicePayment")
    class InvoicePaymentTests {

        private final InvoicePaymentDTO payment = new InvoicePaymentDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cartão C6",
                UUID.randomUUID(), 3, 2026, new BigDecimal("100.00")
        );

        @Test
        @DisplayName("Deve delegar a criação da transação do pagamento ao service")
        void shouldDelegateToTheService() {
            transactionListener.handleCreditCardInvoicePayment(payment);

            verify(transactionServiceInternal, times(1)).createTransactionRelatedToInvoicePayment(payment);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar novamente quando a criação falhar")
        void shouldPropagateFailureSoKafkaCanRetry() {
            doThrow(new IllegalStateException("falha")).when(transactionServiceInternal)
                    .createTransactionRelatedToInvoicePayment(payment);

            assertThrows(IllegalStateException.class, () -> transactionListener.handleCreditCardInvoicePayment(payment));
        }
    }
}
