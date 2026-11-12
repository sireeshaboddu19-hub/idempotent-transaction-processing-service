
package com.mphasis.transactionprocessor.controller;

import com.mphasis.transactionprocessor.model.TransactionRecord;
import com.mphasis.transactionprocessor.model.TransactionRequest;
import com.mphasis.transactionprocessor.model.TransactionStatus;
import com.mphasis.transactionprocessor.repository.InMemoryTransactionRepository;
import com.mphasis.transactionprocessor.service.TransactionProcessingService;
import com.mphasis.transactionprocessor.repository.InMemoryLedgerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionProcessingService processingService;
    private final InMemoryTransactionRepository repository;
    private final InMemoryLedgerRepository ledgerRepository;

    public TransactionController(
        TransactionProcessingService processingService,
        InMemoryTransactionRepository repository,
        InMemoryLedgerRepository ledgerRepository) {

    this.processingService = processingService;
    this.repository = repository;
    this.ledgerRepository = ledgerRepository;
}

    @PostMapping
    public ResponseEntity<TransactionRecord> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {

        TransactionRecord record =
                processingService.submit(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(record);
    }

    @PostMapping("/{transactionId}/retry")
    public ResponseEntity<TransactionRecord> retryTransaction(
            @PathVariable String transactionId) {

        TransactionRecord record =
                processingService.retry(transactionId);

        return ResponseEntity.ok(record);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionRecord> getTransaction(
            @PathVariable String transactionId) {

        return repository.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Collection<TransactionRecord> getAllTransactions() {
        return repository.findAll();
    }

    @GetMapping("/summary")
    public Map<String, Long> getProcessingSummary() {

        return Arrays.stream(TransactionStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        status -> repository.findAll().stream()
                                .filter(record ->
                                        record.getStatus() == status)
                                .count()
                ));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public Map<String, Object> getAccountBalance(
        @PathVariable String accountId) {

        return Map.of(
            "accountId", accountId,
            "balance", ledgerRepository.getBalance(accountId)
    );
    }
}
