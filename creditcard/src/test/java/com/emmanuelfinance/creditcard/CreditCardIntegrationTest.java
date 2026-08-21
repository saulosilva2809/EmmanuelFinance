package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.services.CreditCardService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class CreditCardIntegrationTest {

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private AccountClientCacheService accountClientCacheService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        AccountSummaryInternalDTO summaryInternalDTO = CreditCardTestDataBuilder.accountSummaryInternalDTO(UUID.randomUUID());
        AccountSummaryDTO fakeAccountSummary = CreditCardTestDataBuilder.accountSummaryDTO(summaryInternalDTO);
        when(accountClientCacheService.getAccountSummaryById(any()))
                .thenReturn(fakeAccountSummary);
    }

    @Nested
    @DisplayName("Tests of listDeleted method")
    class ListDeletedMethodTests {

        @Test
        void shouldListDeletedCards() {
            creditCardRepository.deleteAll();

            CreateCreditCardDTO creditCardActiveDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardActiveEntity = CreditCardTestDataBuilder.createEntity(
                    creditCardActiveDTO,
                    false
            );
            creditCardActiveEntity.setUserId(userId);

            CreateCreditCardDTO creditCardDeletedDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardDeletedEntity = CreditCardTestDataBuilder.createEntity(
                    creditCardDeletedDTO,
                    true
            );
            creditCardDeletedEntity.setUserId(userId);

            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(
                    null,
                    null,
                    null
            );

            creditCardRepository.save(creditCardActiveEntity);
            CreditCard savedDeletedCard = creditCardRepository.save(creditCardDeletedEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.listDeleted(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(savedDeletedCard.getId(), response.content().get(0).id());
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldListTheActiveCards() {
            creditCardRepository.deleteAll();

            CreateCreditCardDTO creditCardActiveDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardActiveEntity = CreditCardTestDataBuilder.createEntity(
                    creditCardActiveDTO,
                    false
            );
            creditCardActiveEntity.setUserId(userId);

            CreateCreditCardDTO creditCardDeletedDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardDeletedEntity = CreditCardTestDataBuilder.createEntity(
                    creditCardDeletedDTO,
                    true
            );
            creditCardDeletedEntity.setUserId(userId);

            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(
                    null,
                    null,
                    null
            );

            creditCardRepository.save(creditCardDeletedEntity);
            CreditCard savedCard = creditCardRepository.save(creditCardActiveEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.list(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(savedCard.getId(), response.content().get(0).id());
        }
    }
}
