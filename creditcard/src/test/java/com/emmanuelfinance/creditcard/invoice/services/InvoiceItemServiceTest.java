package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceItemServiceTest {

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private InvoiceItemService invoiceItemService;

    @Nested
    @DisplayName("Cenários do createInvoiceItem")
    class CreateInvoiceItemTests {

        @Test
        @DisplayName("Deve salvar o item com os dados do evento e da parcela e registrar a foto no Outbox")
        void shouldSaveItemWithEventAndInstallmentData() {
            UUID userId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, UUID.randomUUID(), new BigDecimal("300.00"), 3, LocalDateTime.now()
            );

            invoiceItemService.createInvoiceItem(event, invoiceId, 2, new BigDecimal("100.00"));

            ArgumentCaptor<InvoiceItem> captor = ArgumentCaptor.forClass(InvoiceItem.class);
            InOrder inOrder = inOrder(invoiceItemRepository, outboxService);
            inOrder.verify(invoiceItemRepository).save(captor.capture());
            inOrder.verify(outboxService).invoiceItemChanged(captor.getValue());
            InvoiceItem saved = captor.getValue();

            assertEquals(userId, saved.getUserId());
            assertEquals(invoiceId, saved.getInvoiceId());
            assertEquals(event.transactionId(), saved.getTransactionId());
            assertEquals(2, saved.getInstallmentNumber());
            assertEquals(3, saved.getTotalInstallments());
            assertEquals(new BigDecimal("100.00"), saved.getAmount());
        }
    }

    @Nested
    @DisplayName("Cenários do delete")
    class DeleteTests {

        @Test
        @DisplayName("Deve excluir os itens da transação e registrar a exclusão de cada um no Outbox")
        void shouldDeleteTheItemsAndRegisterEachOneInTheOutbox() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            InvoiceItem item1 = InvoiceTestDataBuilder.invoiceItemEntity(userId, UUID.randomUUID(), transactionId);
            InvoiceItem item2 = InvoiceTestDataBuilder.invoiceItemEntity(userId, UUID.randomUUID(), transactionId);
            List<InvoiceItem> items = List.of(item1, item2);
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(items);

            invoiceItemService.delete(transactionId);

            verify(invoiceItemRepository, times(1)).deleteAll(items);
            verify(outboxService, times(1)).invoiceItemDeleted(item1);
            verify(outboxService, times(1)).invoiceItemDeleted(item2);
        }

        @Test
        @DisplayName("Não deve registrar nada no Outbox quando a transação não tem itens")
        void shouldNotRegisterAnythingWhenThereAreNoItems() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(Collections.emptyList());

            invoiceItemService.delete(transactionId);

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    @DisplayName("Cenários do restore")
    class RestoreTests {

        @Test
        @DisplayName("Deve marcar os itens como não excluídos, gravar e registrar a foto de cada um no Outbox")
        void shouldRestoreItems() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            InvoiceItem item1 = InvoiceTestDataBuilder.invoiceItemEntity(
                    userId, UUID.randomUUID(), transactionId, 1, 2, new BigDecimal("50.00"), true
            );
            InvoiceItem item2 = InvoiceTestDataBuilder.invoiceItemEntity(
                    userId, UUID.randomUUID(), transactionId, 2, 2, new BigDecimal("50.00"), true
            );
            List<InvoiceItem> items = List.of(item1, item2);
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(items);

            invoiceItemService.restore(transactionId);

            assertFalse(item1.isDeleted());
            assertFalse(item2.isDeleted());
            InOrder inOrder = inOrder(invoiceItemRepository, outboxService);
            inOrder.verify(invoiceItemRepository).saveAllAndFlush(items);
            inOrder.verify(outboxService).invoiceItemChanged(item1);
            inOrder.verify(outboxService).invoiceItemChanged(item2);
        }

        @Test
        @DisplayName("Não deve lançar erro quando não houver itens para restaurar")
        void shouldNotThrowWhenNoItems() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> invoiceItemService.restore(transactionId));
            verifyNoInteractions(outboxService);
        }
    }
}
