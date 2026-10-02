package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.swing.undo.CannotRedoException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
public class CreditCardSelectorTest {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private CreditCardSelector creditCardSelector;

    @BeforeEach
    void setUp() {
        creditCardSelector = new CreditCardSelector(creditCardRepository, securityUtils);
    }

    @Nested
    @DisplayName("Cenários do getCreditCardById")
    class GetCreditCardByIdTests {

        @Test
        @DisplayName("Deve buscar cartão por ID do usuário logado que não esteja excluído")
        void shouldReturnCardWhenFoundAndNotDeletedForCurrentUser() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, userId, false)
            );

            CreditCard result = creditCardSelector.getCreditCardById(card.getId());

            assertNotNull(result);
            assertEquals(card.getId(), result.getId());
            assertEquals(userId, result.getUserId());
            assertFalse(result.isDeleted());
            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão estiver marcado como excluído")
        void shouldThrowExceptionWhenCardIsDeleted() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, userId, true)
            );

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardSelector.getCreditCardById(card.getId())
            );

            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão pertencer a outro usuário")
        void shouldThrowExceptionWhenCardBelongsToAnotherUser() {
            UUID currentUser = UUID.randomUUID();
            UUID anotherUser = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser);

            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, anotherUser, false)
            );

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardSelector.getCreditCardById(card.getId())
            );

            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o ID do cartão não existir")
        void shouldThrowExceptionWhenCardDoesNotExist() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardSelector.getCreditCardById(UUID.randomUUID())
            );

            verify(securityUtils, times(1)).getCurrentUserId();
        }
    }

    @Nested
    @DisplayName("Cenários do getCreditCardByIdInternal")
    class GetCreditCardByIdInternalTests {

        @Test
        @DisplayName("Deve buscar cartão por ID sem filtrar por usuário ou deleção")
        void shouldReturnCardInternalRegardlessOfUserOrDeletedStatus() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, userId, true)
            );

            CreditCard result = creditCardSelector.getCreditCardByIdInternal(card.getId());

            assertNotNull(result);
            assertEquals(card.getId(), result.getId());
            assertTrue(result.isDeleted());
            verify(securityUtils, never()).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar CannotRedoException quando o cartão não for encontrado no fluxo interno")
        void shouldThrowExceptionWhenInternalCardNotFound() {
            assertThrows(CannotRedoException.class, () ->
                    creditCardSelector.getCreditCardByIdInternal(UUID.randomUUID())
            );
        }
    }

    @Nested
    @DisplayName("Cenários do getCreditCardByIdIncludingDeleted")
    class GetCreditCardByIdIncludingDeletedTests {

        @Test
        @DisplayName("Deve buscar cartão excluído do usuário logado")
        void shouldReturnDeletedCardForCurrentUser() {
            UUID userId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, userId, true)
            );

            CreditCard result = creditCardSelector.getCreditCardByIdIncludingDeleted(card.getId());

            assertNotNull(result);
            assertEquals(card.getId(), result.getId());
            assertTrue(result.isDeleted());
            verify(securityUtils, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound se o cartão pertencer a outro usuário mesmo que excluído")
        void shouldThrowExceptionWhenDeletedCardBelongsToAnotherUser() {
            UUID currentUser = UUID.randomUUID();
            UUID anotherUser = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser);

            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card = creditCardRepository.saveAndFlush(
                    CreditCardTestDataBuilder.createEntity(dto, anotherUser, true)
            );

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardSelector.getCreditCardByIdIncludingDeleted(card.getId())
            );

            verify(securityUtils, times(1)).getCurrentUserId();
        }
    }

    @Nested
    @DisplayName("Cenários do findByAccountId")
    class FindByAccountIdTests {

        @Test
        @DisplayName("Deve buscar todos os cartões associados ao ID da conta")
        void shouldReturnCardsAssociatedWithAccountId() {
            UUID accountId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            CreateCreditCardDTO dto1 = CreditCardTestDataBuilder.createCardDTO(accountId);
            CreditCard card1 = CreditCardTestDataBuilder.createEntity(dto1, userId, false);

            CreateCreditCardDTO dto2 = CreditCardTestDataBuilder.createCardDTO(accountId);
            CreditCard card2 = CreditCardTestDataBuilder.createEntity(dto2, userId, false);

            creditCardRepository.saveAllAndFlush(List.of(card1, card2));

            List<CreditCard> results = creditCardSelector.findByAccountId(accountId);

            assertNotNull(results);
            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(c -> c.getAccountId().equals(accountId)));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando nenhum cartão for encontrado para a conta")
        void shouldReturnEmptyListWhenNoCardsFoundForAccountId() {
            List<CreditCard> results = creditCardSelector.findByAccountId(UUID.randomUUID());

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }
}