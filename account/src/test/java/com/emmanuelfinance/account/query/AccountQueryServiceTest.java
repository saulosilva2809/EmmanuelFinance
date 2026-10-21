package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTest {

    @Mock
    private AccountDocumentSelector accountDocumentSelector;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Spy
    private AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    @InjectMocks
    private AccountQueryService accountQueryService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldReturnTheAccountWhenItExists() {
        AccountDocument document = newDocument();
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId())).thenReturn(document);

        ResponseAccountDTO result = accountQueryService.view(document.getId());

        assertEquals(document.getId(), result.id());
        assertEquals("Conta C6", result.name());
        assertEquals(new BigDecimal("1000.00"), result.currentBalance());
    }

    @Test
    void shouldPropagateAccountNotFoundWhenAccountDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(id))
                .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        AccountDomainException exception = assertThrows(AccountDomainException.class,
                () -> accountQueryService.view(id));

        assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldReturnPaginatedAccounts() {
        Pageable pageable = PageRequest.of(0, 10);
        AccountDocument document = newDocument();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(AccountDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(AccountDocument.class))).thenReturn(List.of(document));

        PageResponseDTO<ResponseAccountDTO> result =
                accountQueryService.list(new AccountFiltersDTO("c6", TypeEnum.CHECKING), pageable);

        assertEquals(1, result.content().size());
        assertEquals(document.getId(), result.content().get(0).id());
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldReturnEmptyPageWhenNoAccountsFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(AccountDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(AccountDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseAccountDTO> result =
                accountQueryService.listDeleted(new AccountFiltersDTO(null, null), pageable);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    private AccountDocument newDocument() {
        AccountDocument document = new AccountDocument();
        document.setId(UUID.randomUUID());
        document.setUserId(userId);
        document.setName("Conta C6");
        document.setType(TypeEnum.CHECKING);
        document.setBank(BanksEnum.C6_BANK);
        document.setInitialBalance(new BigDecimal("1000.00"));
        document.setCurrentBalance(new BigDecimal("1000.00"));
        return document;
    }
}
