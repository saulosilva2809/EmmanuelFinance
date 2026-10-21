package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardQueryServiceTest {

    @Mock
    private CreditCardDocumentSelector creditCardDocumentSelector;

    @Mock
    private CreditCardMapper creditCardMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CreditCardQueryService creditCardQueryService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldReturnTheCreditCardOfTheUser() {
        CreditCardDocument document = newDocument();
        ResponseCreditCardDTO expected = newResponse();
        when(creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId())).thenReturn(document);
        when(creditCardMapper.toResponseDTO(document)).thenReturn(expected);

        assertEquals(expected, creditCardQueryService.view(document.getId()));
    }

    @Test
    void shouldPropagateNotFoundWhenTheCreditCardDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(id)).thenThrow(new CreditCardNotFound());

        assertThrows(CreditCardNotFound.class, () -> creditCardQueryService.view(id));
        verifyNoInteractions(creditCardMapper);
    }

    @Test
    void shouldReturnAPageOfCreditCards() {
        Pageable pageable = PageRequest.of(0, 10);
        CreditCardDocument document = newDocument();
        ResponseCreditCardDTO expected = newResponse();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CreditCardDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(CreditCardDocument.class))).thenReturn(List.of(document));
        when(creditCardMapper.toResponseDTO(document)).thenReturn(expected);

        PageResponseDTO<ResponseCreditCardDTO> result =
                creditCardQueryService.list(new CreditCardFiltersDTO(null, null, null), pageable);

        assertEquals(1, result.content().size());
        assertEquals(expected, result.content().get(0));
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldReturnAnEmptyPageOfDeletedCreditCards() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CreditCardDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(CreditCardDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseCreditCardDTO> result =
                creditCardQueryService.listDeleted(new CreditCardFiltersDTO(null, null, null), pageable);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    private CreditCardDocument newDocument() {
        CreditCardDocument document = new CreditCardDocument();
        document.setId(UUID.randomUUID());
        document.setUserId(userId);
        return document;
    }

    private ResponseCreditCardDTO newResponse() {
        CreditCard creditCard = CreditCardTestDataBuilder.createEntity(
                CreditCardTestDataBuilder.createCardDTO(), userId, false);
        return CreditCardTestDataBuilder.responseCategoryDTO(
                creditCard, CreditCardTestDataBuilder.accountSummaryInternalDTO(creditCard.getAccountId()));
    }
}
