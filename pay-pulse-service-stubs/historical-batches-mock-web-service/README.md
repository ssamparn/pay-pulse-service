# Historical Batches Mock Web Service

`historical-batches-mock-web-service` is a Spring-WS SOAP stub that simulates production-like historical batch retrieval.

It generates deterministic historical batches with realistic status mix, pagination, and optional transaction-level details.

## Base URL

- Local base URL: `http://localhost:7071`
- SOAP endpoint URL: `http://localhost:7071/ws`

## SOAP Contract

- Namespace: `http://paypulse.platform.com/soap/historical-batches`
- Request root: `RetrieveHistoricalBatchesReq`
- Response root: `RetrieveHistoricalBatchesRpy`

## Start the Stub

```bash
cd pay-pulse-service-stubs/historical-batches-mock-web-service
./mvnw spring-boot:run
```

## Request Envelope (default realistic data)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:hist="http://paypulse.platform.com/soap/historical-batches">
  <soapenv:Header/>
  <soapenv:Body>
    <hist:RetrieveHistoricalBatchesReq>
      <period>LAST_3_MONTHS</period>
      <merchantId>MERCHANT-12345</merchantId>
      <customerId>CUSTOMER-67890</customerId>
      <page>1</page>
      <pageSize>20</pageSize>
      <includeTransactions>false</includeTransactions>
    </hist:RetrieveHistoricalBatchesReq>
  </soapenv:Body>
</soapenv:Envelope>
```

## Scenario Trigger Matrix

| Scenario | Trigger field | Trigger value | Behavior |
|---|---|---|---|
| Default realistic feed | `merchantId` | any normal value | Mixed `COMPLETED`, `PARTIALLY_COMPLETED`, `FAILED`, `PROCESSING`, `PENDING` batches |
| Empty historical result | `merchantId` | contains `HIST_EMPTY` | Returns zero batches |
| Volume spike | `merchantId` | contains `HIST_SPIKE` | Returns much higher number of batches for same range |
| Large enterprise batches | `merchantId` | contains `HIST_LARGE` | Returns high transaction counts per batch |
| Partial-heavy estate | `merchantId` | contains `HIST_PARTIAL_HEAVY` | Raises probability of `PARTIALLY_COMPLETED` |
| Stale replicated feed | `merchantId` | contains `HIST_STALE` | Sets `staleAsOf` and marks batch items as stale |
| DR source marker | `merchantId` | contains `HIST_DR` | `sourceSystem=HISTORICAL_DR_REGION` |
| Downstream outage | `merchantId` | contains `HIST_OUTAGE` | Throws service error (SOAP fault path) |

## Example Response Shape

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header/>
  <SOAP-ENV:Body>
    <ns3:RetrieveHistoricalBatchesRpy xmlns:ns3="http://paypulse.platform.com/soap/historical-batches">
      <requestId>HIST-20260501-20260731-4b0a7f9f</requestId>
      <generatedAt>2026-07-31T11:20:14.877717</generatedAt>
      <fromDate>2026-05-01</fromDate>
      <toDate>2026-07-31</toDate>
      <currentPage>1</currentPage>
      <pageSize>20</pageSize>
      <totalPages>9</totalPages>
      <totalBatches>177</totalBatches>
      <sourceSystem>HISTORICAL_PRIMARY_REGION</sourceSystem>
      <batches>
        <batch>
          <batchId>BP-20260731-00001</batchId>
          <externalBatchId>EXT-HIST-20260731-0001</externalBatchId>
          <merchantId>MERCHANT-12345</merchantId>
          <customerId>CUSTOMER-67890</customerId>
          <status>PARTIALLY_COMPLETED</status>
          <totalAmount>12690.00</totalAmount>
          <currency>EUR</currency>
          <paymentMethod>SEPA</paymentMethod>
          <paymentCount>22</paymentCount>
          <successfulPayments>20</successfulPayments>
          <failedPayments>2</failedPayments>
          <pendingPayments>0</pendingPayments>
          <progressPercentage>91</progressPercentage>
          <createdAt>2026-07-31T09:13:11</createdAt>
          <completedAt>2026-07-31T10:04:11</completedAt>
          <lastUpdatedAt>2026-07-31T10:07:11</lastUpdatedAt>
          <lastErrorMessage>Upstream bank timeout</lastErrorMessage>
        </batch>
      </batches>
    </ns3:RetrieveHistoricalBatchesRpy>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

## Postman Quick Setup

- Method: `POST`
- URL: `http://localhost:7071/ws`
- Header: `Content-Type: text/xml; charset=utf-8`
- Body: raw XML (use the request envelope above)

