package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CreditCardClientCacheService creditCardClientCacheService;

    @Spy
    private InvoiceMapper invoiceMapper = Mappers.getMapper(InvoiceMapper.class);

    @InjectMocks
    private InvoiceQueryService invoiceQueryService;

    @Test
    void shouldReturnThePageWithTheCreditCardSummary() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();
        InvoiceDocument document = newDocument(userId);
        CreditCardSummaryDTO creditCard = InvoiceTestDataBuilder.creditCardSummaryDTO(document.getCreditCardId());
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(InvoiceDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceDocument.class))).thenReturn(List.of(document));
        when(creditCardClientCacheService.getCreditCardSummaryDTO(document.getCreditCardId())).thenReturn(creditCard);

        PageResponseDTO<ResponseInvoiceDTO> result =
                invoiceQueryService.list(new InvoiceFiltersDTO(null, null, null, null), pageable);

        assertEquals(1, result.totalElements());
        ResponseInvoiceDTO invoice = result.content().get(0);
        assertEquals(document.getId(), invoice.id());
        assertEquals(creditCard, invoice.creditCard());
        assertEquals(10, invoice.month());
        assertEquals(new BigDecimal("100.00"), invoice.totalAmount());
        assertEquals(InvoiceStatusEnum.OPEN, invoice.status());
    }

    @Test
    void shouldReturnAnEmptyPageWithoutAskingForCreditCards() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(mongoTemplate.count(any(Query.class), eq(InvoiceDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseInvoiceDTO> result =
                invoiceQueryService.list(new InvoiceFiltersDTO(null, null, null, null), pageable);

        assertTrue(result.content().isEmpty());
        verifyNoInteractions(creditCardClientCacheService);
    }

    private InvoiceDocument newDocument(UUID userId) {
        InvoiceDocument document = new InvoiceDocument();
        document.setId(UUID.randomUUID());
        document.setUserId(userId);
        document.setCreditCardId(UUID.randomUUID());
        document.setMonth(10);
        document.setYear(2026);
        document.setDueDate(24);
        document.setClosingDate(17);
        document.setTotalAmount(new BigDecimal("100.00"));
        document.setStatus(InvoiceStatusEnum.OPEN);
        return document;
    }
}
