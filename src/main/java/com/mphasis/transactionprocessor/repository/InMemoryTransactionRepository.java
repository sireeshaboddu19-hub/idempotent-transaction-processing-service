
package com.mphasis.transactionprocessor.repository;

import com.mphasis.transactionprocessor.model.TransactionRecord;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTransactionRepository {

    private final ConcurrentHashMap<String, TransactionRecord> transactions =
            new ConcurrentHashMap<>();

    public boolean saveIfAbsent(TransactionRecord record) {
        return transactions.putIfAbsent(
                record.getTransactionId(), record
        ) == null;
    }

    public Optional<TransactionRecord> findByTransactionId(
            String transactionId) {
        return Optional.ofNullable(transactions.get(transactionId));
    }

    public Collection<TransactionRecord> findAll() {
        return transactions.values();
    }

    public long count() {
        return transactions.size();
    }
}
