package com.emmanuelfinance.creditcard.controllers;

import com.emmanuelfinance.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/credit-card")
@RequiredArgsConstructor
public class CreditCardInternalController {

    private final CreditCardInternalService creditCardInternalService;

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardSummaryDTO> view(@PathVariable UUID id) {
        CreditCardSummaryDTO response = creditCardInternalService.getCreditCardSummary(id);
        return ResponseEntity.ok().body(response);
    }
}
