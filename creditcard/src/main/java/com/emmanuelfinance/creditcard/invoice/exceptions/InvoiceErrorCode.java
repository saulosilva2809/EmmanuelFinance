package com.emmanuelfinance.creditcard.invoice.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InvoiceErrorCode {
    INVOICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Invoice not found."
    );

    private final HttpStatus status;
    private final String message;

    InvoiceErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}