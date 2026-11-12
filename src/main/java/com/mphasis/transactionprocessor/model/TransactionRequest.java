
package com.mphasis.transactionprocessor.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(

        @NotBlank(message = "Transaction ID is required")
        String transactionId,

        @NotBlank(message = "Request ID is required")
        String requestId,

        @NotBlank(message = "Account ID is required")
        String accountId,

        @NotNull(message = "Transaction type is required")
        TransactionType transactionType,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = false,
                message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @Positive(message = "Sequence number must be positive")
        long sequenceNumber
) {
}
