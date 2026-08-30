package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.services.AccountInternalService;
import com.emmanuelfinance.account.services.AccountService;
import com.emmanuelfinance.account.services.AccountValidatorService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestParam UUID userId
    ) {
        boolean isOwner = accountValidatorService.checkAccountOwner(accountId, userId);
        return ResponseEntity.ok(isOwner);
    }
}