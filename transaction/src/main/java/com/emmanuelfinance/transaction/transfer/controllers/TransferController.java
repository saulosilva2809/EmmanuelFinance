package com.emmanuelfinance.transaction.transfer.controllers;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.transaction.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.ResponseTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.TransferFiltersDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import com.emmanuelfinance.transaction.transfer.query.TransferQueryService;
import com.emmanuelfinance.transaction.transfer.services.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final TransferQueryService transferQueryService;

    @PostMapping()
    public ResponseEntity<ResponseTransferDTO> create(
            @Valid @RequestBody CreateTransferDTO data
    ) {
        ResponseTransferDTO response = transferService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseTransferDTO> view(
            @PathVariable UUID id
    ) {
        ResponseTransferDTO response = transferQueryService.view(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping()
    public ResponseEntity<PageResponseDTO<ResponseTransferDTO>> list(
            TransferFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseTransferDTO> response = transferQueryService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponseDTO<ResponseTransferDTO>> listDeleted(
            TransferFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseTransferDTO> response = transferQueryService.listDeleted(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTransferDTO> update(
            @PathVariable UUID id,
            @RequestBody UpdateTransferDTO data
    ) {
        ResponseTransferDTO response = transferService.update(id, data);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transferService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        transferService.restore(id);
        return ResponseEntity.noContent().build();
    }
}
