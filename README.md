# M-Pesa Payment System

A RESTful payment system inspired by Kenya's M-Pesa mobile money platform. Built with **Spring Boot 3** and **Java 17**.

## Features

- **User Registration** — Create accounts with a custom 4-digit PIN
- **PIN Authentication** — Secure login with account lockout after 3 failed attempts (5-min cooldown)
- **JWT Tokens** — Stateless authentication for all protected endpoints
- **Deposit** — Add funds to your account
- **Withdraw** — Remove funds with automatic fee calculation
- **Transfer** — Send money to other users with fee deduction
- **Change PIN** — Secure PIN update with current PIN verification
- **Step-up Auth** — Sensitive operations (withdraw, transfer, change PIN) require PIN re-verification even with a valid JWT
- **Rate Limiting** — 60 requests/minute per IP address

## Tech Stack

| Category | Technology |
|---|---|
| Framework | Spring Boot 3.4.13 |
| Language | Java 17 |
| Build | Maven |
| Database | MariaDB 11.4 |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT (jjwt 0.12.5) + BCrypt |
| Migrations | Flyway |
| Testing | JUnit 5, H2 (in-memory test DB) |
| Containerization | Docker + Docker Compose |

## Quick Start

### Prerequisites
- Java 17+
- Maven (or use included `mvnw` wrapper)
- MariaDB 11.4+ (or Docker)

### 1. Set Environment Variables
```bash
export JWT_SECRET=$(openssl rand -base64 32)
```

### 2. Start Database
```bash
docker-compose up -d db
```

### 3. Build & Run
```bash
./mvnw clean package
java -jar target/payment-system-1.0-SNAPSHOT.jar
```

Or use Docker Compose for everything:
```bash
JWT_SECRET=$(openssl rand -base64 32) docker-compose up --build
```

## API Endpoints

### Public (No Auth)
| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/api/register` | `{username, pin, balance?}` | Register a new user |
| `POST` | `/api/login` | `{userId, pin}` | Login and get JWT token |

### Protected (JWT Required)
| Method | Path | Body | Description |
|---|---|---|---|
| `GET` | `/api/user/{id}` | — | Get user details |
| `GET` | `/api/balance` | — | Get authenticated user's balance |
| `GET` | `/api/transactions` | — | Get authenticated user's transaction history |
| `POST` | `/api/payment/deposit` | `{amount}` | Deposit funds |
| `POST` | `/api/payment/withdraw` | `{amount, pin}` | Withdraw funds (PIN required) |
| `POST` | `/api/payment/transfer` | `{toUserId, amount, pin}` | Transfer to another user (PIN required) |
| `POST` | `/api/change-pin` | `{userId, currentPin, newPin}` | Change PIN (user must match JWT subject) |

### Auth Header
All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```

## Withdrawal Fees

| Amount Range (KES) | Fee (KES) |
|---|---|
| ≤ 100 | 0 |
| ≤ 500 | 13 |
| ≤ 1,000 | 25 |
| > 1,000 | 30 |

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

# Deposit (requires JWT)
curl -X POST http://localhost:8080/api/payment/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"amount": 500}'

# Withdraw (requires PIN)
curl -X POST http://localhost:8080/api/payment/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"amount": 200, "pin": "1234"}'

# Transfer (requires PIN)
curl -X POST http://localhost:8080/api/payment/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"toUserId": 2, "amount": 100, "pin": "1234"}'
```

## Architecture

```
┌─────────┐     ┌──────────────┐     ┌─────────────┐     ┌────────────┐     ┌─────────┐
│ Client  │────▶│ REST API     │────▶│ Controller  │────▶│ Service     │────▶│Repository│
│         │     │ (Spring Sec) │     │             │     │ (Business)  │     │ (JPA)    │
└─────────┘     └──────────────┘     └─────────────┘     └─────────────┘     └─────────┘
                      │                                                          │
                      ▼                                                          ▼
                JWT Filter                                                   MariaDB
              Rate Limiter                                                 (Docker Compose)
                /error
            (Blocked)
```

## Security

- **JWT** — 1-hour token expiry, enforced secret via environment variable
- **BCrypt** — PIN hashing (12 rounds)
- **Step-up Auth** — JWT alone is not enough for sensitive operations; PIN re-verification required
- **Account Lockout** — 3 wrong PIN attempts triggers a 5-minute lock
- **BigDecimal** — All monetary values use `BigDecimal` to prevent floating-point precision bugs
- **CORS** — Configured (restrict in production)
- **Error Sanitization** — No stack traces or internal details exposed to clients

## Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database with Flyway disabled.

## Project Structure

```
src/main/java/com/mpesa/
├── controller/          # REST endpoints
├── config/              # Security configuration
├── dto/                 # Request/response objects
├── exception/           # Custom exceptions
├── model/               # JPA entities
├── repository/          # Data access layer
├── security/            # JWT service, auth filter, rate limiting
└── service/             # Business logic

src/main/resources/
├── application.properties
└── db/migration/        # Flyway SQL migrations
```

## License

MIT
