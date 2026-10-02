package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceItemServiceTest {

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private InvoiceServiceInternal invoiceServiceInternal;

    @Mock
    private TransactionClientCacheService transactionClientCacheService;

    @InjectMocks
    private InvoiceItemService invoiceItemService;

    @Nested
    @DisplayName("Cenários do createInvoiceItem")
    class CreateInvoiceItemTests {

        @Test
        @DisplayName("Deve salvar o item com os dados do evento e da parcela")
        void shouldSaveItemWithEventAndInstallmentData() {
            UUID userId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, UUID.randomUUID(), new BigDecimal("300.00"), 3, LocalDateTime.now()
            );

            invoiceItemService.createInvoiceItem(event, invoiceId, 2, new BigDecimal("100.00"));

            ArgumentCaptor<InvoiceItem> captor = ArgumentCaptor.forClass(InvoiceItem.class);
            verify(invoiceItemRepository, times(1)).save(captor.capture());
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
    @DisplayName("Cenários do listByInvoiceId")
    class ListByInvoiceIdTests {

        @Test
        @DisplayName("Deve retornar página de DTOs com resumo da fatura e da transação")
        void shouldReturnPageOfDTOs() {
            UUID userId = UUID.randomUUID();
            Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(userId, UUID.randomUUID());
            InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(userId, invoice.getId(), UUID.randomUUID());
            ResponseInvoiceSummaryDTO invoiceSummary = InvoiceTestDataBuilder.responseInvoiceSummaryDTO(invoice);
            TransactionSummaryDTO transactionSummary = InvoiceTestDataBuilder.transactionSummaryDTO();
            Pageable pageable = PageRequest.of(0, 10);

            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(invoiceItemRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(item), pageable, 1));
            when(invoiceServiceInternal.invoiceSummaryDTO(invoice.getId())).thenReturn(invoiceSummary);
            when(transactionClientCacheService.getTransactionSummaryDTO(item.getTransactionId()))
                    .thenReturn(transactionSummary);

            PageResponseDTO<ResponseInvoiceItemDTO> result = invoiceItemService.listByInvoiceId(invoice.getId(), pageable);

            assertEquals(1, result.content().size());
            assertEquals(1L, result.totalElements());
            assertEquals(
                    InvoiceTestDataBuilder.responseInvoiceItemDTO(item, invoiceSummary, transactionSummary),
                    result.content().get(0)
            );
            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver itens")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
            when(invoiceItemRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            PageResponseDTO<ResponseInvoiceItemDTO> result = invoiceItemService.listByInvoiceId(UUID.randomUUID(), pageable);

            assertTrue(result.content().isEmpty());
            verifyNoInteractions(invoiceServiceInternal, transactionClientCacheService);
        }
    }

    @Nested
    @DisplayName("Cenários do delete")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar os itens pela transação")
        void shouldDeleteByTransactionId() {
            UUID transactionId = UUID.randomUUID();

            invoiceItemService.delete(transactionId);

            verify(invoiceItemRepository, times(1)).deleteByTransactionId(transactionId);
        }
    }

    @Nested
    @DisplayName("Cenários do restore")
    class RestoreTests {

        @Test
        @DisplayName("Deve marcar os itens da transação como não excluídos")
        void shouldRestoreItems() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            InvoiceItem item1 = InvoiceTestDataBuilder.invoiceItemEntity(
                    userId, UUID.randomUUID(), transactionId, 1, 2, new BigDecimal("50.00"), true
            );
            InvoiceItem item2 = InvoiceTestDataBuilder.invoiceItemEntity(
                    userId, UUID.randomUUID(), transactionId, 2, 2, new BigDecimal("50.00"), true
            );
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(List.of(item1, item2));

            invoiceItemService.restore(transactionId);

            assertFalse(item1.isDeleted());
            assertFalse(item2.isDeleted());
        }

        @Test
        @DisplayName("Não deve lançar erro quando não houver itens para restaurar")
        void shouldNotThrowWhenNoItems() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceItemRepository.findByTransactionId(transactionId)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> invoiceItemService.restore(transactionId));
        }
    }
}
