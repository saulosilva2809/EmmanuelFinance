package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
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
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping()
    public ResponseEntity<ResponseTransactionDTO> create(
            @Valid @RequestBody CreateTransactionDTO data
    ) {
        ResponseTransactionDTO response = transactionService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> view(
            @PathVariable UUID id
    ) {
        ResponseTransactionDTO response = transactionService.view(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ResponseTransactionDTO>> list(
            TransactionFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseTransactionDTO> response = transactionService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponseDTO<ResponseTransactionDTO>> listDeleted(
            TransactionFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseTransactionDTO> response = transactionService.listDeleted(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransactionDTO> update(
            @PathVariable UUID id,
            @RequestBody UpdateTransactionDTO data
    ) {
        ResponseTransactionDTO response = transactionService.update(id, data);
        return ResponseEntity.ok().body(response);
    }
}
