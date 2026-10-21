package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDocumentSelectorTest {

    @Mock
    private AccountDocumentRepository accountDocumentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AccountDocumentSelector accountDocumentSelector;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void shouldReturnTheAccountOfTheCurrentUser() {
        AccountDocument document = new AccountDocument();
        document.setId(UUID.randomUUID());
        when(accountDocumentRepository.findByIdAndUserIdAndDeletedFalse(document.getId(), userId))
                .thenReturn(Optional.of(document));

        assertEquals(document, accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId()));
    }

    @Test
    void shouldThrowAccountNotFoundWhenTheAccountDoesNotExistForTheUser() {
        UUID id = UUID.randomUUID();
        when(accountDocumentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        AccountDomainException exception = assertThrows(AccountDomainException.class,
                () -> accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(id));

        assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }
}
