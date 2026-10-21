package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardInternalServiceTest {

    @Mock
    private CreditCardSelector cardSelector;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CreditCardInternalService creditCardInternalService;

    // Regra: o serviço interno decide com o limite disponível, então lê do Postgres. O Mongo recebe os dados
    // pelo Outbox e pode estar atrasado. Se isto falhar, alguém ligou o serviço interno ao lado de leitura.
    @Test
    @DisplayName("Não deve depender de nenhuma classe do lado de leitura (Mongo)")
    void shouldNotDependOnTheMongoReadModel() {
        boolean dependsOnReadModel = Arrays.stream(CreditCardInternalService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().getPackageName().endsWith(".query"));

        assertFalse(dependsOnReadModel);
    }

    @Nested
    @DisplayName("Cenários do getCreditCardSummary")
    class GetCreditCardSummaryTests {

        @Test
        @DisplayName("Deve retornar o resumo do cartão com sucesso quando o id for encontrado")
        void shouldReturnCreditCardSummarySuccessfully() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            when(cardSelector.getCreditCardById(creditCard.getId())).thenReturn(creditCard);

            CreditCardSummaryDTO result = creditCardInternalService.getCreditCardSummary(creditCard.getId());

            assertNotNull(result);
            assertEquals(creditCard.getId(), result.id());
            assertEquals(creditCard.getName(), result.name());
            assertEquals(creditCard.isDeleted(), result.deleted());

            verify(cardSelector, times(1)).getCreditCardById(creditCard.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o cartão não for encontrado")
        void shouldThrowExceptionWhenCreditCardNotFoundForSummary() {
            UUID notFoundId = UUID.randomUUID();

            when(cardSelector.getCreditCardById(notFoundId))
                    .thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardInternalService.getCreditCardSummary(notFoundId)
            );

            verify(cardSelector, times(1)).getCreditCardById(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do getCreditCardInternalSummary")
    class GetCreditCardInternalSummaryTests {

        @Test
        @DisplayName("Deve retornar o resumo interno do cartão com sucesso quando o id for encontrado")
        void shouldReturnCreditCardInternalSummarySuccessfully() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            when(cardSelector.getCreditCardById(creditCard.getId())).thenReturn(creditCard);

            CreditCardInternalSummaryDTO result = creditCardInternalService.getCreditCardInternalSummary(creditCard.getId());

            assertNotNull(result);
            assertEquals(creditCard.getId(), result.id());
            assertEquals(creditCard.getAccountId(), result.accountId());
            assertEquals(creditCard.getAvailableLimit(), result.availableLimit());
            assertEquals(creditCard.getDueDay(), result.dueDate());
            assertEquals(creditCard.getClosingDay(), result.closingDate());

            verify(cardSelector, times(1)).getCreditCardById(creditCard.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o cartão não for encontrado no resumo interno")
        void shouldThrowExceptionWhenCreditCardNotFoundForInternalSummary() {
            UUID notFoundId = UUID.randomUUID();

            when(cardSelector.getCreditCardById(notFoundId))
                    .thenThrow(new CreditCardNotFound());

            assertThrows(CreditCardNotFound.class, () ->
                    creditCardInternalService.getCreditCardInternalSummary(notFoundId)
            );

            verify(cardSelector, times(1)).getCreditCardById(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do deactivateCardsByAccountId")
    class DeactivateCardsByAccountIdTests {

        @Test
        @DisplayName("Deve desativar todos os cartões vinculados à conta e registrar cada exclusão no Outbox")
        void shouldDeactivateCardsByAccountIdSuccessfully() {
            UUID accountId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard card1 = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);
            CreditCard card2 = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            when(cardSelector.findByAccountId(accountId)).thenReturn(List.of(card1, card2));
            doNothing().when(creditCardRepository).delete(any(CreditCard.class));

            assertDoesNotThrow(() -> creditCardInternalService.deactivateCardsByAccountId(accountId));

            verify(cardSelector, times(1)).findByAccountId(accountId);
            verify(creditCardRepository, times(1)).delete(card1);
            verify(creditCardRepository, times(1)).delete(card2);
            verify(outboxService, times(1)).creditCardDeleted(card1);
            verify(outboxService, times(1)).creditCardDeleted(card2);
        }

        @Test
        @DisplayName("Deve executar sem erros quando não houver cartões vinculados à conta")
        void shouldExecuteSuccessfullyWhenNoCardsFoundForAccount() {
            UUID accountId = UUID.randomUUID();

            when(cardSelector.findByAccountId(accountId)).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> creditCardInternalService.deactivateCardsByAccountId(accountId));

            verify(cardSelector, times(1)).findByAccountId(accountId);
            verify(creditCardRepository, never()).delete(any(CreditCard.class));
            verifyNoInteractions(outboxService);
        }
    }
}
