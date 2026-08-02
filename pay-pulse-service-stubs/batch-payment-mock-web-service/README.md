# Batch Payment Mock Web Service

`batch-payment-mock-web-service` is a Spring-WS SOAP stub that simulates realistic batch payment processing outcomes.

It is intended to be called by `pay-pulse-service-web` (the orchestrator), and it returns transaction-level outcomes (`SUCCESS` / `FAILED`) with retryability and failure reason details.

## Base URL

- Local base URL: `http://localhost:7070`
- SOAP endpoint URL: `http://localhost:7070/ws`

## SOAP Operation

- SOAP Action: Not required by this stub
- Namespace: `http://paypulse.platform.com/soap/batch-payment`
- Request root element: `ProcessBatchPaymentReq`
- Response root element: `ProcessBatchPaymentRpy`

## Start the Stub

```bash
$ cd pay-pulse-service-stubs/batch-payment-mock-web-service/
$ ./mvnw spring-boot:run
```

## Postman Setup

1. Create a new request in Postman.
2. Method: `POST`
3. URL: `http://localhost:7070/ws`
4. Headers:
   - `Content-Type: text/xml; charset=utf-8`
5. Body -> `raw` -> paste the SOAP envelope below.

## Sample SOAP Request (Happy Path)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-SCENARIO-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1500.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-001</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor A</beneficiaryName>
          <beneficiaryIban>DE89370400440532013000</beneficiaryIban>
          <amount>1500.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1001</paymentReference>
        </transaction>
        <transaction>
          <paymentId>PAY-002</paymentId>
          <externalPaymentId>EXT-PAY-002</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor B</beneficiaryName>
          <beneficiaryIban>DE89370400440532013001</beneficiaryIban>
          <amount>2500.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1001</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

## Sample SOAP Response

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Header/>
  <SOAP-ENV:Body>
    <ns3:ProcessBatchPaymentRpy xmlns:ns3="http://paypulse.platform.com/soap/batch-payment">
      <batchId>BATCH-SCENARIO-001</batchId>
      <processedAt>2026-07-25T15:31:44.632552</processedAt>
      <transactions>
        <transaction>
          <externalPaymentId>EXT-PAY-001</externalPaymentId>
          <outcome>SUCCESS</outcome>
          <retryable>false</retryable>
          <processedAt>2026-07-25T15:31:44.632873169</processedAt>
        </transaction>
        <transaction>
          <externalPaymentId>EXT-PAY-002</externalPaymentId>
          <outcome>SUCCESS</outcome>
          <retryable>false</retryable>
          <processedAt>2026-07-25T15:31:44.633408237</processedAt>
        </transaction>
      </transactions>
    </ns3:ProcessBatchPaymentRpy>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```


## Complete Scenario Trigger Matrix

This section documents every implemented scenario in evaluation order, with exact trigger fields and expected SOAP outcome.

### Evaluation Order and Precedence

The stub evaluates each transaction in this order:

1. Batch-level outage
2. Duplicate `externalPaymentId` inside same request
3. Amount validity
4. Currency support
5. Payment method amount limit
6. IBAN validation
7. Beneficiary account eligibility
8. Explicit timeout/retry reference markers
9. Manual compliance review threshold
10. Weekend clearing delay probability
11. Risk-score based outcome
12. Success fallback

If a condition matches earlier in the list, later rules are not evaluated for that transaction.

### Batch-Level Scenarios

| Scenario | Where to set | Trigger condition | Outcome | Retryable | Failure reason |
|---|---|---|---|---|---|
| Full downstream outage | `merchantId` or `batchId` | Contains `SOAP_DOWN` or `OUTAGE` (case-insensitive) | All transactions `FAILED` | `true` | `Downstream clearing network unavailable` |

### Transaction-Level Scenarios

| Scenario | Field(s) | Trigger condition | Outcome | Retryable | Failure reason |
|---|---|---|---|---|---|
| Duplicate payment in same batch | `externalPaymentId` | Same `externalPaymentId` appears more than once in one request | `FAILED` | `false` | `Duplicate externalPaymentId within batch` |
| Invalid amount | `amount` | Non-numeric, blank, or `<= 0` | `FAILED` | `false` | `Invalid amount` |
| Unsupported currency | `currency` | Not equal to `EUR` (case-insensitive) | `FAILED` | `false` | `Unsupported currency` |
| Payment method limit exceeded | `paymentMethod`, `amount` | `paymentMethod != SEPA` and `amount > 5000` | `FAILED` | `false` | `Payment method limit exceeded` |
| IBAN invalid by keyword | `beneficiaryIban` | Contains `INVALID` (case-insensitive) | `FAILED` | `false` | `IBAN validation failed` |
| IBAN invalid by suffix | `beneficiaryIban` | Ends with `0000` | `FAILED` | `false` | `IBAN validation failed` |
| Beneficiary ineligible account | `paymentReference` | Contains one of: `ACCOUNT_CLOSED`, `FROZEN`, `BLACKLIST` (case-insensitive) | `FAILED` | `false` | `Beneficiary account not eligible` |
| Explicit temporary upstream timeout | `paymentReference` | Contains one of: `TIMEOUT`, `RETRY`, `UPSTREAM_TEMP` (case-insensitive) | `FAILED` | `true` | `Temporary upstream timeout` |
| Manual compliance review | `amount` | `amount >= 10000` | `FAILED` | `true` | `Manual compliance review pending` |
| Weekend clearing delay | `executionDate` | Date is Saturday/Sunday, plus probability check (~35%) | `FAILED` | `true` | `Clearing house closed for non-urgent weekend processing` |
| Risk engine deferred | `beneficiaryName`, `paymentReference`, seeded randomness | Computed risk score in `[65..84]` | `FAILED` | `true` | `Risk engine deferred for additional checks` |
| Fraud threshold exceeded | `beneficiaryName`, `paymentReference`, seeded randomness | Computed risk score in `[85..100]` | `FAILED` | `false` | `Fraud risk threshold exceeded` |
| Successful processing | Any | No rule above matched | `SUCCESS` | `false` | _none_ |

### Risk Score Inputs (for realistic mixed outcomes)

- Base score uses deterministic seeded randomness per batch (`batchId`, `merchantId`).
- Additional score boosts:
  - Single-word `beneficiaryName`: `+8`
  - `paymentReference` contains one of `URGENT`, `CASH`, `CRYPTO`, `GIFT`: `+20`

### Determinism Notes

- Same batch identifiers (`batchId`, `merchantId`) produce reproducible random branches.
- Timestamp fields (`processedAt`) still change per invocation.

### Scenario Forcing Cheat Sheet (Postman)

Use these quick overrides to force outcomes:

- **Force full outage for all transactions**
  - `merchantId = MERCHANT-SOAP_DOWN`
  - or `batchId = BATCH-SOAP_DOWN-001`
- **Force duplicate scenario**
  - Send two `<transaction>` nodes with same `<externalPaymentId>`
- **Force invalid amount**
  - `amount = 0` or `amount = -5` or `amount = ABC`
- **Force unsupported currency**
  - `currency = USD`
- **Force payment method limit**
  - `paymentMethod = CARD` and `amount = 6000`
- **Force IBAN invalid**
  - `beneficiaryIban = INVALID-IBAN`
  - or `beneficiaryIban = DE89370400440532010000`
- **Force non-retryable account block**
  - `paymentReference = ACCOUNT_CLOSED`
- **Force retryable timeout**
  - `paymentReference = UPSTREAM_TEMP`
- **Force compliance review**
  - `amount = 10000`
- **Increase weekend delay chance**
  - `executionDate` set to Saturday/Sunday

### Scenario Payloads (Copy-Paste XML)

Use each payload as Postman raw XML body (`POST http://localhost:7070/ws`, `Content-Type: text/xml; charset=utf-8`).

#### Template

Start with this shell and replace only marked fields for each scenario:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-SCENARIO-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1500.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-001</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor A</beneficiaryName>
          <beneficiaryIban>DE89370400440532013000</beneficiaryIban>
          <amount>1500.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1001</paymentReference>
        </transaction>
        <transaction>
          <paymentId>PAY-002</paymentId>
          <externalPaymentId>EXT-PAY-002</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor B</beneficiaryName>
          <beneficiaryIban>DE89370400440532013001</beneficiaryIban>
          <amount>2500.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1001</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

#### 1) Batch outage (all transactions fail, retryable)

Set any one field as below:

```xml
<merchantId>MERCHANT-SOAP_DOWN</merchantId>
```

Alternative triggers:

```xml
<batchId>BATCH-SOAP_DOWN-001</batchId>
```

#### 2) Duplicate external payment id (permanent fail)

Use two transactions with same `externalPaymentId`:

```xml
<transactions>
  <transaction>
    <paymentId>PAY-001</paymentId>
    <externalPaymentId>EXT-DUP-001</externalPaymentId>
    <beneficiaryId>BEN-001</beneficiaryId>
    <beneficiaryName>Vendor A</beneficiaryName>
    <beneficiaryIban>DE89370400440532013000</beneficiaryIban>
    <amount>700.00</amount>
    <currency>EUR</currency>
    <paymentReference>Invoice A</paymentReference>
  </transaction>
  <transaction>
    <paymentId>PAY-002</paymentId>
    <externalPaymentId>EXT-DUP-001</externalPaymentId>
    <beneficiaryId>BEN-002</beneficiaryId>
    <beneficiaryName>Vendor B</beneficiaryName>
    <beneficiaryIban>DE89370400440532013001</beneficiaryIban>
    <amount>800.00</amount>
    <currency>EUR</currency>
    <paymentReference>Invoice B</paymentReference>
  </transaction>
</transactions>
```

#### 3) Invalid amount (permanent fail)

```xml
<amount>0</amount>
```

or

```xml
<amount>ABC</amount>
```

#### 4) Unsupported currency (permanent fail)

```xml
<currency>USD</currency>
```

#### 5) Payment method limit exceeded (permanent fail)

```xml
<paymentMethod>CARD</paymentMethod>
```

and transaction:

```xml
<amount>6000.00</amount>
```

#### 6) Invalid IBAN (permanent fail)

Option A:

```xml
<beneficiaryIban>INVALID-IBAN</beneficiaryIban>
```

Option B:

```xml
<beneficiaryIban>DE89370400440532010000</beneficiaryIban>
```

#### 7) Beneficiary blocked/ineligible (permanent fail)

```xml
<paymentReference>ACCOUNT_CLOSED</paymentReference>
```

Other valid triggers:

```xml
<paymentReference>FROZEN</paymentReference>
```

```xml
<paymentReference>BLACKLIST</paymentReference>
```

#### 8) Temporary timeout (retryable fail)

```xml
<paymentReference>UPSTREAM_TEMP</paymentReference>
```

Other valid triggers:

```xml
<paymentReference>TIMEOUT</paymentReference>
```

```xml
<paymentReference>RETRY</paymentReference>
```

#### 9) Manual compliance review (retryable fail)

```xml
<amount>10000.00</amount>
```

#### 10) Weekend clearing delay (probabilistic retryable fail)

Set weekend date (Saturday/Sunday):

```xml
<executionDate>2026-08-16</executionDate>
```

Note: this is probability-based (~35%), not guaranteed on every call.

#### 11) Risk-engine driven outcomes (probabilistic)

Increase risk likelihood with:

```xml
<beneficiaryName>SingleName</beneficiaryName>
<paymentReference>URGENT CASH</paymentReference>
```

Possible outcomes:
- retryable fail: `Risk engine deferred for additional checks`
- non-retryable fail: `Fraud risk threshold exceeded`
- success

#### 12) Success fallback

Use a clean payload with:
- `currency=EUR`
- valid positive amount under major thresholds
- valid IBAN
- no trigger keywords
- no duplicate `externalPaymentId`

## Appendix: Full Ready-to-Send Request Per Scenario (12 Envelopes)

All requests below are complete SOAP envelopes and can be pasted directly in Postman.

### 1) Full Downstream Outage (Batch-Level)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-OUTAGE-001</batchId>
      <merchantId>MERCHANT-SOAP_DOWN</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1500.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-001</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor A</beneficiaryName>
          <beneficiaryIban>DE89370400440532013000</beneficiaryIban>
          <amount>1500.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1001</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 2) Duplicate `externalPaymentId` In Same Batch

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-DUP-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1500.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-DUP-777</externalPaymentId>
          <beneficiaryId>BEN-001</beneficiaryId>
          <beneficiaryName>Vendor A</beneficiaryName>
          <beneficiaryIban>DE89370400440532013000</beneficiaryIban>
          <amount>700.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice A</paymentReference>
        </transaction>
        <transaction>
          <paymentId>PAY-002</paymentId>
          <externalPaymentId>EXT-DUP-777</externalPaymentId>
          <beneficiaryId>BEN-002</beneficiaryId>
          <beneficiaryName>Vendor B</beneficiaryName>
          <beneficiaryIban>DE89370400440532013001</beneficiaryIban>
          <amount>800.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice B</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 3) Invalid Amount

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-INVALID-AMOUNT-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>0.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-003</externalPaymentId>
          <beneficiaryId>BEN-003</beneficiaryId>
          <beneficiaryName>Vendor C</beneficiaryName>
          <beneficiaryIban>DE89370400440532013003</beneficiaryIban>
          <amount>0</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1003</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 4) Unsupported Currency

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-UNSUPPORTED-CURRENCY-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1200.00</totalAmount>
      <currency>USD</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-004</externalPaymentId>
          <beneficiaryId>BEN-004</beneficiaryId>
          <beneficiaryName>Vendor D</beneficiaryName>
          <beneficiaryIban>DE89370400440532013004</beneficiaryIban>
          <amount>1200.00</amount>
          <currency>USD</currency>
          <paymentReference>Invoice INV-1004</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 5) Payment Method Limit Exceeded

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-METHOD-LIMIT-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>CARD</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>6000.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-005</externalPaymentId>
          <beneficiaryId>BEN-005</beneficiaryId>
          <beneficiaryName>Vendor E</beneficiaryName>
          <beneficiaryIban>DE89370400440532013005</beneficiaryIban>
          <amount>6000.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1005</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 6) Invalid IBAN

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-INVALID-IBAN-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>900.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-006</externalPaymentId>
          <beneficiaryId>BEN-006</beneficiaryId>
          <beneficiaryName>Vendor F</beneficiaryName>
          <beneficiaryIban>INVALID-IBAN</beneficiaryIban>
          <amount>900.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1006</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 7) Beneficiary Blocked/Ineligible Account

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-ACCOUNT-BLOCK-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>800.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-007</externalPaymentId>
          <beneficiaryId>BEN-007</beneficiaryId>
          <beneficiaryName>Vendor G</beneficiaryName>
          <beneficiaryIban>DE89370400440532013007</beneficiaryIban>
          <amount>800.00</amount>
          <currency>EUR</currency>
          <paymentReference>ACCOUNT_CLOSED</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 8) Temporary Upstream Timeout (Retryable)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-TIMEOUT-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>850.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-008</externalPaymentId>
          <beneficiaryId>BEN-008</beneficiaryId>
          <beneficiaryName>Vendor H</beneficiaryName>
          <beneficiaryIban>DE89370400440532013008</beneficiaryIban>
          <amount>850.00</amount>
          <currency>EUR</currency>
          <paymentReference>UPSTREAM_TEMP</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 9) Manual Compliance Review (Retryable)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-COMPLIANCE-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>10000.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-009</externalPaymentId>
          <beneficiaryId>BEN-009</beneficiaryId>
          <beneficiaryName>Vendor I</beneficiaryName>
          <beneficiaryIban>DE89370400440532013009</beneficiaryIban>
          <amount>10000.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1009</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 10) Weekend Clearing Delay (Probabilistic)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-WEEKEND-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-16</executionDate>
      <totalAmount>1200.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-010</externalPaymentId>
          <beneficiaryId>BEN-010</beneficiaryId>
          <beneficiaryName>Vendor J</beneficiaryName>
          <beneficiaryIban>DE89370400440532013010</beneficiaryIban>
          <amount>1200.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1010</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 11) Risk Engine Scenario (Probabilistic)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-RISK-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-17</executionDate>
      <totalAmount>1500.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-011</externalPaymentId>
          <beneficiaryId>BEN-011</beneficiaryId>
          <beneficiaryName>SingleName</beneficiaryName>
          <beneficiaryIban>DE89370400440532013011</beneficiaryIban>
          <amount>1500.00</amount>
          <currency>EUR</currency>
          <paymentReference>URGENT CASH</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

### 12) Success Fallback (Clean Path)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:bat="http://paypulse.platform.com/soap/batch-payment">
  <soapenv:Header/>
  <soapenv:Body>
    <bat:ProcessBatchPaymentReq>
      <batchId>BATCH-SUCCESS-001</batchId>
      <merchantId>MERCHANT-100</merchantId>
      <customerId>CUSTOMER-200</customerId>
      <paymentMethod>SEPA</paymentMethod>
      <executionDate>2026-08-18</executionDate>
      <totalAmount>1400.00</totalAmount>
      <currency>EUR</currency>
      <requestedBy>user@merchant.com</requestedBy>
      <transactions>
        <transaction>
          <paymentId>PAY-001</paymentId>
          <externalPaymentId>EXT-PAY-012</externalPaymentId>
          <beneficiaryId>BEN-012</beneficiaryId>
          <beneficiaryName>Vendor L</beneficiaryName>
          <beneficiaryIban>DE89370400440532013012</beneficiaryIban>
          <amount>1400.00</amount>
          <currency>EUR</currency>
          <paymentReference>Invoice INV-1012</paymentReference>
        </transaction>
      </transactions>
    </bat:ProcessBatchPaymentReq>
  </soapenv:Body>
</soapenv:Envelope>
```

## Integration Expectation with `pay-pulse-service-web`

The web app maps:
- `ProcessBatchPaymentReq` from batch + transaction entities
- `ProcessBatchPaymentRpy` back into internal SOAP outcome DTOs

Those mapped outcomes are then used to update:
- transaction statuses (`COMPLETED` / `FAILED`)
- retryability and failure reasons
- batch aggregate status/metrics

## Troubleshooting

- If you get 404:
  - verify URL is exactly `http://localhost:7070/ws`
- If SOAP parse fails:
  - ensure request root is `ProcessBatchPaymentReq` in namespace `http://paypulse.platform.com/soap/batch-payment`
- If no responses in orchestrator:
  - confirm `pay-pulse-service-web` has
    - `paypulse.soap.batch-payment.uri: http://localhost:7070/ws`
