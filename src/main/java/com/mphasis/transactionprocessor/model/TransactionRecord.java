
package com.mphasis.transactionprocessor.model;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRecord {

    private final TransactionRequest request;
    private TransactionStatus status;
    private int retryCount;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public TransactionRecord(TransactionRequest request) {
        this.request = request;
        this.status = TransactionStatus.RECEIVED;
        this.retryCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public TransactionRequest getRequest() {
        return request;
    }

    public String getTransactionId() {
        return request.transactionId();
    }

    public String getAccountId() {
        return request.accountId();
    }

    public long getSequenceNumber() {
        return request.sequenceNumber();
    }

    public BigDecimal getAmount() {
        return request.amount();
    }

    public TransactionType getTransactionType() {
        return request.transactionType();
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
