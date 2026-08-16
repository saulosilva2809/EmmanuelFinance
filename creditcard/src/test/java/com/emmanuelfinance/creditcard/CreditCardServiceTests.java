package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.enums.BanksEnum;
import com.emmanuelfinance.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardServiceTests {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private AccountClientCacheService accountClientCacheService;

    @Spy
    private CreditCardMapper creditCardMapper = Mappers.getMapper(CreditCardMapper.class);

    @InjectMocks
    private CreditCardService creditCardService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        userId = securityUtils.getCurrentUserId();
    }

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

        @Test
        void shouldReturnAccountNotFoundError() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();

            doThrow(new AccountNotFound())
                    .when(accountOwnershipValidator)
                    .validate(creditCardDTO.accountId());

            assertThrows(AccountNotFound.class, () -> {
                creditCardService.create(creditCardDTO);
            });

            verify(accountOwnershipValidator, times(1)).validate(creditCardDTO.accountId());
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        void shouldCreateTheCardSuccessfully() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);
            AccountSummaryDTO mockAccount = CreditCardTestDataBuilder.accountSummaryDTO(creditCardEntity.getAccountId());
            ResponseCreditCardDTO expectedResponse = CreditCardTestDataBuilder.responseCategoryDTO(
                    creditCardEntity,
                    mockAccount
            );

            doNothing()
                    .when(accountOwnershipValidator)
                    .validate(creditCardEntity.getAccountId());
            when(accountClientCacheService.getInternalAccountById(creditCardEntity.getAccountId())).thenReturn(mockAccount);
            when(creditCardRepository.save(any(CreditCard.class))).thenReturn(creditCardEntity);

            ResponseCreditCardDTO response = creditCardService.create(creditCardDTO);

            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.account().id(), response.account().id());

            verify(accountOwnershipValidator, times(1)).validate(creditCardEntity.getAccountId());
            verify(creditCardRepository, times(1)).save(any(CreditCard.class));
        }
    }

    @Nested
    @DisplayName("Tests of view method")
    class ViewMethodTests {

        @Test
        void shouldReturnCardNotFoundError() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.empty());

            assertThrows(CreditCardNotFound.class, () -> {
                creditCardService.view(creditCardEntity.getId());
            });

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            );
        }

        @Test
        void shouldReturnTheCardSuccessfully() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);
            AccountSummaryDTO mockAccount = CreditCardTestDataBuilder.accountSummaryDTO(creditCardEntity.getAccountId());
            ResponseCreditCardDTO expectedResponse = CreditCardTestDataBuilder.responseCategoryDTO(
                    creditCardEntity,
                    mockAccount
            );

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.of(creditCardEntity));

            ResponseCreditCardDTO response = creditCardService.view(creditCardEntity.getId());

            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            );
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldReturnABlankPage() {
            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(
                    null,
                    null,
                    BanksEnum.PAGBANK
            );

            Pageable pageable = PageRequest.of(0, 10);
            Page<CreditCard> creditCardPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(creditCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(creditCardPage);
            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.list(filters, pageable);

            assertNotNull(response);
            assertTrue(response.content().isEmpty());

            verify(creditCardRepository, times(1)).findAll(
                    any(Specification.class),
                    any(Pageable.class)
            );
        }

        @Test
        void shouldReturnTheCardSuccessfully() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);
            AccountSummaryDTO mockAccount = CreditCardTestDataBuilder.accountSummaryDTO(creditCardEntity.getAccountId());
            ResponseCreditCardDTO expectedResponse = CreditCardTestDataBuilder.responseCategoryDTO(
                    creditCardEntity,
                    mockAccount
            );

            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(
                    mockAccount.id(),
                    "Nubank",
                    BanksEnum.NUBANK
            );

            Pageable pageable = PageRequest.of(0, 10);
            List<CreditCard> creditCardList = List.of(creditCardEntity);
            Page<CreditCard> creditCardPage = new PageImpl<>(creditCardList, pageable, creditCardList.size());

            when(creditCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(creditCardPage);
            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.list(filters, pageable);

            assertNotNull(response);
            assertNotNull(response.content());
            assertEquals(1, response.content().size());
            assertEquals(expectedResponse.id(), response.content().get(0).id());
            assertEquals(expectedResponse.name(), response.content().get(0).name());

            verify(creditCardRepository, times(1)).findAll(
                    any(Specification.class),
                    any(Pageable.class)
            );
        }
    }

    @Nested
    @DisplayName("Tests of update method")
    class UpdateMethodTests {

        @Test
        void shouldReturnCardNotFoundError() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);

            UpdateCreditCardDTO updateCreditCardDTO = new UpdateCreditCardDTO(
                    null,
                    "Conta C6 Bank",
                    BanksEnum.C6_BANK,
                    new BigDecimal(15000),
                    null,
                    null
            );

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.empty());

            assertThrows(CreditCardNotFound.class, () -> {
                creditCardService.update(creditCardEntity.getId(), updateCreditCardDTO);
            });

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            );
        }

        @Test
        void shouldReturnAccountNotFoundError() {
            UUID accountNotFoundId = UUID.randomUUID();

            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);

            UpdateCreditCardDTO updateCreditCardDTO = new UpdateCreditCardDTO(
                    accountNotFoundId,
                    "Conta C6 Bank",
                    BanksEnum.C6_BANK,
                    new BigDecimal(15000),
                    null,
                    null
            );

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.of(creditCardEntity));

            doThrow(new AccountNotFound())
                    .when(accountOwnershipValidator)
                    .validate(accountNotFoundId);

            assertThrows(AccountNotFound.class, () -> {
                creditCardService.update(creditCardEntity.getId(), updateCreditCardDTO);
            });

            verify(accountOwnershipValidator, times(1)).validate(accountNotFoundId);
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        void shouldUpdateAnAccountSuccessfully() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);
            AccountSummaryDTO mockAccount = CreditCardTestDataBuilder.accountSummaryDTO(creditCardEntity.getAccountId());


            UpdateCreditCardDTO updateCreditCardDTO = new UpdateCreditCardDTO(
                    null,
                    "Conta C6 Bank",
                    BanksEnum.C6_BANK,
                    new BigDecimal(15000),
                    null,
                    null
            );

            when(accountClientCacheService.getInternalAccountById(creditCardEntity.getAccountId())).thenReturn(mockAccount);
            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.of(creditCardEntity));
            when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(
                    invocation -> invocation.getArgument(0)
            );

            ResponseCreditCardDTO response = creditCardService.update(
                    creditCardEntity.getId(),
                    updateCreditCardDTO
            );

            assertEquals(creditCardEntity.getAccountId(), response.account().id());
            assertEquals(updateCreditCardDTO.name(), response.name());
            assertEquals(updateCreditCardDTO.bank(), response.bank());

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            );
        }
    }

    @Nested
    @DisplayName("Tests of delete method")
    class DeleteMethodTests {

        @Test
        void shouldReturnCardNotFoundError() {
            UUID cardNotFoundId = UUID.randomUUID();

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    cardNotFoundId,
                    userId
            )).thenReturn(Optional.empty());

            assertThrows(CreditCardNotFound.class, () -> {
                creditCardService.delete(cardNotFoundId);
            });

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    cardNotFoundId,
                    userId
            );
        }

        @Test
        void shouldDeleteAnAccountSuccessfully() {
            CreateCreditCardDTO creditCardDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCardEntity = CreditCardTestDataBuilder.createEntity(creditCardDTO);

            when(creditCardRepository.findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            )).thenReturn(Optional.of(creditCardEntity));

            creditCardService.delete(creditCardEntity.getId());

            verify(creditCardRepository, times(1)).findByIdAndUserIdAndDeletedFalse(
                    creditCardEntity.getId(),
                    userId
            );
        }
    }
}
