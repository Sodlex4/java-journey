# M-Pesa Payment System

A RESTful payment system inspired by Kenya's M-Pesa mobile money platform. Built with **Spring Boot 3.4** and **Java 17**.

## Features

- **User Registration** — Create accounts with a 4-digit PIN
- **PIN Authentication** — Secure login with account lockout after 3 failed attempts (5-min cooldown)
- **JWT Tokens** — Stateless 1-hour expiry, step-up PIN re-verification for sensitive operations
- **Deposit / Withdraw / Transfer** — Full payment operations with automatic fee deduction
- **Idempotent Payments** — `Idempotency-Key` header prevents duplicate processing on retry
- **Fee Collection** — Withdrawal/transfer fees deposited into SAFARICOM system account
- **Rate Limiting** — DB-backed (works across multiple instances), configurable limit per IP
- **Paginated Transactions** — `GET /api/transactions?page=0&size=20`
- **Monitoring** — Actuator health + Prometheus metrics endpoints
- **Structured Logging** — JSON logs to stdout, ready for ELK/Loki/Datadog
- **Docker** — Multi-stage build with Docker Compose

## Tech Stack

| Category | Technology |
|---|---|
| Framework | Spring Boot 3.4.13 |
| Language | Java 17 |
| Build | Maven (wrapper included) |
| Database | MariaDB 11.4 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Security | Spring Security + jjwt 0.12.5 + BCrypt (12 rounds) |
| Monitoring | Micrometer + Prometheus |
| Logging | Logstash Logback (JSON) |
| Testing | JUnit 5, H2 in-memory DB |
| Containerization | Docker + Docker Compose |

## Quick Start

### Prerequisites
- Java 17+
- Docker + Docker Compose (or MariaDB 11.4+)

### 1. Clone and configure
```bash
cp .env.example .env   # edit with your values
export JWT_SECRET=$(openssl rand -base64 32)
```

### 2. Start everything
```bash
JWT_SECRET=$JWT_SECRET docker-compose up --build
```

Or run locally:
```bash
docker-compose up -d db
./mvnw clean package
java -jar target/payment-system-1.0-SNAPSHOT.jar
```

### 3. Verify it's alive
```bash
curl http://localhost:8080/actuator/health
```

## Environment Variables

| Variable | Default | Required | Description |
|---|---|---|---|
| `JWT_SECRET` | — | **Yes** | Base64-encoded HMAC key (generate: `openssl rand -base64 32`) |
| `DB_USER` | `root` | No | Database user |
| `DB_PASSWORD` | `root` | No | Database password |
| `DB_URL` | `jdbc:mariadb://localhost:3306/mpesa_db` | No | JDBC connection URL |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | No | Comma-separated allowed origins |
| `RATE_LIMIT_MAX` | `60` | No | Max requests per minute per IP |
| `FEE_TIER_1` | `100` | No | First fee tier threshold (KES) |
| `FEE_TIER_2` | `500` | No | Second fee tier threshold (KES) |
| `FEE_TIER_3` | `1000` | No | Third fee tier threshold (KES) |
| `FEE_RATE_1` | `0` | No | Fee for amounts ≤ tier 1 (KES) |
| `FEE_RATE_2` | `13` | No | Fee for amounts ≤ tier 2 (KES) |
| `FEE_RATE_3` | `25` | No | Fee for amounts ≤ tier 3 (KES) |
| `FEE_RATE_4` | `30` | No | Fee for amounts > tier 3 (KES) |

## API Endpoints

### Public (No Auth)
| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/api/register` | `{username, pin, balance?}` | Register a new user |
| `POST` | `/api/login` | `{userId, pin}` | Login and get JWT token |

### Protected (JWT Required)
| Method | Path | Body / Params | Description |
|---|---|---|---|
| `GET` | `/api/user/{id}` | — | Get user details |
| `GET` | `/api/balance` | — | Get authenticated user's balance |
| `GET` | `/api/transactions` | `?page=0&size=20` | Paginated transaction history |
| `POST` | `/api/payment/deposit` | `{amount}` | Deposit funds |
| `POST` | `/api/payment/withdraw` | `{amount, pin}` | Withdraw funds (PIN required) |
| `POST` | `/api/payment/transfer` | `{toUserId, amount, pin}` | Transfer to another user |
| `POST` | `/api/change-pin` | `{userId, currentPin, newPin}` | Change PIN |

### Monitoring
| Method | Path | Description |
|---|---|---|
| `GET` | `/actuator/health` | Health check (no auth) |
| `GET` | `/actuator/prometheus` | Prometheus metrics (no auth) |

### Auth Header
```
Authorization: Bearer <jwt_token>
```

### Idempotency
All `POST /api/payment/*` endpoints support the `Idempotency-Key` header:
```bash
curl -X POST /api/payment/deposit \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: a1b2c3d4-e5f6-..." \
  -d '{"amount": 500}'
```
Retrying with the same key returns the cached 2xx response — no duplicate processing.

## Fee Schedule

| Amount Range (KES) | Fee (KES) |
|---|---|
| ≤ 100 | 0 |
| ≤ 500 | 13 |
| ≤ 1,000 | 25 |
| > 1,000 | 30 |

Tiers are configurable via `FEE_TIER_*` and `FEE_RATE_*` env vars.

## Transaction Limits
- **Maximum**: KES 500,000 per transaction
- **Minimum**: KES 0.01 per transaction

## Response Format

### Success
```json
{
  "success": true,
  "message": "Operation completed",
  "data": {},
  "timestamp": "2026-05-05T14:30:00Z"
}
```

### Error
```json
{
  "success": false,
  "error": "Insufficient funds",
  "errorCode": "INSUFFICIENT_FUNDS",
  "correlationId": "a1b2c3d4-e5f6-7890",
  "timestamp": "2026-05-05T14:30:00Z"
}
```

### Paginated Transactions
```json
{
  "transactions": [...],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

## Example Usage

```bash
# Register
curl -X POST http://localhost:8080/api/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "pin": "1234", "balance": 1000}'

# Login
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "pin": "1234"}'

# Deposit (with idempotency)
curl -X POST http://localhost:8080/api/payment/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount": 500}'

# Withdraw (PIN required)
curl -X POST http://localhost:8080/api/payment/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"amount": 200, "pin": "1234"}'

# Transfer (PIN required)
curl -X POST http://localhost:8080/api/payment/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"toUserId": 2, "amount": 100, "pin": "1234"}'

# Paginated transactions
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/transactions?page=0&size=10"
```

## Security

- **JWT** — 1-hour token expiry, HMAC-SHA signing via configured `JWT_SECRET`
- **BCrypt** — PIN hashing (12 rounds)
- **Step-up Auth** — JWT alone is not enough; withdraw/transfer/change-pin require PIN re-entry
- **Account Lockout** — 3 failed attempts → 5-minute lock, auto-unlock after cooldown
- **Idempotency** — Prevents duplicate payment processing on network retries
- **Rate Limiting** — DB-backed (multi-instance safe), configurable via `RATE_LIMIT_MAX`
- **CORS** — Configurable via `CORS_ALLOWED_ORIGINS` env var
- **Error Sanitization** — No stack traces or internal details; correlation IDs on all errors
- **BigDecimal** — All monetary values use `BigDecimal(15,2)` — no floating-point bugs
- **401 vs 403** — Missing/invalid JWT returns 401 (not 403)

## Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database with Flyway disabled.

## Project Structure

```
src/main/java/com/mpesa/
├── config/              # Security config, FeeProperties, CORS
├── controller/          # REST endpoints + GlobalExceptionHandler
├── dto/                 # Request/response DTOs, ApiResponse, ErrorCode
├── exception/           # PaymentException
├── model/               # JPA entities (User, Transaction, SystemAccount, etc.)
├── repository/          # Spring Data JPA repositories
├── security/            # JWT service, auth filter, rate limit filter, idempotency filter
└── service/             # Business logic (UserService, RateLimitService)

src/main/resources/
├── application.properties
├── logback-spring.xml   # JSON structured logging
└── db/migration/        # Flyway SQL migrations (V1–V5)
```

## License

MIT
