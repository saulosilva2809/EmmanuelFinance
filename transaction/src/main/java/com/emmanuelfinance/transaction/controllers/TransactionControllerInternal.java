package com.emmanuelfinance.transaction.controllers;

import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.transaction.services.TransactionServiceInternal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/transactions")
@RequiredArgsConstructor
public class TransactionControllerInternal {

    private final TransactionServiceInternal transactionServiceInternal;

    @GetMapping("/summary/{id}")
    public ResponseEntity<TransactionSummaryDTO> getSummary(
            @PathVariable UUID id
    ) {
        TransactionSummaryDTO response = transactionServiceInternal.getSummaryDTO(id);
        return ResponseEntity.ok().body(response);
    }
}
