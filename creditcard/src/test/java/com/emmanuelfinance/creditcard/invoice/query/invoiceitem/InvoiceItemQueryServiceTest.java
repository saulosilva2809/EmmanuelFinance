package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocumentRepository;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceItemQueryServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private InvoiceDocumentRepository invoiceDocumentRepository;

    @Mock
    private TransactionClientCacheService transactionClientCacheService;

    @Spy
    private InvoiceMapper invoiceMapper = Mappers.getMapper(InvoiceMapper.class);

    @InjectMocks
    private InvoiceItemQueryService invoiceItemQueryService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldReturnTheItemsWithTheInvoiceAndTransactionSummaries() {
        Pageable pageable = PageRequest.of(0, 10);
        InvoiceDocument invoice = newInvoice();
        InvoiceItemDocument item = newItem(invoice.getId());
        TransactionSummaryDTO transaction = InvoiceTestDataBuilder.transactionSummaryDTO();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(invoiceDocumentRepository.findByIdAndUserIdAndDeletedFalse(invoice.getId(), userId))
                .thenReturn(Optional.of(invoice));
        when(mongoTemplate.count(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(List.of(item));
        when(transactionClientCacheService.getTransactionSummaryDTO(item.getTransactionId())).thenReturn(transaction);

        PageResponseDTO<ResponseInvoiceItemDTO> result =
                invoiceItemQueryService.listByInvoiceId(invoice.getId(), pageable);

        assertEquals(1, result.totalElements());
        ResponseInvoiceItemDTO dto = result.content().get(0);
        assertEquals(invoice.getId(), dto.invoice().id());
        assertEquals(10, dto.invoice().month());
        assertEquals(transaction, dto.transaction());
        assertEquals(2, dto.installmentNumber());
        assertEquals(3, dto.totalInstallments());
        assertEquals(new BigDecimal("100.00"), dto.amount());
    }

    @Test
    void shouldThrowInvoiceNotFoundAndNotSearchItemsWhenTheInvoiceIsNotFromTheUser() {
        UUID invoiceId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(invoiceDocumentRepository.findByIdAndUserIdAndDeletedFalse(invoiceId, userId))
                .thenReturn(Optional.empty());

        InvoiceDomainException exception = assertThrows(InvoiceDomainException.class,
                () -> invoiceItemQueryService.listByInvoiceId(invoiceId, PageRequest.of(0, 10)));

        assertEquals(InvoiceErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(mongoTemplate, transactionClientCacheService);
    }

    @Test
    void shouldReturnAnEmptyPageWhenTheInvoiceHasNoItems() {
        InvoiceDocument invoice = newInvoice();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(invoiceDocumentRepository.findByIdAndUserIdAndDeletedFalse(invoice.getId(), userId))
                .thenReturn(Optional.of(invoice));
        when(mongoTemplate.count(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseInvoiceItemDTO> result =
                invoiceItemQueryService.listByInvoiceId(invoice.getId(), PageRequest.of(0, 10));

        assertTrue(result.content().isEmpty());
        verifyNoInteractions(transactionClientCacheService);
    }

    private InvoiceDocument newInvoice() {
        InvoiceDocument invoice = new InvoiceDocument();
        invoice.setId(UUID.randomUUID());
        invoice.setUserId(userId);
        invoice.setMonth(10);
        invoice.setYear(2026);
        invoice.setTotalAmount(new BigDecimal("300.00"));
        invoice.setStatus(InvoiceStatusEnum.OPEN);
        return invoice;
    }

    private InvoiceItemDocument newItem(UUID invoiceId) {
        InvoiceItemDocument item = new InvoiceItemDocument();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setInvoiceId(invoiceId);
        item.setTransactionId(UUID.randomUUID());
        item.setInstallmentNumber(2);
        item.setTotalInstallments(3);
        item.setAmount(new BigDecimal("100.00"));
        return item;
    }
}
