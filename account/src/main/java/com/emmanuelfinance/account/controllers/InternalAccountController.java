package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.services.AccountInternalService;
import com.emmanuelfinance.account.services.AccountService;
import com.emmanuelfinance.account.services.AccountValidatorService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountInternalService accountInternalService;
    private final AccountValidatorService accountValidatorService;

    @GetMapping("summary/{id}")
    public ResponseEntity<AccountSummaryDTO> summaryAccount(@PathVariable UUID id) {
        AccountSummaryDTO response = accountInternalService.getAccountSummary(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("internal-summary/{id}")
    public ResponseEntity<AccountSummaryInternalDTO> summaryInternalAccount(@PathVariable UUID id) {
        AccountSummaryInternalDTO response = accountInternalService.getAccountSummaryInternal(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{accountId}/ownership")
    public ResponseEntity<Boolean> checkOwnership(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boolean isOwner = accountValidatorService.checkAccountOwner(accountId, jwt);
        return ResponseEntity.ok(isOwner);
    }
}