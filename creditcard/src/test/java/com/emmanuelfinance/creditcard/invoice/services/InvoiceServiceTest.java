package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceItemSelector;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceSelector invoiceSelector;

    @Mock
    private CreditCardClientCacheService creditCardClientCacheService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemService invoiceItemService;

    @Mock
    private InvoiceItemSelector invoiceItemSelector;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private InvoiceService invoiceService;

    @Nested
    @DisplayName("Cenários do findOrCreate")
    class FindOrCreateTests {

        private final UUID userId = UUID.randomUUID();
        private final UUID creditCardId = UUID.randomUUID();

        private void stubCreditCard() {
            when(creditCardClientCacheService.getCreditCardInternalSummaryDTO(creditCardId))
                    .thenReturn(InvoiceTestDataBuilder.creditCardInternalSummaryDTO(creditCardId));
        }

        private void stubSaveGeneratingId() {
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
                Invoice invoice = invocation.getArgument(0);
                if (invoice.getId() == null) {
                    invoice.setId(UUID.randomUUID());
                }
                return invoice;
            });
        }

        @Test
        @DisplayName("Deve criar fatura e item quando for 1 parcela e a fatura não existir")
        void shouldCreateInvoiceAndItemWhenSingleInstallmentAndInvoiceDoesNotExist() {
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("100.00"), 1, LocalDateTime.of(2026, 10, 5, 10, 0)
            );
            when(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026))
                    .thenReturn(Optional.empty());
            stubCreditCard();
            stubSaveGeneratingId();

            invoiceService.findOrCreate(event);

            ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
            verify(invoiceRepository, times(1)).save(captor.capture());
            Invoice saved = captor.getValue();

            assertEquals(userId, saved.getUserId());
            assertEquals(creditCardId, saved.getCreditCardId());
            assertEquals(10, saved.getMonth());
            assertEquals(2026, saved.getYear());
            assertEquals(InvoiceTestDataBuilder.DUE_DATE, saved.getDueDate());
            assertEquals(InvoiceTestDataBuilder.CLOSING_DATE, saved.getClosingDate());
            assertEquals(new BigDecimal("100.00"), saved.getTotalAmount());
            assertEquals(InvoiceStatusEnum.OPEN, saved.getStatus());

            verify(invoiceItemService, times(1)).createInvoiceItem(
                    event, saved.getId(), 1, new BigDecimal("100.00")
            );
        }

        @Test
        @DisplayName("Deve tratar installmentsCount nulo como 1 parcela")
        void shouldTreatNullInstallmentsCountAsOne() {
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("80.00"), null, LocalDateTime.of(2026, 10, 5, 10, 0)
            );
            when(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026))
                    .thenReturn(Optional.empty());
            stubCreditCard();
            stubSaveGeneratingId();

            invoiceService.findOrCreate(event);

            verify(invoiceRepository, times(1)).save(any(Invoice.class));
            verify(invoiceItemService, times(1)).createInvoiceItem(
                    eq(event), any(UUID.class), eq(1), eq(new BigDecimal("80.00"))
            );
        }

        @Test
        @DisplayName("Deve criar uma fatura e um item por parcela, com meses consecutivos e virada de ano")
        void shouldCreateOneInvoiceAndItemPerInstallmentAcrossYearBoundary() {
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("100.00"), 3, LocalDateTime.of(2026, 11, 20, 10, 0)
            );
            when(invoiceSelector.getByCreditCardAndMonthAndYear(eq(creditCardId), anyInt(), anyInt()))
                    .thenReturn(Optional.empty());
            stubCreditCard();
            stubSaveGeneratingId();

            invoiceService.findOrCreate(event);

            ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
            verify(invoiceRepository, times(3)).save(captor.capture());
            List<Invoice> saved = captor.getAllValues();

            assertEquals(11, saved.get(0).getMonth());
            assertEquals(2026, saved.get(0).getYear());
            assertEquals(12, saved.get(1).getMonth());
            assertEquals(2026, saved.get(1).getYear());
            assertEquals(1, saved.get(2).getMonth());
            assertEquals(2027, saved.get(2).getYear());
            saved.forEach(invoice -> assertEquals(new BigDecimal("33.33"), invoice.getTotalAmount()));

            for (int i = 0; i < 3; i++) {
                verify(invoiceItemService, times(1)).createInvoiceItem(
                        event, saved.get(i).getId(), i + 1, new BigDecimal("33.33")
                );
            }
        }

        @Test
        @DisplayName("Deve somar o valor da parcela na fatura existente sem criar uma nova")
        void shouldAddInstallmentAmountToExistingInvoice() {
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("100.00"), 2, LocalDateTime.of(2026, 10, 5, 10, 0)
            );
            Invoice existing = InvoiceTestDataBuilder.invoiceEntity(userId, creditCardId, 10, 2026);
            existing.setTotalAmount(new BigDecimal("200.00"));

            when(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026))
                    .thenReturn(Optional.of(existing));
            when(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 11, 2026))
                    .thenReturn(Optional.of(InvoiceTestDataBuilder.invoiceEntity(userId, creditCardId, 11, 2026)));

            invoiceService.findOrCreate(event);

            // parcela = 50.00; 200 + 50 (e não 200 + 100, o valor total da compra)
            assertEquals(new BigDecimal("250.00"), existing.getTotalAmount());
            verify(invoiceRepository, times(2)).save(any(Invoice.class));
            verify(creditCardClientCacheService, never()).getCreditCardInternalSummaryDTO(any());
            verify(invoiceItemService, times(1)).createInvoiceItem(
                    event, existing.getId(), 1, new BigDecimal("50.00")
            );
        }

        @Test
        @DisplayName("Deve tratar totalAmount nulo da fatura existente como zero")
        void shouldTreatNullTotalAmountAsZeroOnExistingInvoice() {
            TransactionCreatedEvent event = InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("100.00"), 1, LocalDateTime.of(2026, 10, 5, 10, 0)
            );
            Invoice existing = InvoiceTestDataBuilder.invoiceEntity(userId, creditCardId, 10, 2026);
            existing.setTotalAmount(null);

            when(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026))
                    .thenReturn(Optional.of(existing));

            invoiceService.findOrCreate(event);

            assertEquals(new BigDecimal("100.00"), existing.getTotalAmount());
            verify(invoiceRepository, times(1)).save(existing);
        }
    }

    @Nested
    @DisplayName("Cenários do list")
    class ListTests {

        @Test
        @DisplayName("Deve retornar página de DTOs com o resumo do cartão")
        void shouldReturnPageOfDTOs() {
            UUID userId = UUID.randomUUID();
            UUID creditCardId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(userId, creditCardId);
            CreditCardSummaryDTO cardSummary = InvoiceTestDataBuilder.creditCardSummaryDTO(creditCardId);

            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(invoiceRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));
            when(creditCardClientCacheService.getCreditCardSummaryDTO(creditCardId)).thenReturn(cardSummary);

            InvoiceFiltersDTO filters = new InvoiceFiltersDTO(creditCardId, 10, 2026, InvoiceStatusEnum.OPEN);
            PageResponseDTO<ResponseInvoiceDTO> result = invoiceService.list(filters, pageable);

            assertEquals(1, result.content().size());
            assertEquals(1L, result.totalElements());
            assertEquals(1, result.totalPages());
            assertEquals(InvoiceTestDataBuilder.responseInvoiceDTO(invoice, cardSummary), result.content().get(0));
            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver faturas")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
            when(invoiceRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            PageResponseDTO<ResponseInvoiceDTO> result = invoiceService.list(
                    new InvoiceFiltersDTO(null, null, null, null), pageable
            );

            assertTrue(result.content().isEmpty());
            assertEquals(0L, result.totalElements());
            verifyNoInteractions(creditCardClientCacheService);
        }
    }

    @Nested
    @DisplayName("Cenários do delete")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar itens e faturas distintas da transação")
        void shouldDeleteItemsAndDistinctInvoices() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID invoiceA = UUID.randomUUID();
            UUID invoiceB = UUID.randomUUID();
            List<InvoiceItem> items = List.of(
                    InvoiceTestDataBuilder.invoiceItemEntity(userId, invoiceA, transactionId),
                    InvoiceTestDataBuilder.invoiceItemEntity(userId, invoiceA, transactionId),
                    InvoiceTestDataBuilder.invoiceItemEntity(userId, invoiceB, transactionId)
            );
            when(invoiceItemSelector.getByTransactionId(transactionId)).thenReturn(items);

            TransactionDeletedAndRestoreEvent event = InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(transactionId);
            invoiceService.delete(event);

            verify(invoiceItemService, times(1)).delete(transactionId);
            verify(invoiceRepository, times(1)).deleteAllById(List.of(invoiceA, invoiceB));
        }

        @Test
        @DisplayName("Não deve deletar faturas quando a transação não tiver itens")
        void shouldNotDeleteInvoicesWhenNoItems() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceItemSelector.getByTransactionId(transactionId)).thenReturn(Collections.emptyList());

            invoiceService.delete(InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(transactionId));

            verify(invoiceItemService, times(1)).delete(transactionId);
            verify(invoiceRepository, never()).deleteAllById(anyList());
        }
    }

    @Nested
    @DisplayName("Cenários do restore")
    class RestoreTests {

        @Test
        @DisplayName("Deve restaurar itens e marcar faturas como não excluídas")
        void shouldRestoreItemsAndInvoices() {
            UUID userId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(
                    userId, UUID.randomUUID(), 10, 2026, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, true
            );
            InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(userId, invoice.getId(), transactionId);

            when(invoiceItemSelector.getByTransactionId(transactionId)).thenReturn(List.of(item, item));
            when(invoiceRepository.findAllById(List.of(invoice.getId()))).thenReturn(List.of(invoice));

            invoiceService.restore(InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(transactionId));

            verify(invoiceItemService, times(1)).restore(transactionId);
            assertFalse(invoice.isDeleted());
        }

        @Test
        @DisplayName("Não deve buscar faturas quando a transação não tiver itens")
        void shouldNotFindInvoicesWhenNoItems() {
            UUID transactionId = UUID.randomUUID();
            when(invoiceItemSelector.getByTransactionId(transactionId)).thenReturn(Collections.emptyList());

            invoiceService.restore(InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(transactionId));

            verify(invoiceItemService, times(1)).restore(transactionId);
            verify(invoiceRepository, never()).findAllById(anyList());
        }
    }
}
