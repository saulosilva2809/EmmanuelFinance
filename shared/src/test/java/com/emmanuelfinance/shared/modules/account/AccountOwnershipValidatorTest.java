package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountOwnershipValidatorTest {

    @Mock
    private AccountClientCacheService accountClientCacheService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AccountOwnershipValidator validator;

    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldAcceptTheAccountOfTheCurrentUser() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(accountClientCacheService.isAccountOwnedByUser(accountId, userId)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(accountId));
    }

    @Test
    void shouldAnswerNotFoundWhenTheAccountBelongsToSomeoneElse() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(accountClientCacheService.isAccountOwnedByUser(accountId, userId)).thenReturn(false);

        assertThrows(AccountNotFound.class, () -> validator.validate(accountId));
    }
}
