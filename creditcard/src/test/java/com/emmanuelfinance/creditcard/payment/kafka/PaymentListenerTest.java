package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.creditcard.payment.services.PaymentService;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentListenerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentListener paymentListener;

    @Test
    void shouldRevertTheFailedPayment() {
        InvoicePaymentFailedEvent event = new InvoicePaymentFailedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN);

        paymentListener.handleInvoicePaymentFailed(event);

        verify(paymentService).revertPayment(event);
    }
}
