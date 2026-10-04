package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.config.exceptions.APIException;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.exceptions.TransactionErrorCode;
import com.emmanuelfinance.transaction.services.scheduler.TransactionSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * O {@link TransactionValidatorService} expõe {@code create/update/restore} como campos finais inicializados
 * no construtor, então um mock dele deixaria esses campos nulos. Por isso usamos o validator real, alimentado
 * por mocks dos clients externos, e montamos o service manualmente.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionEventsService transactionEventsService;

    @Mock
    private TransactionSchedulerService transactionSchedulerService;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private CategoryClientCacheService categoryClientCacheService;

    @Mock
    private CreditCardClientCacheService creditCardClientCacheService;

    @Mock
    private AccountClientCacheService accountClientCacheService;

    private TransactionService transactionService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TransactionValidatorService validator = new TransactionValidatorService(
                accountOwnershipValidator, categoryClientCacheService, creditCardClientCacheService, accountClientCacheService
        );

        transactionService = new TransactionService(
                transactionRepository,
                securityUtils,
                transactionMapper,
                transactionSelector,
                idempotencyService,
                validator,
                transactionEventsService,
                transactionSchedulerService
        );
    }

    private void assertDomainError(TransactionErrorCode expected, org.junit.jupiter.api.function.Executable executable) {
        APIException exception = assertThrows(TransactionDomainException.class, executable);
        assertEquals(expected.getStatus(), exception.getStatus());
        assertEquals(expected.getMessage(), exception.getMessage());
    }

    /** Conta com saldo suficiente para os cenários que não testam saldo. */
    private void stubAccountBalance(UUID accountId) {
        when(accountClientCacheService.getInternalAccountById(accountId)).thenReturn(
                TransactionTestDataBuilder.accountSummaryInternalDTO(accountId, new BigDecimal("1000.00"))
        );
    }

    private void stubCategory(UUID categoryId, TypeEnum type) {
        when(categoryClientCacheService.getCategoryInternalSummaryDTO(categoryId))
                .thenReturn(TransactionTestDataBuilder.categoryInternalSummaryDTO(categoryId, type));
    }

    /** Faz o save devolver a própria entidade, gerando id quando ainda não houver. */
    private void stubSaveReturningArgument() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            return transaction;
        });
    }

    @Nested
    @DisplayName("Cenários do create")
    class CreateTests {

        private final UUID accountId = UUID.randomUUID();
        private final UUID categoryId = UUID.randomUUID();

        private void stubHappyPath(CreateTransactionDTO dto) {
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            stubCategory(categoryId, dto.type());
            stubAccountBalance(accountId);
            when(idempotencyService.generateIdempotencyKey(userId, dto)).thenReturn("chave-123");
            when(transactionMapper.toEntity(dto)).thenReturn(TransactionTestDataBuilder.entityFromDTO(dto));
            stubSaveReturningArgument();
            when(transactionMapper.toResponseDTO(any(Transaction.class)))
                    .thenAnswer(invocation -> TransactionTestDataBuilder.responseDTO(invocation.getArgument(0)));
        }

        @Test
        @DisplayName("Deve criar transação não agendada como PAID, sem agendar, e publicar o evento")
        void shouldCreatePaidTransaction() {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(accountId, categoryId);
            stubHappyPath(dto);

            ResponseTransactionDTO response = transactionService.create(dto);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(1)).save(captor.capture());
            Transaction saved = captor.getValue();

            assertEquals(userId, saved.getUserId());
            assertEquals("chave-123", saved.getIdempotencyKey());
            assertEquals(StatusTransactionEnum.PAID, saved.getStatus());
            assertEquals(saved.getId(), response.id());

            verify(idempotencyService, times(1)).validateAndLock("chave-123");
            verify(transactionEventsService, times(1)).publishTransactionCreatedEvent(saved);
            verifyNoInteractions(transactionSchedulerService);
        }

        @Test
        @DisplayName("Deve criar transação agendada como PENDING e agendar a execução")
        void shouldCreateScheduledTransaction() {
            LocalDateTime date = LocalDateTime.now().plusDays(2);
            CreateTransactionDTO dto = TransactionTestDataBuilder.scheduledDTO(accountId, categoryId, date);
            stubHappyPath(dto);

            transactionService.create(dto);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            Transaction saved = captor.getValue();

            assertEquals(StatusTransactionEnum.PENDING, saved.getStatus());
            verify(transactionSchedulerService, times(1)).schedule(saved.getId(), date);
            verify(transactionEventsService, times(1)).publishTransactionCreatedEvent(saved);
        }

        @Test
        @DisplayName("Não deve salvar nem publicar quando a validação falhar")
        void shouldNotSaveWhenValidationFails() {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(accountId, categoryId);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            doThrow(new AccountNotFound()).when(accountOwnershipValidator).validate(accountId);

            assertThrows(AccountNotFound.class, () -> transactionService.create(dto));

            verifyNoInteractions(idempotencyService, transactionRepository, transactionEventsService, transactionSchedulerService);
        }

        @Test
        @DisplayName("Não deve salvar quando a transação já tiver sido feita (idempotência)")
        void shouldNotSaveWhenTransactionAlreadyExists() {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(accountId, categoryId);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            stubCategory(categoryId, TypeEnum.EXPENSE);
            stubAccountBalance(accountId);
            when(idempotencyService.generateIdempotencyKey(userId, dto)).thenReturn("chave-123");
            doThrow(new TransactionDomainException(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS))
                    .when(idempotencyService).validateAndLock("chave-123");

            assertDomainError(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS, () -> transactionService.create(dto));

            verifyNoInteractions(transactionRepository, transactionEventsService, transactionSchedulerService);
        }
    }

    @Nested
    @DisplayName("Cenários do view")
    class ViewTests {

        @Test
        @DisplayName("Deve retornar o DTO da transação encontrada")
        void shouldReturnDTO() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            ResponseTransactionDTO dto = TransactionTestDataBuilder.responseDTO(transaction);
            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);
            when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

            assertEquals(dto, transactionService.view(transaction.getId()));
        }

        @Test
        @DisplayName("Deve propagar TransactionNotFound quando a transação não existir")
        void shouldPropagateNotFound() {
            UUID id = UUID.randomUUID();
            when(transactionSelector.getTransactionById(id)).thenThrow(new TransactionNotFound());

            assertThrows(TransactionNotFound.class, () -> transactionService.view(id));

            verifyNoInteractions(transactionMapper);
        }
    }

    @Nested
    @DisplayName("Cenários do list e listDeleted")
    class ListTests {

        private final Pageable pageable = PageRequest.of(0, 10);
        private final TransactionFiltersDTO filters =
                new TransactionFiltersDTO(null, null, null, null, null, null, null, null, null);

        @Test
        @DisplayName("Deve listar as transações mapeadas para DTO")
        void shouldListTransactions() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            ResponseTransactionDTO dto = TransactionTestDataBuilder.responseDTO(transaction);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
            when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

            PageResponseDTO<ResponseTransactionDTO> result = transactionService.list(filters, pageable);

            assertEquals(List.of(dto), result.content());
            assertEquals(1L, result.totalElements());
            assertEquals(1, result.totalPages());
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver transações")
        void shouldReturnEmptyPage() {
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

            PageResponseDTO<ResponseTransactionDTO> result = transactionService.list(filters, pageable);

            assertTrue(result.content().isEmpty());
            assertEquals(0L, result.totalElements());
            verifyNoInteractions(transactionMapper);
        }

        @Test
        @DisplayName("Deve listar as transações excluídas mapeadas para DTO")
        void shouldListDeletedTransactions() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId, true);
            ResponseTransactionDTO dto = TransactionTestDataBuilder.responseDTO(transaction);
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
            when(transactionMapper.toResponseDTO(transaction)).thenReturn(dto);

            PageResponseDTO<ResponseTransactionDTO> result = transactionService.listDeleted(filters, pageable);

            assertEquals(List.of(dto), result.content());
            assertTrue(result.content().get(0).deleted());
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver transações excluídas")
        void shouldReturnEmptyDeletedPage() {
            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));

            assertTrue(transactionService.listDeleted(filters, pageable).content().isEmpty());
        }
    }

    @Nested
    @DisplayName("Cenários do update")
    class UpdateTests {

        @Test
        @DisplayName("Deve lançar erro ao atualizar transação feita com cartão")
        void shouldThrowWhenTransactionHasCreditCard() {
            Transaction transaction = TransactionTestDataBuilder.cardTransactionEntity(
                    userId, UUID.randomUUID(), UUID.randomUUID()
            );
            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);

            assertDomainError(TransactionErrorCode.CARD_TRANSACTIONS_CANNOT_BE_CHANGED,
                    () -> transactionService.update(transaction.getId(), TransactionTestDataBuilder.updateDTO()));

            verifyNoInteractions(transactionRepository, transactionEventsService, transactionSchedulerService);
        }

        @Test
        @DisplayName("Deve publicar o evento de atualização com os valores antigos")
        void shouldPublishUpdatedEventWithOldValues() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            UUID oldAccountId = transaction.getAccountId();
            BigDecimal oldAmount = transaction.getAmount();
            UUID newAccountId = UUID.randomUUID();
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    newAccountId, null, null, new BigDecimal("200.00"), null, null, null
            );

            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);
            stubAccountBalance(newAccountId);
            doAnswer(invocation -> {
                transaction.setAccountId(newAccountId);
                transaction.setAmount(new BigDecimal("200.00"));
                return null;
            }).when(transactionMapper).updateEntityFromDTO(dto, transaction);
            when(transactionRepository.save(transaction)).thenReturn(transaction);

            transactionService.update(transaction.getId(), dto);

            verify(accountOwnershipValidator, times(1)).validate(newAccountId);
            verify(transactionEventsService, times(1)).publishTransactionUpdatedEvent(
                    transaction, oldAccountId, oldAmount, TypeEnum.EXPENSE, StatusTransactionEnum.PAID
            );
        }

        @Test
        @DisplayName("Ao desativar o agendamento deve limpar a data, marcar como PAID e cancelar o agendamento")
        void shouldCancelScheduleWhenSchedulingIsDisabled() {
            Transaction transaction = TransactionTestDataBuilder.pendingTransactionEntity(
                    userId, LocalDateTime.now().plusDays(1)
            );
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(null, null, null, null, false, null, null);

            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);
            doAnswer(invocation -> {
                transaction.setScheduled(false);
                return null;
            }).when(transactionMapper).updateEntityFromDTO(dto, transaction);
            when(transactionRepository.save(transaction)).thenReturn(transaction);

            transactionService.update(transaction.getId(), dto);

            assertNull(transaction.getDate());
            assertEquals(StatusTransactionEnum.PAID, transaction.getStatus());
            verify(transactionSchedulerService, times(1)).cancel(transaction.getId());
            verify(transactionSchedulerService, never()).schedule(any(), any());
            verify(transactionEventsService, times(1)).publishTransactionUpdatedEvent(
                    transaction, transaction.getAccountId(), transaction.getAmount(), TypeEnum.EXPENSE,
                    StatusTransactionEnum.PENDING
            );
        }

        @Test
        @DisplayName("Deve reagendar quando continuar agendada e PENDING")
        void shouldRescheduleWhenStillScheduledAndPending() {
            Transaction transaction = TransactionTestDataBuilder.pendingTransactionEntity(
                    userId, LocalDateTime.now().plusDays(1)
            );
            LocalDateTime newDate = LocalDateTime.now().plusDays(5);
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(null, null, null, null, true, newDate, null);

            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);
            doAnswer(invocation -> {
                transaction.setDate(newDate);
                return null;
            }).when(transactionMapper).updateEntityFromDTO(dto, transaction);
            when(transactionRepository.save(transaction)).thenReturn(transaction);

            transactionService.update(transaction.getId(), dto);

            verify(transactionSchedulerService, times(1)).schedule(transaction.getId(), newDate);
            verify(transactionSchedulerService, never()).cancel(any());
        }

        @Test
        @DisplayName("Não deve salvar quando a validação falhar")
        void shouldNotSaveWhenValidationFails() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            UpdateTransactionDTO dto = TransactionTestDataBuilder.updateDTO(
                    null, null, null, null, true, LocalDateTime.now().plusDays(1), null
            );
            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);

            assertDomainError(TransactionErrorCode.CANNOT_SCHEDULE_UNSCHEDULED_TRANSACTION,
                    () -> transactionService.update(transaction.getId(), dto));

            verifyNoInteractions(transactionMapper, transactionRepository, transactionEventsService);
        }
    }

    @Nested
    @DisplayName("Cenários do delete")
    class DeleteTests {

        @Test
        @DisplayName("Deve excluir a transação e publicar o evento de exclusão")
        void shouldDeleteAndPublishEvent() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId);
            when(transactionSelector.getTransactionById(transaction.getId())).thenReturn(transaction);

            transactionService.delete(transaction.getId());

            verify(transactionRepository, times(1)).delete(transaction);
            verify(transactionEventsService, times(1)).publishTransactionDeletedEvent(transaction);
        }

        @Test
        @DisplayName("Deve propagar TransactionNotFound e não excluir quando a transação não existir")
        void shouldPropagateNotFound() {
            UUID id = UUID.randomUUID();
            when(transactionSelector.getTransactionById(id)).thenThrow(new TransactionNotFound());

            assertThrows(TransactionNotFound.class, () -> transactionService.delete(id));

            verifyNoInteractions(transactionRepository, transactionEventsService);
        }
    }

    @Nested
    @DisplayName("Cenários do restore")
    class RestoreTests {

        @Test
        @DisplayName("Deve restaurar a transação excluída e publicar o evento de restauração")
        void shouldRestoreAndPublishEvent() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId, true);
            when(transactionSelector.getTransactionByIdIncluingDeleted(transaction.getId())).thenReturn(transaction);

            transactionService.restore(transaction.getId());

            assertFalse(transaction.isDeleted());
            verify(transactionRepository, times(1)).save(transaction);
            verify(transactionEventsService, times(1)).publishTransactionRestoreEvent(transaction);
        }

        @Test
        @DisplayName("Não deve restaurar uma transação que não está excluída")
        void shouldNotRestoreWhenNotDeleted() {
            Transaction transaction = TransactionTestDataBuilder.transactionEntity(userId, false);
            when(transactionSelector.getTransactionByIdIncluingDeleted(transaction.getId())).thenReturn(transaction);

            assertDomainError(TransactionErrorCode.RESTORE_TRANSACTION_NOT_DELETED,
                    () -> transactionService.restore(transaction.getId()));

            verifyNoInteractions(transactionRepository, transactionEventsService);
        }

        @Test
        @DisplayName("Não deve restaurar quando o limite do cartão for insuficiente")
        void shouldNotRestoreWhenCardLimitIsInsufficient() {
            UUID accountId = UUID.randomUUID();
            UUID cardId = UUID.randomUUID();
            Transaction transaction = TransactionTestDataBuilder.cardTransactionEntity(userId, accountId, cardId);
            transaction.setDeleted(true);
            when(transactionSelector.getTransactionByIdIncluingDeleted(transaction.getId())).thenReturn(transaction);
            when(creditCardClientCacheService.getCreditCardInternalSummaryDTO(cardId)).thenReturn(
                    TransactionTestDataBuilder.creditCardInternalSummaryDTO(cardId, accountId, new BigDecimal("10.00"))
            );

            assertDomainError(TransactionErrorCode.INSUFFICIENT_LIMIT_ON_THE_CARD,
                    () -> transactionService.restore(transaction.getId()));

            assertTrue(transaction.isDeleted());
            verifyNoInteractions(transactionRepository, transactionEventsService);
        }
    }
}
