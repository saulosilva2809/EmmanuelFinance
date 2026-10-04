package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditCardValidatorServiceTest {

    @Mock
    private AccountClientCacheService accountClientCacheService;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @InjectMocks
    private CreditCardValidatorService creditCardValidatorService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Cenários do validateAccountOwnership")
    class ValidateAccountOwnershipTests {

        @Test
        @DisplayName("Deve validar a posse da conta com sucesso")
        void shouldValidateAccountOwnershipSuccessfully() {
            UUID accountId = UUID.randomUUID();

            doNothing().when(accountOwnershipValidator).validate(accountId);

            assertDoesNotThrow(() -> creditCardValidatorService.validateAccountOwnership(accountId));

            verify(accountOwnershipValidator, times(1)).validate(accountId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando a conta não pertencer ao usuário")
        void shouldThrowExceptionWhenAccountOwnershipIsInvalid() {
            UUID accountId = UUID.randomUUID();

            doThrow(new RuntimeException("Account does not belong to user"))
                    .when(accountOwnershipValidator).validate(accountId);

            assertThrows(RuntimeException.class, () ->
                    creditCardValidatorService.validateAccountOwnership(accountId)
            );

            verify(accountOwnershipValidator, times(1)).validate(accountId);
        }
    }

    @Nested
    @DisplayName("Cenários do validateCardAndAccountBank")
    class ValidateCardAndAccountBankTests {

        @Test
        @DisplayName("Deve validar com sucesso quando os bancos do cartão e da conta forem iguais")
        void shouldValidateSuccessfullyWhenBanksAreEqual() {
            UUID accountId = UUID.randomUUID();
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(accountId);

            when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(accountSummary);

            assertDoesNotThrow(() ->
                    creditCardValidatorService.validateCardAndAccountBank(BanksEnum.C6_BANK, accountId)
            );

            verify(accountClientCacheService, times(1)).getInternalAccountById(accountId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando o banco do cartão for diferente do banco da conta")
        void shouldThrowExceptionWhenBanksAreDifferent() {
            UUID accountId = UUID.randomUUID();
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(accountId);

            when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(accountSummary);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateCardAndAccountBank(BanksEnum.NUBANK, accountId)
            );

            verify(accountClientCacheService, times(1)).getInternalAccountById(accountId);
        }
    }

    @Nested
    @DisplayName("Cenários do validateIsCardDeleted")
    class ValidateIsCardDeletedTests {

        @Test
        @DisplayName("Deve validar com sucesso quando o cartão estiver marcado como excluído")
        void shouldValidateSuccessfullyWhenCardIsDeleted() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, true);

            assertDoesNotThrow(() -> creditCardValidatorService.validateIsCardDeleted(creditCard));
        }

        @Test
        @DisplayName("Deve lançar exceção quando o cartão não estiver excluído")
        void shouldThrowExceptionWhenCardIsNotDeleted() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateIsCardDeleted(creditCard)
            );
        }
    }

    @Nested
    @DisplayName("Cenários do validateAccountNotDeleted")
    class ValidateAccountNotDeletedTests {

        @Test
        @DisplayName("Deve validar com sucesso quando a conta vinculada não estiver excluída")
        void shouldValidateSuccessfullyWhenAccountIsNotDeleted() {
            UUID accountId = UUID.randomUUID();
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(accountId);

            when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(accountSummary);

            assertDoesNotThrow(() -> creditCardValidatorService.validateAccountNotDeleted(accountId));

            verify(accountClientCacheService, times(1)).getInternalAccountById(accountId);
        }

        @Test
        @DisplayName("Deve lançar exceção quando a conta vinculada estiver excluída")
        void shouldThrowExceptionWhenAccountIsDeleted() {
            UUID accountId = UUID.randomUUID();
            AccountSummaryInternalDTO accountSummary = new AccountSummaryInternalDTO(
                    accountId,
                    "Conta Corrente",
                    BanksEnum.C6_BANK,
                    new BigDecimal("5000.00"),
                    true
            );

            when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(accountSummary);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateAccountNotDeleted(accountId)
            );

            verify(accountClientCacheService, times(1)).getInternalAccountById(accountId);
        }
    }

    @Nested
    @DisplayName("Cenários do validateCreation")
    class ValidateCreationTests {

        @Test
        @DisplayName("Deve validar a criação com sucesso")
        void shouldValidateCreationSuccessfully() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(createDTO.accountId());

            doNothing().when(accountOwnershipValidator).validate(createDTO.accountId());
            when(accountClientCacheService.getInternalAccountById(createDTO.accountId())).thenReturn(accountSummary);

            assertDoesNotThrow(() -> creditCardValidatorService.validateCreation(createDTO));

            verify(accountOwnershipValidator, times(1)).validate(createDTO.accountId());
            verify(accountClientCacheService, times(1)).getInternalAccountById(createDTO.accountId());
        }

        @Test
        @DisplayName("Deve lançar exceção na criação quando os bancos do cartão e da conta forem diferentes")
        void shouldThrowExceptionInCreationWhenBanksAreDifferent() {
            CreateCreditCardDTO createDTO = new CreateCreditCardDTO(
                    UUID.randomUUID(),
                    "Cartão de Crédito Nubank",
                    BanksEnum.NUBANK,
                    CreditCardTestDataBuilder.createCardDTO().creditLimit(),
                    10,
                    20
            );
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(createDTO.accountId());

            doNothing().when(accountOwnershipValidator).validate(createDTO.accountId());
            when(accountClientCacheService.getInternalAccountById(createDTO.accountId())).thenReturn(accountSummary);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateCreation(createDTO)
            );

            verify(accountOwnershipValidator, times(1)).validate(createDTO.accountId());
            verify(accountClientCacheService, times(1)).getInternalAccountById(createDTO.accountId());
        }
    }

    @Nested
    @DisplayName("Cenários do validateRestoration")
    class ValidateRestorationTests {

        @Test
        @DisplayName("Deve validar a restauração com sucesso quando o cartão e a conta cumprirem as regras")
        void shouldValidateRestorationSuccessfully() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, true);
            AccountSummaryInternalDTO accountSummary = CreditCardTestDataBuilder.accountSummaryInternalDTO(creditCard.getAccountId());

            when(accountClientCacheService.getInternalAccountById(creditCard.getAccountId())).thenReturn(accountSummary);

            assertDoesNotThrow(() -> creditCardValidatorService.validateRestoration(creditCard));

            verify(accountClientCacheService, times(1)).getInternalAccountById(creditCard.getAccountId());
        }

        @Test
        @DisplayName("Deve lançar exceção na restauração se o cartão não estiver excluído")
        void shouldThrowExceptionInRestorationWhenCardIsNotDeleted() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, false);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateRestoration(creditCard)
            );

            verify(accountClientCacheService, never()).getInternalAccountById(any());
        }

        @Test
        @DisplayName("Deve lançar exceção na restauração se a conta associada estiver excluída")
        void shouldThrowExceptionInRestorationWhenAccountIsDeleted() {
            CreateCreditCardDTO createDTO = CreditCardTestDataBuilder.createCardDTO();
            CreditCard creditCard = CreditCardTestDataBuilder.createEntity(createDTO, userId, true);
            AccountSummaryInternalDTO deletedAccountSummary = new AccountSummaryInternalDTO(
                    creditCard.getAccountId(),
                    "Conta Corrente Excluída",
                    BanksEnum.C6_BANK,
                    new BigDecimal("5000.00"),
                    true
            );

            when(accountClientCacheService.getInternalAccountById(creditCard.getAccountId())).thenReturn(deletedAccountSummary);

            assertThrows(CreditCardDomainException.class, () ->
                    creditCardValidatorService.validateRestoration(creditCard)
            );

            verify(accountClientCacheService, times(1)).getInternalAccountById(creditCard.getAccountId());
        }
    }
}