package com.emmanuelfinance.creditcard.payment.controllers;

import com.emmanuelfinance.creditcard.payment.dto.PaymentTotalDTO;
import com.emmanuelfinance.creditcard.payment.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/credit-card/")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("{creditCardId}/payment/{invoiceId}/total")
    public ResponseEntity<Void> totalInvoicePayment(
            @PathVariable UUID creditCardId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody PaymentTotalDTO data
    ) {
        paymentService.totalInvoicePayment(creditCardId, invoiceId, data);
        return ResponseEntity.noContent().build();
    }
}
