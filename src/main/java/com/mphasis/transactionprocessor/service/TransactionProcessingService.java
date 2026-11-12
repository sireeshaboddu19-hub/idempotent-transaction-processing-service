
package com.mphasis.transactionprocessor.service;

import com.mphasis.transactionprocessor.model.LedgerEntry;
import com.mphasis.transactionprocessor.model.TransactionRecord;
import com.mphasis.transactionprocessor.model.TransactionRequest;
import com.mphasis.transactionprocessor.model.TransactionStatus;
import com.mphasis.transactionprocessor.repository.InMemoryLedgerRepository;
import com.mphasis.transactionprocessor.repository.InMemoryTransactionRepository;
import org.springframework.stereotype.Service;
import com.mphasis.transactionprocessor.exception.DuplicateTransactionException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionProcessingService {

    private final InMemoryTransactionRepository repository;
    private final InMemoryLedgerRepository ledgerRepository;

    private static final int MAX_RETRIES = 3;

    public TransactionProcessingService(
            InMemoryTransactionRepository repository,
            InMemoryLedgerRepository ledgerRepository) {
        this.repository = repository;
        this.ledgerRepository = ledgerRepository;
    }

    
    public synchronized TransactionRecord submit(TransactionRequest request) {

        var existing = repository.findByTransactionId(request.transactionId());

        if (existing.isPresent()) {

            TransactionRecord existingRecord = existing.get();

        if (!isSameRequest(existingRecord.getRequest(), request)) {
            throw new DuplicateTransactionException(
                    "Transaction ID already exists with different request data");
            }

            return existingRecord;
        }

        TransactionRecord record = new TransactionRecord(request);

        repository.saveIfAbsent(record);

        process(record);
        processPendingTransactions(request.accountId());

        return record;
    }

    private void process(TransactionRecord record) {

        long expectedSequence = getExpectedSequence(record.getAccountId());
        long incomingSequence = record.getSequenceNumber();

        if (incomingSequence > expectedSequence) {
            record.setStatus(TransactionStatus.PENDING);
            return;
        }

        if (incomingSequence < expectedSequence) {
            record.setStatus(TransactionStatus.DUPLICATE);
            return;
        }

        record.setStatus(TransactionStatus.PROCESSING);

        try {
            simulateProcessing(record);

            LedgerEntry entry = new LedgerEntry(
                    record.getTransactionId(),
                    record.getAccountId(),
                    record.getTransactionType(),
                    record.getAmount(),
                    record.getRequest().currency(),
                    record.getSequenceNumber(),
                    Instant.now()
            );

            ledgerRepository.addEntryIfAbsent(entry);

            record.setFailureReason(null);
            record.setStatus(TransactionStatus.PROCESSED);

        } catch (RuntimeException exception) {

            record.incrementRetryCount();
            record.setFailureReason(exception.getMessage());

            if (record.getRetryCount() < MAX_RETRIES) {
                record.setStatus(TransactionStatus.RETRY_PENDING);
            } else {
                record.setStatus(TransactionStatus.FAILED);
            }
        }
    }

    private long getExpectedSequence(String accountId) {

        List<TransactionRecord> processed = repository.findAll().stream()
                .filter(r -> r.getAccountId().equals(accountId))
                .filter(r -> r.getStatus() == TransactionStatus.PROCESSED)
                .toList();

        return processed.stream()
                .mapToLong(TransactionRecord::getSequenceNumber)
                .max()
                .orElse(0L) + 1;
    }

    private void processPendingTransactions(String accountId) {

        List<TransactionRecord> pending = repository.findAll().stream()
                .filter(r -> r.getAccountId().equals(accountId))
                .filter(r -> r.getStatus() == TransactionStatus.PENDING)
                .sorted(Comparator.comparingLong(
                        TransactionRecord::getSequenceNumber))
                .toList();

        for (TransactionRecord record : pending) {

            if (record.getSequenceNumber() == getExpectedSequence(accountId)) {
                process(record);
            }
        }
    }

    public synchronized TransactionRecord retry(String transactionId) {

        TransactionRecord record = repository.findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found"));

        if (record.getStatus() != TransactionStatus.RETRY_PENDING) {
            throw new IllegalStateException(
                    "Transaction is not eligible for retry");
        }

        process(record);
        processPendingTransactions(record.getAccountId());

        return record;
    }

    private void simulateProcessing(TransactionRecord record) {

        // Simulate a transient failure for ACC-203.
        if ("ACC-203".equals(record.getAccountId())
                && record.getRetryCount() == 0) {
            throw new RuntimeException("Simulated transient failure");
        }

        System.out.println(
                "Processed transaction: " + record.getTransactionId());
    }

    public InMemoryTransactionRepository getRepository() {
        return repository;
    }

    public InMemoryLedgerRepository getLedgerRepository() {
        return ledgerRepository;
    }


    private boolean isSameRequest(
        TransactionRequest existing,
        TransactionRequest incoming) {

        return existing.transactionId().equals(incoming.transactionId())
            && existing.requestId().equals(incoming.requestId())
            && existing.accountId().equals(incoming.accountId())
            && existing.transactionType() == incoming.transactionType()
            && existing.amount().compareTo(incoming.amount()) == 0
            && existing.currency().equals(incoming.currency())
            && existing.sequenceNumber() == incoming.sequenceNumber();
    }

}
