
package com.mphasis.transactionprocessor.repository;

import com.mphasis.transactionprocessor.model.LedgerEntry;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryLedgerRepository {

    private final ConcurrentHashMap<String, LedgerEntry> ledger =
            new ConcurrentHashMap<>();

    public boolean addEntryIfAbsent(LedgerEntry entry) {
        return ledger.putIfAbsent(entry.transactionId(), entry) == null;
    }

    public Optional<LedgerEntry> findByTransactionId(String transactionId) {
        return Optional.ofNullable(ledger.get(transactionId));
    }

    public Collection<LedgerEntry> findAll() {
        return ledger.values();
    }

    public BigDecimal getBalance(String accountId) {
        return ledger.values().stream()
                .filter(entry -> entry.accountId().equals(accountId))
                .map(entry -> {
                    return switch (entry.transactionType()) {
                        case CREDIT -> entry.amount();
                        case DEBIT, REVERSAL -> entry.amount().negate();
                    };
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long count() {
        return ledger.size();
    }
}
