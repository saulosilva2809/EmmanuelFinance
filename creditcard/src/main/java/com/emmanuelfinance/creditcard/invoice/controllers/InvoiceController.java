package com.emmanuelfinance.creditcard.invoice.controllers;

import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-card/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<PageResponseDTO<ResponseInvoiceDTO>> list(
            InvoiceFiltersDTO filters,
            @PageableDefault(
                    sort = {"year", "month"},
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        PageResponseDTO<ResponseInvoiceDTO> response = invoiceService.list(filters, pageable);
        return ResponseEntity.ok().body(response);
    }
}
