package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.*;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping()
    public ResponseEntity<ResponseAccountDTO> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAccountDTO data
    ) {
        ResponseAccountDTO response = accountService.create(jwt, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseAccountDTO> view(@PathVariable UUID id) {
        ResponseAccountDTO response = accountService.view(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping()
    public ResponseEntity<PageResponseDTO<ResponseAccountDTO>> list(AccountFiltersDTO filters, Pageable pageable) {
        PageResponseDTO<ResponseAccountDTO> response = accountService.list(filters, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseAccountDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountDTO data
    ) {
        ResponseAccountDTO response = accountService.update(id, data);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<AccountSummaryDTO> summaryAccount(@PathVariable UUID id) {
        AccountSummaryDTO response = accountService.getInternalAccount(id);
        return ResponseEntity.ok().body(response);
    }
}
