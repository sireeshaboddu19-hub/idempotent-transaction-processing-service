
# Idempotent Background Transaction Processing Service

A Spring Boot REST API that processes financial transactions with idempotency, sequence validation, duplicate detection, retry handling, and ledger balance tracking.

## Features

- Idempotent transaction submission
- Duplicate transaction detection
- Account-level transaction sequence enforcement
- Out-of-order transaction handling
- Retry handling for transient failures
- Transaction status tracking
- Ledger balance calculation
- Request validation
- REST APIs for transaction processing and monitoring
- Unit tests using JUnit
- Spring Boot Actuator health endpoint


## Technology Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Validation
- Spring Boot Actuator
- Maven
- JUnit 5
- ConcurrentHashMap (in-memory repositories)


## Prerequisites

- Java 17 installed
- Git installed (for cloning the repository)

## Run Locally

Clone the repository:

```bash
git clone https://github.com/sireeshaboddu19-hub/idempotent-transaction-processing-service

cd transaction-processor
```

Run the application using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

For Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Application runs at:

http://localhost:8080

Health endpoint:

http://localhost:8080/actuator/health


## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions` | Submit a transaction |
| GET | `/api/transactions/{transactionId}` | Get transaction details |
| GET | `/api/transactions` | Get all transactions |
| POST | `/api/transactions/{transactionId}/retry` | Retry a failed transaction |
| GET | `/api/transactions/summary` | Get transaction processing summary |
| GET | `/api/transactions/accounts/{accountId}/balance` | Get account ledger balance |

## Transaction Request

Example request:

```json
{
  "transactionId": "TXN-1001",
  "requestId": "REQ-1001",
  "accountId": "ACC-100",
  "transactionType": "CREDIT",
  "amount": 100.00,
  "currency": "INR",
  "sequenceNumber": 1
}
```

### Transaction Types

- `CREDIT`
- `DEBIT`
- `REVERSAL`

### Transaction Statuses

- `RECEIVED`
- `PROCESSING`
- `PROCESSED`
- `DUPLICATE`
- `FAILED`
- `RETRY_PENDING`
- `PENDING`


## Sample Responses

### Successful Transaction

A successful transaction submission returns:

```json
{
  "transactionId": "TXN-1001",
  "requestId": "REQ-1001",
  "accountId": "ACC-100",
  "transactionType": "CREDIT",
  "amount": 100.00,
  "sequenceNumber": 1,
  "status": "PROCESSED",
  "retryCount": 0
}
```

### Duplicate Transaction

Submitting the same transaction ID with different request data returns HTTP `409 Conflict`.

### Out-of-Order Transaction

A transaction submitted before its expected sequence is marked `PENDING` and processed when the preceding sequence becomes available.

### Transient Failure

A simulated transient failure results in `RETRY_PENDING`. The retry endpoint can be used to retry processing.


## Design Decisions

### Idempotency

- `transactionId` acts as the idempotency key.
- The transaction repository uses `ConcurrentHashMap` and `putIfAbsent`-style insertion to prevent duplicate transaction records.
- Repeated submissions with identical transaction data return the existing transaction record.
- Reusing a transaction ID with different request data returns HTTP 409 Conflict.
- Ledger entries are also inserted only once per transaction ID.

### Sequence Handling

- Transactions are processed in account-level sequence order.
- The expected sequence number is determined from the highest successfully processed sequence.
- Transactions arriving ahead of the expected sequence are marked `PENDING`.
- Pending transactions are processed automatically when the missing sequence is submitted.
- Transactions with already processed sequence numbers are marked `DUPLICATE`.

### Retry Strategy

- Transient failures are represented using `RETRY_PENDING`.
- A retry counter tracks retry attempts.
- Processing allows a maximum of three attempts.
- A retry API allows eligible transactions to be retried.
- Successfully retried transactions clear their failure reason.

### Ledger

- Successfully processed transactions generate ledger entries.
- CREDIT increases account balance.
- DEBIT and REVERSAL decrease account balance.
- Duplicate transactions do not create additional ledger entries.


## Architecture

The application follows a layered architecture:

```text
REST Controller
      |
      v
Transaction Processing Service
      |
      +----------------------+
      |                      |
      v                      v
Transaction Repository   Ledger Repository
      |                      |
      v                      v
Transaction Records      Ledger Entries
```

### Responsibilities

- **Controller:** Exposes REST endpoints and handles HTTP requests.
- **Service:** Implements idempotency, sequence enforcement, transaction processing, and retries.
- **Transaction Repository:** Stores transaction records using an in-memory ConcurrentHashMap.
- **Ledger Repository:** Stores ledger entries and calculates account balances.
- **Exception Handler:** Converts validation and business exceptions into appropriate HTTP responses.


## Testing

Run the unit tests:

```bash
./mvnw test
```

For Windows PowerShell:

```powershell
.\mvnw.cmd test
```

### Test Scenarios

The test suite covers:

- Successful credit processing and balance updates
- Duplicate transaction idempotency
- Duplicate sequence handling
- Out-of-order transaction processing
- Successful retry after transient failure

Latest test result:

```text
Tests run: 6
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

## Observability

- Spring Boot Actuator health endpoint
- Transaction processing logs
- Transaction status tracking
- Retry count and failure reason tracking
- Transaction summary endpoint


## Production Hardening Considerations

This assessment implementation uses in-memory repositories for simplicity. In a production environment, the following improvements would be appropriate:

- Replace in-memory repositories with a durable database.
- Enforce idempotency using database unique constraints and atomic transactions.
- Use distributed locking or concurrency controls for account-level sequence processing.
- Implement a durable message queue for background transaction processing.
- Add configurable retry policies with exponential backoff.
- Introduce dead-letter handling for permanently failed transactions.
- Add structured logging, correlation IDs, metrics, and distributed tracing.
- Secure APIs using authentication and authorization.
- Add integration tests and load tests.
- Ensure ledger updates and transaction status changes are atomic.


## AI Usage

AI assistance was used during development for:

- Understanding assessment requirements.
- Discussing implementation approaches and design decisions.
- Reviewing code structure and debugging errors.
- Preparing documentation and test scenarios.

All implementation decisions and generated code were reviewed, tested, and validated by the developer.


## Sample Requests

Sample JSON payloads are available in the `sample-requests/` directory:

- `credit-transaction.json` – Successful credit transaction.
- `debit-transaction.json` – Debit transaction following a credit.
- `out-of-order-transaction.json` – Transaction arriving before its expected sequence.
- `retry-transaction.json` – Transaction designed to trigger a simulated transient failure.

Submit a sample request using:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d @sample-requests/credit-transaction.json
```



