package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.AccountService;
import com.emmanuelfinance.account.dto.*;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping()
    public ResponseEntity<ResponseAccountDTO> create(
            @Valid @RequestBody CreateAccountDTO data
    ) {
        ResponseAccountDTO response = accountService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseAccountDTO> view(@PathVariable UUID id) {
        ResponseAccountDTO response = accountService.view(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping()
    public ResponseEntity<PageResponseDTO<ResponseAccountDTO>> list(
            AccountFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseAccountDTO> response = accountService.list(filters, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponseDTO<ResponseAccountDTO>> listDeleted(
            AccountFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseAccountDTO> response = accountService.listDeleted(filters, pageable);
        return ResponseEntity.ok().body(response);
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

    @PostMapping("/restore/{id}")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        accountService.restore(id);
        return ResponseEntity.noContent().build();
    }
}
