# PayPulse

## Business Objective

PayPulse is a **Batch Payment Orchestration Platform**.

It does not process payments directly. Instead, it orchestrates the full batch payment lifecycle:

1. Users submit a batch payment request.
2. PayPulse validates and stores the request.
3. PayPulse immediately returns `202 Accepted`.
4. A background worker asynchronously calls an external SOAP service.
5. The SOAP service processes transactions in the batch.
6. PayPulse tracks transaction outcomes and derives batch status.
7. Batch and transaction data are persisted in PostgreSQL.
8. Users can query live status and historical batches.

---

## Core Components

```text
Frontend
   |
   v
PayPulse REST API
   |
   +------------------------+
   |                        |
   v                        v
PostgreSQL            Background Worker
                              |
                              v
                    External SOAP Service
```

---

## API Suite

### 1) Submit Batch Payment

- **Purpose:** Initiate a new payment batch
- **Endpoint:** `POST /api/v1/payment-batch`
- **Behavior:**
  - Validate payload
  - Create batch
  - Create transactions
  - Persist in PostgreSQL
  - Assign status `PENDING`
  - Trigger async worker
  - Return `202 Accepted`

**Request**

```json
{
  "merchantId": "MERCHANT-12345",
  "customerId": "CUSTOMER-67890",
  "batchId": "BATCH-20260710-001",
  "idempotencyKey": "8d83c7b4-98f3-440b-a0b0-fd741ef5d623",
  "totalAmount": 3500.00,
  "currency": "EUR",
  "paymentMethod": "SEPA",
  "executionDate": "2026-07-15",
  "batchDescription": "July invoices batch",
  "requestedBy": "user@merchant.com",
  "payments": [
    {
      "paymentId": "PAY-001",
      "beneficiaryId": "BENEFICIARY-001",
      "beneficiaryName": "Vendor A",
      "beneficiaryIBAN": "DE89370400440532013000",
      "amount": 1500.00,
      "paymentReference": "Invoice INV-1001",
      "description": "Payment for services"
    },
    {
      "paymentId": "PAY-002",
      "beneficiaryId": "BENEFICIARY-002",
      "beneficiaryName": "Vendor B",
      "beneficiaryIBAN": "DE89370400440532013000",
      "amount": 1000.00,
      "paymentReference": "Invoice INV-1002",
      "description": "Payment for services"
    },
    {
      "paymentId": "PAY-002",
      "beneficiaryId": "BENEFICIARY-003",
      "beneficiaryName": "Vendor C",
      "beneficiaryIBAN": "DE89370400440532013000",
      "amount": 1000.00,
      "paymentReference": "Invoice INV-1003",
      "description": "Payment for services"
    }
  ]
}
```
**Sample Response**

```json
{
  "batchId": "BP-20260709-00001",
  "status": "PENDING",
  "createdAt": "2026-07-10T09:15:34",
  "statusUrl": "/api/v1/payment-batches/BP-20260709-00001/status",
  "isDuplicate": false
}
```

### 2) Batch Status

- **Purpose:** Retrieve real-time batch status
- **Endpoint:** `GET /api/v1/payment-batches/{batchId}/status`

**Sample Response**

```json
{
  "batchId": "BP-20260709-00001",
  "status": "IN_PROGRESS",
  "summary": {
    "totalTransactions": 500,
    "successfulTransactions": 320,
    "failedTransactions": 12,
    "pendingTransactions": 168
  },
  "timing": {
    "createdAt": "2026-07-10T09:15:34",
    "lastUpdatedAt": "2026-07-10T09:25:00",
    "estimatedCompletionTime": "2026-07-10T10:30:00"
  },
  "failureInfo": {
    "retryableFailures": 5,
    "permanentFailures": 7,
    "lastErrorMessage": "IBAN validation failed"
  },
  "links": {
    "paymentDetails": "/api/v1/payment-batches/BP-20260709-00001/payments",
    "failedPayments": "/api/v1/payment-batches/BP-20260709-00001/payments?status=FAILED"
  }
}
```

### 3) Historical Batch Retrieval

- **Purpose:** Retrieve submitted batches for a time window
- **Endpoint:** `GET /api/v1/payment-batches`

**Supported Filters**

- Last 3 months:
  - `GET /api/v1/payment-batches?period=LAST_3_MONTHS`
- Last 6 months:
  - `GET /api/v1/payment-batches?period=LAST_6_MONTHS`
- Custom range:
  - `GET /api/v1/payment-batches?fromDate=2026-01-01&toDate=2026-06-30`

### Important Requirement

If historical data is not available locally, PayPulse can call a SOAP historical API and optionally cache the retrieved records in PostgreSQL.

```text
SOAP Historical Service
   |
   +--> Retrieve historical batches
```

---

## Asynchronous Processing Flow

1. User submits batch (`POST` request).
2. PayPulse persists:
   - `Batch = PENDING`
   - `Transactions = PENDING`
3. PayPulse returns immediately: `202 Accepted`.
4. Background worker starts (`BatchProcessorWorker`).
5. Worker invokes SOAP submit operation (`submitBatch(batchId)`).
6. SOAP service processes transactions (`SUCCESS` / `FAILED`).
7. Worker updates `payment_batch` and `payment_transaction` tables.
8. Batch status is recalculated from transaction outcomes.

**Example aggregation**

- Total: `500`
- Success: `450`
- Failed: `50`
- Derived Status: `PARTIALLY_COMPLETED`

---

## Batch Status Calculation Rules

- `PENDING`: all transactions are pending
- `IN_PROGRESS`: at least one transaction is currently processing
- `COMPLETED`: all transactions succeeded
- `FAILED`: all transactions failed
- `PARTIALLY_COMPLETED`: mix of success and failure

---

## Database Model

### `payment_batch`

- `id`
- `batch_id`
- `batch_reference`
- `requested_by`
- `status`
- `total_transactions`
- `successful_transactions`
- `failed_transactions`
- `pending_transactions`
- `progress_percentage`
- `created_at`
- `updated_at`

### `payment_transaction`

- `id`
- `transaction_id`
- `batch_id`
- `employee_id`
- `amount`
- `currency`
- `transaction_status`
- `external_reference`
- `failure_reason`
- `processed_at`
- `created_at`
- `updated_at`

---

## Sequence Diagram

```text
+--------+           +-----------+         +------------+          +-------------+          +-------------+
|   FE   |           | PayPulse  |         | PostgreSQL |          | SOAP Worker |          | SOAP System |
+--------+           +-----------+         +------------+          +-------------+          +-------------+
|                    |                     |                        |                        |
| POST Batch         |                     |                        |                        |
|------------------->|                     |                        |                        |
|                    | Save Batch          |                        |                        |
|                    |-------------------->|                        |                        |
|                    | Save Transactions   |                        |                        |
|                    |-------------------->|                        |                        |
|                    |                     |                        |                        |
| 202 Accepted       |                     |                        |                        |
|<-------------------|                     |                        |                        |
|                    |                     |                        |                        |
|                    | Publish Async Task  |                        |                        |
|                    |--------------------------------------------->|                        |
|                    |                     |                        |                        |
|                    |                     |                        | Call SOAP Submit       |
|                    |                     |                        |----------------------->|
|                    |                     |                        |                        |
|                    |                     |                        | SOAP Response          |
|                    |                     |                        |<-----------------------|
|                    |                     | Update Transactions    |                        |
|                    |<---------------------------------------------|                        |
|                    |                     |                        |                        |
|                    | Update Batch Status |                        |                        |
|                    |-------------------->|                        |                        |
|                    |                     |                        |                        |
```

---

## Suggested Internal Architecture

```text
com.paypulse
│
├── api
│   └── PaymentBatchController
│
├── application
│   ├── BatchSubmissionService
│   ├── BatchStatusService
│   └── HistoricalBatchService
│
├── domain
│   ├── PaymentBatch
│   ├── PaymentTransaction
│   ├── BatchStatus
│   └── TransactionStatus
│
├── infrastructure
│   ├── soap
│   │   ├── PaymentSoapClient
│   │   └── HistoricalPaymentSoapClient
│   └── worker
│       └── BatchProcessingWorker
│
├── persistence
│   ├── PaymentBatchRepository
│   └── PaymentTransactionRepository
│
├── configuration
└── exception
```

---

## Why This Design Is Strong

This design demonstrates:

- Async REST processing (`202 Accepted` pattern)
- Background worker orchestration
- SOAP integration
- PostgreSQL persistence
- Batch-level orchestration with transaction-level tracking
- Aggregated status and progress reporting
- Historical search with optional SOAP fallback
- Enterprise integration patterns for legacy-to-modern systems

## For portfolio, interview, or architecture discussions, this is stronger than a simple CRUD payment app because it reflects real-world financial integration and asynchronous orchestration concerns.

### Prompt: Generate PayPulse Backend Application

Act as a **Principal Software Architect**, **Senior Java Engineer**, and **Enterprise Integration Specialist**.

Generate a complete production-grade backend application called **PayPulse**.

#### Technology Stack

- Java 25
- Spring Boot 4
- Maven
- PostgreSQL
- Spring Data JPA
- Flyway
- Spring Validation
- OpenAPI / Swagger
- Lombok
- MapStruct
- Virtual Threads (Project Loom)
- Structured Logging (SLF4J)
- Docker
- JUnit 5
- Testcontainers
- Clean Architecture
- SOLID Principles
- Domain Driven Design (DDD)

#### Business Context

PayPulse is a batch payment orchestration platform.

Users submit payment batches through a REST API. A payment batch contains multiple payment transactions.

After a batch is submitted:

1. The request is validated.
2. Batch details are persisted in PostgreSQL.
3. Individual payment transactions are persisted.
4. API immediately returns HTTP `202 Accepted`.
5. A background worker is triggered asynchronously.
6. The worker invokes an external SOAP service.
7. The SOAP service processes all transactions belonging to the batch.
8. PayPulse updates transaction statuses in PostgreSQL.
9. Batch status is derived from aggregated transaction statuses.
10. Clients can poll the status endpoint.
11. Clients can retrieve historical batches for specified date ranges.

The system is an orchestration layer and does **not** perform payment processing itself.

#### Functional Requirements

##### API #1 - Submit Batch Payment Request

- **Endpoint:** `POST /api/v1/payment-batch`

**Request**

```json
{
  "batchReference": "PAYROLL-JULY-2026",
  "requestedBy": "finance-team",
  "transactions": [
    {
      "employeeId": "EMP001",
      "amount": 2500.00,
      "currency": "EUR"
    },
    {
      "employeeId": "EMP002",
      "amount": 3500.00,
      "currency": "EUR"
    }
  ]
}
```

**Expected Behavior**

- Validate request.
- Create payment batch record.
- Create transaction records.
- Store everything in PostgreSQL.
- Set batch status to `PENDING`.
- Trigger asynchronous processing.
- Return HTTP `202 Accepted`.

**Response**

```json
{
  "batchId": "BP-20260709-000001",
  "status": "PENDING",
  "statusUrl": "/api/v1/payment-batches/BP-20260709-000001/status"
}
```

##### API #2 - Retrieve Batch Status

- **Endpoint:** `GET /api/v1/payment-batches/{batchId}/status`

**Response**

```json
{
  "batchId": "BP-20260709-000001",
  "batchStatus": "IN_PROGRESS",
  "totalTransactions": 100,
  "successfulTransactions": 50,
  "failedTransactions": 5,
  "pendingTransactions": 45,
  "progressPercentage": 55,
  "lastUpdated": "2026-07-09T12:30:00Z"
}
```

Status must be calculated from transaction statuses.

##### API #3 - Retrieve Historical Payment Batches

- **Endpoint:** `GET /api/v1/payment-batches`

Support the following filters:

1. Last 3 months:
   - `GET /api/v1/payment-batches?period=LAST_3_MONTHS`
2. Last 6 months:
   - `GET /api/v1/payment-batches?period=LAST_6_MONTHS`
3. Custom date range:
   - `GET /api/v1/payment-batches?fromDate=2026-01-01&toDate=2026-06-30`

Response should include:

- `batchId`
- `batchReference`
- `submittedBy`
- `submittedAt`
- `status`
- `totalTransactions`
- `progressPercentage`

Historical data source:

- **Primary source:** PostgreSQL
- If historical data is not available locally, the application should demonstrate how a SOAP historical service could be queried and synchronized.

#### Async Processing Requirements

After a batch is submitted:

1. Create asynchronous processing task.
2. Use Virtual Thread Executor.
3. Worker invokes SOAP payment service.
4. Simulate response if SOAP service unavailable.
5. Update individual transaction statuses.
6. Update overall batch metrics.
7. Persist all updates.

The REST thread must never wait for processing and must return immediately with HTTP `202`.

#### SOAP Integration Requirements

Create infrastructure for SOAP integration.

**Payment SOAP Client**

- `submitBatch(batchId)`

**Mock SOAP Response Example**

```json
{
  "transactionId": "TXN001",
  "status": "SUCCESS"
}
```

**Historical SOAP Client**

- `retrieveHistoricalBatches(fromDate, toDate)`

Implement SOAP adapters using Spring Web Services and provide mock implementations for local development.

#### Database Requirements

**Table: `payment_batch`**

- `id` (UUID)
- `batch_id` (business identifier)
- `batch_reference`
- `requested_by`
- `status`
- `total_transactions`
- `successful_transactions`
- `failed_transactions`
- `pending_transactions`
- `progress_percentage`
- `created_at`
- `updated_at`

**Table: `payment_transaction`**

- `id` (UUID)
- `transaction_id`
- `batch_id`
- `employee_id`
- `amount`
- `currency`
- `status`
- `external_reference`
- `failure_reason`
- `created_at`
- `updated_at`

#### Status Rules

**Batch status values**

- `PENDING`
- `IN_PROGRESS`
- `COMPLETED`
- `FAILED`
- `PARTIALLY_COMPLETED`

**Calculation rules**

- `PENDING` -> all transactions pending
- `IN_PROGRESS` -> at least one transaction processing
- `COMPLETED` -> all transactions successful
- `FAILED` -> all transactions failed
- `PARTIALLY_COMPLETED` -> combination of success and failure

**Transaction status values**

- `PENDING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`

#### Architecture Requirements

Use clean layered architecture.

**Package structure**

```text
com.paypulse
├── api
├── application
├── domain
├── persistence
├── infrastructure
│   ├── soap
│   ├── async
│   └── scheduler
├── mapper
├── configuration
├── exception
└── util
```

#### Non-Functional Requirements

- Global exception handling
- RFC7807 Problem Details responses
- Validation using Jakarta Validation
- OpenAPI documentation
- Request correlation ID logging
- Structured JSON logging
- Flyway migration scripts
- Dockerfile
- `docker-compose.yml`
- Health endpoint
- Actuator
- Configuration properties pattern
- Environment-specific configuration
- Pagination support
- Sorting support

#### Testing Requirements

Generate:

- Unit tests
- Repository tests
- Service tests
- Controller tests
- Integration tests
- Testcontainers for PostgreSQL

Minimum 80% code coverage.

#### Deliverables

Generate:

1. Complete Maven project structure.
2. `pom.xml`.
3. Domain model.
4. REST controllers.
5. Services.
6. Repositories.
7. SOAP clients.
8. Async processing worker.
9. Database migrations.
10. DTOs.
11. Mappers.
12. Configuration classes.
13. Tests.
14. Docker support.
15. `README.md`.
16. Sequence diagram (Mermaid).
17. Component diagram (Mermaid).
18. API documentation examples.

Generate production-quality code, not pseudocode. Follow enterprise Spring Boot best practices throughout.
