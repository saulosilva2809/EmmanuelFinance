package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountTestDataBuilder;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountValidatorServiceTest {

    @InjectMocks
    private AccountValidatorService accountValidatorService;

    @Mock
    private AccountRepository accountRepository;

    private Account account;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
        account = AccountTestDataBuilder.accountEntity(accountDTO, userId, false);
    }

    @Test
    @DisplayName("Deve retornar falso quando o usuário não for o dono da conta")
    void shouldReturnFalseWhenTheUserIsNotTheAccountOwner() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", UUID.randomUUID().toString())
                .build();

        when(accountRepository.existsByIdAndUserIdAndDeletedFalse(any(), any())).thenReturn(
                false
        );

        boolean response = accountValidatorService
                .checkAccountOwner(account.getId(), jwt);

        assertEquals(false, response);
    }

    @Test
    @DisplayName("Deve retornar falso quando a conta não existir")
    void shouldReturnFalseWhenTheAccountDoesNotExist() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", UUID.randomUUID().toString())
                .build();

        when(accountRepository.existsByIdAndUserIdAndDeletedFalse(any(), any())).thenReturn(
                false
        );

        boolean response = accountValidatorService
                .checkAccountOwner(UUID.randomUUID(), jwt);

        assertEquals(false, response);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o usuário for o dono da conta")
    void shouldReturnTrueWhenTheUserIsTheAccountOwner() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", UUID.randomUUID().toString())
                .build();

        when(accountRepository.existsByIdAndUserIdAndDeletedFalse(any(), any())).thenReturn(
                true
        );

        boolean response = accountValidatorService
                .checkAccountOwner(account.getId(), jwt);

        assertEquals(true, response);
    }
}
