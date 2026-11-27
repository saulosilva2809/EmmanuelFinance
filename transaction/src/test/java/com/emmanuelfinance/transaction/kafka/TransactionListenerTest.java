package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.transaction.transaction.kafka.TransactionListener;
import com.emmanuelfinance.transaction.transaction.services.TransactionFailureService;
import com.emmanuelfinance.transaction.transaction.services.TransactionServiceInternal;
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
    private TransactionFailureService transactionFailureService;

    @Mock
    private TransactionServiceInternal transactionServiceInternal;

    @InjectMocks
    private TransactionListener transactionListener;

    @Nested
    @DisplayName("Cenários do handleTransactionFailed")
    class TransactionFailedTests {

        private final TransactionFailedEvent event = new TransactionFailedEvent(
                UUID.randomUUID(), FailedOperationEnum.CREATE, null, null, null, null);

        @Test
        @DisplayName("Deve delegar o aviso de falha ao service")
        void shouldDelegateToTheService() {
            transactionListener.handleTransactionFailed(event);

            verify(transactionFailureService).compensate(event);
        }

        @Test
        @DisplayName("Deve propagar a exceção quando a compensação falhar")
        void shouldPropagateFailure() {
            doThrow(new IllegalStateException("falha")).when(transactionFailureService).compensate(event);

            assertThrows(IllegalStateException.class, () -> transactionListener.handleTransactionFailed(event));
        }
    }

    @Nested
    @DisplayName("Cenários do handleCreditCardInvoicePayment")
    class InvoicePaymentTests {

        private final InvoicePaymentDTO payment = new InvoicePaymentDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cartão C6",
                UUID.randomUUID(), 3, 2026, new BigDecimal("100.00"), UUID.randomUUID()
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
