package com.emmanuelfinance.creditcard.creditcard.controllers;

import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardQueryService;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardService;
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
@RequestMapping("/credit-card")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardQueryService creditCardQueryService;

    @PostMapping
    public ResponseEntity<ResponseCreditCardDTO> create(@Valid @RequestBody CreateCreditCardDTO data) {
        ResponseCreditCardDTO response = creditCardService.create(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseCreditCardDTO> view(@PathVariable UUID id) {
        ResponseCreditCardDTO response = creditCardQueryService.view(id);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ResponseCreditCardDTO>> list(
            CreditCardFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseCreditCardDTO> response = creditCardQueryService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponseDTO<ResponseCreditCardDTO>> listDeleted(
            CreditCardFiltersDTO filters,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseCreditCardDTO> response = creditCardQueryService.listDeleted(filters, pageable);
        return ResponseEntity.ok().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseCreditCardDTO> update(
            @PathVariable UUID id,
            @RequestBody UpdateCreditCardDTO data
    ) {
        ResponseCreditCardDTO response = creditCardService.update(id, data);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        creditCardService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        creditCardService.restore(id);
        return ResponseEntity.noContent().build();
    }
}
