
package com.mphasis.transactionprocessor;

import com.mphasis.transactionprocessor.model.TransactionRecord;
import com.mphasis.transactionprocessor.model.TransactionRequest;
import com.mphasis.transactionprocessor.model.TransactionStatus;
import com.mphasis.transactionprocessor.model.TransactionType;
import com.mphasis.transactionprocessor.repository.InMemoryLedgerRepository;
import com.mphasis.transactionprocessor.repository.InMemoryTransactionRepository;
import com.mphasis.transactionprocessor.service.TransactionProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessingServiceTest {

    private TransactionProcessingService service;
    private InMemoryLedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        InMemoryTransactionRepository transactionRepository =
                new InMemoryTransactionRepository();

        ledgerRepository = new InMemoryLedgerRepository();

        service = new TransactionProcessingService(
                transactionRepository,
                ledgerRepository
        );
    }

    private TransactionRequest createRequest(
            String transactionId,
            String accountId,
            TransactionType type,
            String amount,
            long sequence) {

        return new TransactionRequest(
                transactionId,
                "REQ-" + transactionId,
                accountId,
                type,
                new BigDecimal(amount),
                "INR",
                sequence
        );
    }

    @Test
    void shouldProcessCreditAndUpdateBalance() {

        TransactionRecord record = service.submit(
                createRequest("TXN-1", "ACC-1",
                        TransactionType.CREDIT, "100", 1)
        );

        assertEquals(TransactionStatus.PROCESSED, record.getStatus());
        assertEquals(new BigDecimal("100"),
                ledgerRepository.getBalance("ACC-1"));
    }

    @Test
    void shouldNotApplyDuplicateTransactionTwice() {

        var request = createRequest("TXN-2", "ACC-2",
                TransactionType.CREDIT, "100", 1);

        service.submit(request);
        service.submit(request);

        assertEquals(new BigDecimal("100"),
                ledgerRepository.getBalance("ACC-2"));

        assertEquals(1, ledgerRepository.count());
    }

    @Test
    void shouldMarkDuplicateSequence() {

        service.submit(createRequest("TXN-3", "ACC-3",
                TransactionType.CREDIT, "100", 1));

        TransactionRecord duplicate = service.submit(
                createRequest("TXN-4", "ACC-3",
                        TransactionType.CREDIT, "50", 1)
        );

        assertEquals(TransactionStatus.DUPLICATE, duplicate.getStatus());

        assertEquals(new BigDecimal("100"),
                ledgerRepository.getBalance("ACC-3"));
    }

    @Test
    void shouldProcessTransactionsInSequence() {

        TransactionRecord second = service.submit(
                createRequest("TXN-6", "ACC-4",
                        TransactionType.DEBIT, "40", 2)
        );

        assertEquals(TransactionStatus.PENDING, second.getStatus());

        service.submit(createRequest("TXN-5", "ACC-4",
                TransactionType.CREDIT, "100", 1));

        assertEquals(TransactionStatus.PROCESSED, second.getStatus());

        assertEquals(new BigDecimal("60"),
                ledgerRepository.getBalance("ACC-4"));
    }

    @Test
    void shouldRetryTransientFailureSuccessfully() {

        TransactionRecord record = service.submit(
                createRequest("TXN-7", "ACC-203",
                        TransactionType.CREDIT, "200", 1)
        );

        assertEquals(TransactionStatus.RETRY_PENDING, record.getStatus());
        assertEquals(1, record.getRetryCount());

        service.retry("TXN-7");

        assertEquals(TransactionStatus.PROCESSED, record.getStatus());
        assertEquals(new BigDecimal("200"),
                ledgerRepository.getBalance("ACC-203"));
        assertNull(record.getFailureReason());
    }
}
