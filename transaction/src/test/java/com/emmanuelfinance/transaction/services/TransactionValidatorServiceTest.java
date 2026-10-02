package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.config.exceptions.APIException;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.exceptions.TransactionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionValidatorServiceTest {

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private CategoryClientCacheService categoryClientCacheService;

    @Mock
    private CreditCardClientCacheService creditCardClientCacheService;

    @InjectMocks
    private TransactionValidatorService validator;

    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID creditCardId = UUID.randomUUID();

    private void assertDomainError(TransactionErrorCode expected, Executable executable) {
        APIException exception = assertThrows(TransactionDomainException.class, executable);
        assertEquals(expected.getStatus(), exception.getStatus());
        assertEquals(expected.getMessage(), exception.getMessage());
    }

    private void stubCategory(TypeEnum type) {
        when(categoryClientCacheService.getCategoryInternalSummaryDTO(categoryId))
                .thenReturn(TransactionTestDataBuilder.categoryInternalSummaryDTO(categoryId, type));
    }

    private void stubCard(UUID cardAccountId, String availableLimit) {
        when(creditCardClientCacheService.getCreditCardInternalSummaryDTO(creditCardId))
                .thenReturn(TransactionTestDataBuilder.creditCardInternalSummaryDTO(
                        creditCardId, cardAccountId, new BigDecimal(availableLimit)
                ));
    }

    @Nested
    @DisplayName("Cenários do create.validate")
    class CreateTests {

        @Test
        @DisplayName("Deve passar para despesa de conta válida")
        void shouldPassForValidAccountExpense() {
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(accountId, categoryId);

            assertDoesNotThrow(() -> validator.create.validate(dto));

            verify(accountOwnershipValidator, times(1)).validate(accountId);
        }

        @Test
        @DisplayName("Deve passar para compra no cartão válida")
        void shouldPassForValidCardPurchase() {
            stubCard(accountId, "1000.00");
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.cardDTO(accountId, categoryId, creditCardId, 3);

            assertDoesNotThrow(() -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro ao parcelar uma transação sem cartão")
        void shouldThrowWhenInstallmentsWithoutCard() {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(
                    accountId, categoryId, null, BigDecimal.TEN, 3, false, null, TypeEnum.EXPENSE
            );

            assertDomainError(TransactionErrorCode.INSTALLMENTS_IN_TRANSACTION_ACCOUNT, () -> validator.create.validate(dto));
            verifyNoInteractions(accountOwnershipValidator);
        }

        @Test
        @DisplayName("Deve lançar erro quando a compra no cartão não for despesa")
        void shouldThrowWhenCardTransactionIsIncome() {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(
                    accountId, categoryId, creditCardId, BigDecimal.TEN, 1, false, null, TypeEnum.INCOME
            );

            assertDomainError(TransactionErrorCode.CARD_TRANSACTION_TYPE, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando o valor ultrapassar o limite disponível do cartão")
        void shouldThrowWhenAmountExceedsCardLimit() {
            stubCard(accountId, "50.00");
            CreateTransactionDTO dto = TransactionTestDataBuilder.cardDTO(accountId, categoryId, creditCardId, 1);

            assertDomainError(TransactionErrorCode.INSUFFICIENT_LIMIT_ON_THE_CARD, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve aceitar valor igual ao limite disponível do cartão")
        void shouldAcceptAmountEqualToCardLimit() {
            stubCard(accountId, "100.00");
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.cardDTO(accountId, categoryId, creditCardId, 1);

            assertDoesNotThrow(() -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando o número de parcelas for menor que 1")
        void shouldThrowWhenInstallmentsLessThanOne() {
            stubCard(accountId, "1000.00");
            CreateTransactionDTO dto = TransactionTestDataBuilder.cardDTO(accountId, categoryId, creditCardId, 0);

            assertDomainError(TransactionErrorCode.NUMBER_OF_INSTALLMENTS, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar CreditCardNotFound quando o cartão pertencer a outra conta")
        void shouldThrowWhenCardBelongsToAnotherAccount() {
            stubCard(UUID.randomUUID(), "1000.00");
            CreateTransactionDTO dto = TransactionTestDataBuilder.cardDTO(accountId, categoryId, creditCardId, 1);

            assertThrows(CreditCardNotFound.class, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando o tipo da categoria for incompatível com o da transação")
        void shouldThrowWhenCategoryTypeIsIncompatible() {
            stubCategory(TypeEnum.INCOME);
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(accountId, categoryId);

            assertDomainError(TransactionErrorCode.INCOMPATIBLE_CATEGORY_TYPE, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando a transação agendada não tiver data")
        void shouldThrowWhenScheduledWithoutDate() {
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(
                    accountId, categoryId, null, BigDecimal.TEN, 1, true, null, TypeEnum.EXPENSE
            );

            assertDomainError(TransactionErrorCode.SCHEDULED_TRANSACTION_DATE_REQUIRED, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando a data agendada estiver no passado")
        void shouldThrowWhenScheduledInThePast() {
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.scheduledDTO(
                    accountId, categoryId, LocalDateTime.now().minusDays(1)
            );

            assertDomainError(TransactionErrorCode.TRANSACTION_SCHEDULED_IN_THE_PAST, () -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve aceitar transação agendada para o futuro")
        void shouldAcceptScheduledInTheFuture() {
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.scheduledDTO(
                    accountId, categoryId, LocalDateTime.now().plusDays(1)
            );

            assertDoesNotThrow(() -> validator.create.validate(dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando a transação não agendada tiver data")
        void shouldThrowWhenUnscheduledHasDate() {
            stubCategory(TypeEnum.EXPENSE);
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(
                    accountId, categoryId, null, BigDecimal.TEN, 1, false, LocalDateTime.now().plusDays(1), TypeEnum.EXPENSE
            );

            assertDomainError(TransactionErrorCode.UNSCHEDULED_TRANSACTION_DATE_NOT_ALLOWED, () -> validator.create.validate(dto));
        }
    }

    @Nested
    @DisplayName("Cenários do update.validate")
    class UpdateTests {

        private Transaction existing(boolean scheduled) {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            transaction.setAccountId(accountId);
            transaction.setScheduled(scheduled);
            transaction.setType(TypeEnum.EXPENSE);
            return transaction;
        }

        @Test
        @DisplayName("Deve passar quando nenhum campo for informado")
        void shouldPassWhenNothingIsInformed() {
            assertDoesNotThrow(() -> validator.update.validate(existing(false), TransactionTestDataBuilder.emptyUpdateDTO()));

            verifyNoInteractions(accountOwnershipValidator, categoryClientCacheService);
        }

        @Test
        @DisplayName("Deve validar a posse da conta quando a conta for alterada")
        void shouldValidateAccountWhenChanged() {
            UUID newAccountId = UUID.randomUUID();
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(newAccountId, null, null, null, null, null, null);

            validator.update.validate(existing(false), dto);

            verify(accountOwnershipValidator, times(1)).validate(newAccountId);
        }

        @Test
        @DisplayName("Não deve validar a posse da conta quando for a mesma conta")
        void shouldNotValidateAccountWhenUnchanged() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(accountId, null, null, null, null, null, null);

            validator.update.validate(existing(false), dto);

            verifyNoInteractions(accountOwnershipValidator);
        }

        @Test
        @DisplayName("Deve validar a categoria com o tipo atual da transação")
        void shouldValidateCategoryAgainstExistingType() {
            stubCategory(TypeEnum.INCOME);
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(null, categoryId, null, null, null, null, null);

            assertDomainError(TransactionErrorCode.INCOMPATIBLE_CATEGORY_TYPE,
                    () -> validator.update.validate(existing(false), dto));
        }

        @Test
        @DisplayName("Deve validar a categoria com o novo tipo quando o tipo for alterado")
        void shouldValidateCategoryAgainstNewType() {
            stubCategory(TypeEnum.INCOME);
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    null, categoryId, null, null, null, null, TypeEnum.INCOME
            );

            assertDoesNotThrow(() -> validator.update.validate(existing(false), dto));
        }

        @Test
        @DisplayName("Deve lançar erro ao agendar uma transação que não era agendada")
        void shouldThrowWhenSchedulingUnscheduledTransaction() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    null, null, null, null, true, LocalDateTime.now().plusDays(1), null
            );

            assertDomainError(TransactionErrorCode.CANNOT_SCHEDULE_UNSCHEDULED_TRANSACTION,
                    () -> validator.update.validate(existing(false), dto));
        }

        @Test
        @DisplayName("Deve lançar erro ao desativar o agendamento informando data")
        void shouldThrowWhenUnschedulingWithDate() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    null, null, null, null, false, LocalDateTime.now().plusDays(1), null
            );

            assertDomainError(TransactionErrorCode.UNSCHEDULED_TRANSACTION_DATE_NOT_ALLOWED,
                    () -> validator.update.validate(existing(true), dto));
        }

        @Test
        @DisplayName("Deve permitir desativar o agendamento sem data")
        void shouldAllowUnschedulingWithoutDate() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(null, null, null, null, false, null, null);

            assertDoesNotThrow(() -> validator.update.validate(existing(true), dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando a transação continuar agendada sem data")
        void shouldThrowWhenScheduledWithoutDate() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(null, null, null, null, true, null, null);

            assertDomainError(TransactionErrorCode.SCHEDULED_TRANSACTION_DATE_REQUIRED,
                    () -> validator.update.validate(existing(true), dto));
        }

        @Test
        @DisplayName("Deve lançar erro quando a nova data agendada estiver no passado")
        void shouldThrowWhenNewDateIsInThePast() {
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    null, null, null, null, true, LocalDateTime.now().minusDays(1), null
            );

            assertDomainError(TransactionErrorCode.TRANSACTION_SCHEDULED_IN_THE_PAST,
                    () -> validator.update.validate(existing(true), dto));
        }
    }

    @Nested
    @DisplayName("Cenários do restore.validate")
    class RestoreTests {

        @Test
        @DisplayName("Deve lançar erro quando a transação não estiver excluída")
        void shouldThrowWhenNotDeleted() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId, false);

            assertDomainError(TransactionErrorCode.RESTORE_TRANSACTION_NOT_DELETED,
                    () -> validator.restore.validate(transaction));
        }

        @Test
        @DisplayName("Deve passar para transação de conta excluída sem consultar o cartão")
        void shouldPassForDeletedAccountTransaction() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId, true);

            assertDoesNotThrow(() -> validator.restore.validate(transaction));

            verifyNoInteractions(creditCardClientCacheService);
        }

        @Test
        @DisplayName("Deve lançar erro quando o limite do cartão for insuficiente para restaurar")
        void shouldThrowWhenCardLimitIsInsufficient() {
            stubCard(accountId, "10.00");
            Transaction transaction = TransactionTestDataBuilder.cardTransactionEntity(userId, accountId, creditCardId);
            transaction.setDeleted(true);

            assertDomainError(TransactionErrorCode.INSUFFICIENT_LIMIT_ON_THE_CARD,
                    () -> validator.restore.validate(transaction));
        }

        @Test
        @DisplayName("Deve passar quando o limite do cartão for suficiente")
        void shouldPassWhenCardLimitIsEnough() {
            stubCard(accountId, "1000.00");
            Transaction transaction = TransactionTestDataBuilder.cardTransactionEntity(userId, accountId, creditCardId);
            transaction.setDeleted(true);
            transaction.setStatus(StatusTransactionEnum.PAID);

            assertDoesNotThrow(() -> validator.restore.validate(transaction));
        }
    }
}
