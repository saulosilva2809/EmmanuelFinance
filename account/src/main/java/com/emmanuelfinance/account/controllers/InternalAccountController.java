package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.AccountService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @GetMapping("summary/{id}")
    public ResponseEntity<AccountSummaryDTO> summaryAccount(@PathVariable UUID id) {
        AccountSummaryDTO response = accountService.getAccountSummary(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{accountId}/ownership")
    public ResponseEntity<Boolean> checkOwnership(
            @PathVariable UUID accountId,
            @RequestParam UUID userId
    ) {
        boolean isOwner = accountService.checkAccountOwner(accountId, userId);
        return ResponseEntity.ok(isOwner);
    }
}