package com.emmanuelfinance.creditcard.invoice.controllers;

import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceItemService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/credit-card/invoice-item")
@RequiredArgsConstructor
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;

    @GetMapping("/{invoiceId}")
    public ResponseEntity<PageResponseDTO<ResponseInvoiceItemDTO>> listById(
            @PathVariable UUID invoiceId,
            @PageableDefault(
                    sort = {"installmentNumber", "createdAt"},
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseInvoiceItemDTO> response = invoiceItemService.listByInvoiceId(
                invoiceId,
                pageable
        );
        return ResponseEntity.ok().body(response);
    }
}
