package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.creditcard.creditcard.exceptions.CreditCardErrorCode;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private CreditCardMapper creditCardMapper;

    @Mock
    private CreditCardSelector creditCardSelector;

    @Mock
    private CreditCardValidatorService creditCardValidatorService;

    @Mock
    private CreditCardBalanceService creditCardBalanceService;

    @InjectMocks
    private CreditCardService creditCardService;

    private final UUID userId = UUID.randomUUID();

    private CreditCard cardEntity(boolean deleted) {
        return CreditCardTestDataBuilder.createEntity(CreditCardTestDataBuilder.createCardDTO(), userId, deleted);
    }

    private ResponseCreditCardDTO responseFor(CreditCard card) {
        return CreditCardTestDataBuilder.responseCategoryDTO(
                card, CreditCardTestDataBuilder.accountSummaryInternalDTO(card.getAccountId())
        );
    }

    private UpdateCreditCardDTO updateDTO(UUID accountId) {
        return new UpdateCreditCardDTO(accountId, "Cartão Atualizado", BanksEnum.C6_BANK, new BigDecimal("15000"), null, null);
    }

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

        @Test
        @DisplayName("Deve repassar AccountNotFound da validação e não salvar o cartão")
        void shouldReturnAccountNotFoundError() {
            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            doThrow(new AccountNotFound()).when(creditCardValidatorService).validateCreation(dto);

            assertThrows(AccountNotFound.class, () -> creditCardService.create(dto));

            verify(creditCardValidatorService, times(1)).validateCreation(dto);
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve repassar o erro de banco diferente entre cartão e conta e não salvar")
        void shouldGiveErrorWhenTryingToCreateCardWithBankDifferentFromAccount() {
            CreateCreditCardDTO dto = new CreateCreditCardDTO(
                    UUID.randomUUID(), "Cartão de Crédito C6", BanksEnum.NUBANK, new BigDecimal(10000), 17, 24
            );
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            doThrow(new CreditCardDomainException(CreditCardErrorCode.BANK_OF_CARD_AND_ACCOUNT_DIFFERENT))
                    .when(creditCardValidatorService).validateCreation(dto);

            CreditCardDomainException exception = assertThrows(
                    CreditCardDomainException.class, () -> creditCardService.create(dto)
            );

            assertEquals(CreditCardErrorCode.BANK_OF_CARD_AND_ACCOUNT_DIFFERENT, exception.getErrorCode());
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve criar o cartão com usuário e limite disponível igual ao limite total")
        void shouldCreateTheCardSuccessfully() {
            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard mapped = new CreditCard();
            mapped.setAccountId(dto.accountId());
            CreditCard saved = CreditCardTestDataBuilder.createEntity(dto, userId, false);
            ResponseCreditCardDTO expected = responseFor(saved);

            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardMapper.toEntity(dto)).thenReturn(mapped);
            when(creditCardRepository.save(mapped)).thenReturn(saved);
            when(creditCardMapper.toResponseDTO(saved)).thenReturn(expected);

            ResponseCreditCardDTO response = creditCardService.create(dto);

            assertEquals(expected, response);
            assertEquals(userId, mapped.getUserId());
            assertEquals(dto.creditLimit(), mapped.getCreditLimit());
            assertEquals(dto.creditLimit(), mapped.getAvailableLimit());
            verify(creditCardValidatorService, times(1)).validateCreation(dto);
            verify(creditCardRepository, times(1)).save(mapped);
        }
    }

    @Nested
    @DisplayName("Tests of view method")
    class ViewMethodTests {

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão não for encontrado")
        void shouldReturnCardNotFoundError() {
            UUID cardId = UUID.randomUUID();
            when(creditCardSelector.getCreditCardById(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardService.view(cardId));

            verify(creditCardSelector, times(1)).getCreditCardById(cardId);
            verifyNoInteractions(creditCardMapper);
        }

        @Test
        @DisplayName("Deve retornar o DTO do cartão encontrado")
        void shouldReturnTheCardSuccessfully() {
            CreditCard card = cardEntity(false);
            ResponseCreditCardDTO expected = responseFor(card);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);
            when(creditCardMapper.toResponseDTO(card)).thenReturn(expected);

            ResponseCreditCardDTO response = creditCardService.view(card.getId());

            assertEquals(expected, response);
            verify(creditCardSelector, times(1)).getCreditCardById(card.getId());
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("Deve retornar página vazia quando não houver cartões")
        void shouldReturnABlankPage() {
            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(null, null, BanksEnum.PAGBANK);
            Page<CreditCard> page = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.list(filters, pageable);

            assertNotNull(response);
            assertTrue(response.content().isEmpty());
            verify(creditCardRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Deve retornar os cartões mapeados para DTO")
        void shouldReturnTheCardsSuccessfully() {
            CreditCard card = cardEntity(false);
            ResponseCreditCardDTO expected = responseFor(card);
            CreditCardFiltersDTO filters = new CreditCardFiltersDTO(card.getAccountId(), "Nubank", BanksEnum.NUBANK);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(card), pageable, 1));
            when(creditCardMapper.toResponseDTO(card)).thenReturn(expected);

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.list(filters, pageable);

            assertEquals(List.of(expected), response.content());
            assertEquals(1L, response.totalElements());
        }

        @Test
        @DisplayName("Deve listar os cartões excluídos mapeados para DTO")
        void shouldListDeletedCards() {
            CreditCard card = cardEntity(true);
            ResponseCreditCardDTO expected = responseFor(card);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(card), pageable, 1));
            when(creditCardMapper.toResponseDTO(card)).thenReturn(expected);

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.listDeleted(
                    new CreditCardFiltersDTO(null, null, null), pageable
            );

            assertEquals(List.of(expected), response.content());
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver cartões excluídos")
        void shouldReturnBlankPageOfDeletedCards() {
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(creditCardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

            PageResponseDTO<ResponseCreditCardDTO> response = creditCardService.listDeleted(
                    new CreditCardFiltersDTO(null, null, null), pageable
            );

            assertTrue(response.content().isEmpty());
            verifyNoInteractions(creditCardMapper);
        }
    }

    @Nested
    @DisplayName("Tests of update method")
    class UpdateMethodTests {

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão não for encontrado")
        void shouldReturnCardNotFoundError() {
            UUID cardId = UUID.randomUUID();
            when(creditCardSelector.getCreditCardById(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardService.update(cardId, updateDTO(null)));

            verifyNoInteractions(creditCardBalanceService, creditCardRepository, accountOwnershipValidator);
        }

        @Test
        @DisplayName("Deve lançar AccountNotFound quando a nova conta não pertencer ao usuário")
        void shouldReturnAccountNotFoundError() {
            UUID accountNotFoundId = UUID.randomUUID();
            CreditCard card = cardEntity(false);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);
            doThrow(new AccountNotFound()).when(accountOwnershipValidator).validate(accountNotFoundId);

            assertThrows(AccountNotFound.class,
                    () -> creditCardService.update(card.getId(), updateDTO(accountNotFoundId)));

            verify(accountOwnershipValidator, times(1)).validate(accountNotFoundId);
            verifyNoInteractions(creditCardBalanceService);
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve propagar o erro de limite e não salvar quando o limite ficar negativo")
        void shouldNotSaveWhenLimitValidationFails() {
            CreditCard card = cardEntity(false);
            UpdateCreditCardDTO dto = updateDTO(null);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);
            doThrow(new CreditCardDomainException(CreditCardErrorCode.THE_AVAILABLE_LIMIT_CANT_BE_NEGATIVE))
                    .when(creditCardBalanceService).updateAvailableLimit(card, dto);

            assertThrows(CreditCardDomainException.class, () -> creditCardService.update(card.getId(), dto));

            verify(creditCardRepository, never()).save(any(CreditCard.class));
            verifyNoInteractions(creditCardMapper);
        }

        @Test
        @DisplayName("Deve atualizar o cartão sem validar a conta quando o accountId não for informado")
        void shouldUpdateTheCardSuccessfully() {
            CreditCard card = cardEntity(false);
            UpdateCreditCardDTO dto = updateDTO(null);
            ResponseCreditCardDTO expected = responseFor(card);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);
            when(creditCardRepository.save(card)).thenReturn(card);
            when(creditCardMapper.toResponseDTO(card)).thenReturn(expected);

            ResponseCreditCardDTO response = creditCardService.update(card.getId(), dto);

            assertEquals(expected, response);
            verify(creditCardBalanceService, times(1)).updateAvailableLimit(card, dto);
            verify(creditCardMapper, times(1)).updateCreditCardFromDTO(dto, card);
            verify(creditCardRepository, times(1)).save(card);
            verifyNoInteractions(accountOwnershipValidator);
        }

        @Test
        @DisplayName("Deve validar a posse da conta quando o accountId for informado")
        void shouldValidateAccountOwnershipWhenAccountIdIsInformed() {
            UUID newAccountId = UUID.randomUUID();
            CreditCard card = cardEntity(false);
            UpdateCreditCardDTO dto = updateDTO(newAccountId);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);
            when(creditCardRepository.save(card)).thenReturn(card);
            when(creditCardMapper.toResponseDTO(card)).thenReturn(responseFor(card));

            creditCardService.update(card.getId(), dto);

            verify(accountOwnershipValidator, times(1)).validate(newAccountId);
        }
    }

    @Nested
    @DisplayName("Tests of delete method")
    class DeleteMethodTests {

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão não for encontrado")
        void shouldReturnCardNotFoundError() {
            UUID cardId = UUID.randomUUID();
            when(creditCardSelector.getCreditCardById(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardService.delete(cardId));

            verify(creditCardRepository, never()).delete(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve excluir o cartão encontrado")
        void shouldDeleteTheCardSuccessfully() {
            CreditCard card = cardEntity(false);
            when(creditCardSelector.getCreditCardById(card.getId())).thenReturn(card);

            creditCardService.delete(card.getId());

            ArgumentCaptor<CreditCard> captor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository, times(1)).delete(captor.capture());
            assertEquals(card.getId(), captor.getValue().getId());
        }
    }

    @Nested
    @DisplayName("Tests of restore method")
    class RestoreMethodTests {

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão não existir")
        void shouldFailWhenTryingToRestoreNonexistentCard() {
            UUID cardId = UUID.randomUUID();
            when(creditCardSelector.getCreditCardByIdIncludingDeleted(cardId)).thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () -> creditCardService.restore(cardId));

            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve lançar erro e não salvar ao restaurar um cartão que não está excluído")
        void shouldGiveErrorWhenTryingToRestoreNonDeletedCard() {
            CreditCard card = cardEntity(false);
            when(creditCardSelector.getCreditCardByIdIncludingDeleted(card.getId())).thenReturn(card);
            doThrow(new CreditCardDomainException(CreditCardErrorCode.RESTORE_CARD_NOT_DELETED))
                    .when(creditCardValidatorService).validateRestoration(card);

            CreditCardDomainException exception = assertThrows(
                    CreditCardDomainException.class, () -> creditCardService.restore(card.getId())
            );

            assertEquals(CreditCardErrorCode.RESTORE_CARD_NOT_DELETED, exception.getErrorCode());
            assertFalse(card.isDeleted());
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve lançar erro e não salvar ao restaurar cartão cuja conta está excluída")
        void shouldGiveErrorWhenTryingToRestoreCardWithDeletedAccount() {
            CreditCard card = cardEntity(true);
            when(creditCardSelector.getCreditCardByIdIncludingDeleted(card.getId())).thenReturn(card);
            doThrow(new CreditCardDomainException(CreditCardErrorCode.RESTORE_CARD_WITH_DELETED_ACCOUNT))
                    .when(creditCardValidatorService).validateRestoration(card);

            CreditCardDomainException exception = assertThrows(
                    CreditCardDomainException.class, () -> creditCardService.restore(card.getId())
            );

            assertEquals(CreditCardErrorCode.RESTORE_CARD_WITH_DELETED_ACCOUNT, exception.getErrorCode());
            assertTrue(card.isDeleted());
            verify(creditCardRepository, never()).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Deve restaurar o cartão excluído e salvar")
        void shouldRestoreTheCardSuccessfully() {
            CreditCard card = cardEntity(true);
            when(creditCardSelector.getCreditCardByIdIncludingDeleted(card.getId())).thenReturn(card);

            creditCardService.restore(card.getId());

            assertFalse(card.isDeleted());
            verify(creditCardValidatorService, times(1)).validateRestoration(card);
            verify(creditCardRepository, times(1)).save(card);
        }
    }
}
