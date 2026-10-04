package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.shared.enums.BanksEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardBalanceServiceTest {

    @InjectMocks
    private CreditCardBalanceService creditCardBalanceService;

    private UpdateCreditCardDTO createUpdateCardDTO(BigDecimal newLimit) {
        return new UpdateCreditCardDTO(
                UUID.randomUUID(),
                "Cartão de Crédito C6",
                BanksEnum.C6_BANK,
                newLimit,
                17,
                24
        );
    }

    @Nested
    @DisplayName("Cenários do updateAvailableLimit")
    class UpdateAvailableLimitTests {

        @Test
        @DisplayName("Deve aumentar o limite disponível com sucesso quando o limite total for aumentado")
        void shouldIncreaseAvailableLimitSuccessfullyWhenCreditLimitIncreases() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO(); // Limit = 10000
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);
            creditCard.setAvailableLimit(new BigDecimal("8000.00"));

            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(new BigDecimal("12000.00")); // +2000 diff

            assertDoesNotThrow(() -> creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO));

            assertEquals(new BigDecimal("10000.00"), creditCard.getAvailableLimit());
        }

        @Test
        @DisplayName("Deve diminuir o limite disponível com sucesso quando o limite total for reduzido sem ficar negativo")
        void shouldDecreaseAvailableLimitSuccessfullyWhenCreditLimitDecreases() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO(); // Limit = 10000
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);
            creditCard.setAvailableLimit(new BigDecimal("8000.00"));

            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(new BigDecimal("9000.00")); // -1000 diff

            assertDoesNotThrow(() -> creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO));

            assertEquals(new BigDecimal("7000.00"), creditCard.getAvailableLimit());
        }

        @Test
        @DisplayName("Deve manter o limite disponível inalterado quando o novo limite total for igual ao limite antigo")
        void shouldKeepAvailableLimitUnchangedWhenNewLimitEqualsOldLimit() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO(); // Limit = 10000
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);
            creditCard.setAvailableLimit(new BigDecimal("8000.00"));

            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(new BigDecimal("10000.00")); // 0 diff

            assertDoesNotThrow(() -> creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO));

            assertEquals(new BigDecimal("8000.00"), creditCard.getAvailableLimit());
        }

        @Test
        @DisplayName("Deve permitir definir o novo limite como zero quando a diferença não for negativa")
        void shouldAllowZeroLimitWhenDifferenceIsNotNegative() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            // Limite antigo de 0
            CreateCreditCardDTO zeroDTO = new CreateCreditCardDTO(
                    createDTO.accountId(),
                    createDTO.name(),
                    createDTO.bank(),
                    BigDecimal.ZERO,
                    createDTO.closingDay(),
                    createDTO.dueDay()
            );
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(zeroDTO, userId, false);
            creditCard.setAvailableLimit(BigDecimal.ZERO);

            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(BigDecimal.ZERO); // 0 diff

            assertDoesNotThrow(() -> creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO));

            assertEquals(BigDecimal.ZERO, creditCard.getAvailableLimit());
        }

        @Test
        @DisplayName("Deve lançar exceção quando o novo limite total for negativo")
        void shouldThrowExceptionWhenNewLimitIsNegative() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO(); // Limit = 10000
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);
            creditCard.setAvailableLimit(new BigDecimal("8000.00"));

            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(new BigDecimal("-100.00"));

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO)
            );

            // Garante que o limite disponível não foi alterado após a falha
            assertEquals(new BigDecimal("8000.00"), creditCard.getAvailableLimit());
        }

        @Test
        @DisplayName("Deve lançar exceção quando a redução do limite fizer o saldo disponível ficar negativo")
        void shouldThrowExceptionWhenDecreaseMakesAvailableLimitNegative() {
            UUID userId = UUID.randomUUID();
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO(); // Limit = 10000
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            // O cliente usou R$ 8.000 do limite, restando apenas R$ 2.000 disponíveis
            creditCard.setAvailableLimit(new BigDecimal("2000.00"));

            // Tenta reduzir o limite total para R$ 5.000 (diferença de -5000)
            // Cálculo: 2000 + (-5000) = -3000 (O disponível ficaria negativo!)
            UpdateCreditCardDTO updateDTO = createUpdateCardDTO(new BigDecimal("5000.00"));

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardBalanceService.updateAvailableLimit(creditCard, updateDTO)
            );

            // Garante que o estado original da entidade permaneceu intacto
            assertEquals(new BigDecimal("2000.00"), creditCard.getAvailableLimit());
        }
    }
}