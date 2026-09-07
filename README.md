# Social Cup prototype

Social Cup is a demonstrable coffee-membership platform for Dallas. It includes a member mobile app, a cafe barista scanner, an operations admin console, and a Spring Boot modular-monolith API backed by PostgreSQL and Stripe Test Mode.

This repository is a prototype. It demonstrates the main transaction path, but it is not production-ready.

## Architecture

```text
Expo member app ───────┐
Admin React app ───────┼── HTTP/JSON ── Spring Boot API ── Supabase PostgreSQL
Barista React app ─────┘                       │
                                              └── Stripe Test Mode
```

The Spring Boot API is authoritative for membership, credits, redemption state, and payout snapshots. Clients never deduct credits or complete redemptions themselves.

The backend currently lives at the repository root rather than in a nested `backend/` directory:

```text
social-cup-backend/
├── pom.xml                 # Spring Boot backend
├── src/                    # backend source and Flyway migrations
├── mobile/                 # Expo member app
├── barista-web/            # Vite barista scanner
└── admin-web/              # Vite admin console
```

## Implemented prototype flows

- Email/password registration, login, refresh tokens, and JWT authentication
- Profile onboarding, neighbourhood, and coffee preferences
- Cafe discovery, cafe details, menus, ratings, and drink diary
- MEMBER/ADMIN application roles and first-admin bootstrap
- Stripe subscription checkout, signed webhooks, and reconciliation fallback
- A 30-credit, no-rollover billing-cycle reset with an immutable credit ledger
- Five-minute QR and six-digit backup redemption sessions
- Trusted cafe devices, atomic barista validation, replay prevention, and today's list
- Admin cafe/drink/PIN management, redemption history, and payout snapshot summary

## Requirements

- Java 25
- PostgreSQL (Supabase is used for the prototype)
- Node.js and npm
- Expo Go or an Expo-compatible simulator/device
- Stripe CLI for local webhook testing

## Environment variables

Never commit real values. Root and frontend `.env` files are ignored; only placeholder `.env.example` files belong in Git.

### Backend

| Variable | Required | Purpose |
|---|---:|---|
| `DB_URL` | Yes | JDBC PostgreSQL URL, normally with SSL enabled for Supabase |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Base64-encoded signing key containing at least 32 bytes |
| `STRIPE_SECRET_KEY` | Yes | Stripe Test Mode secret key |
| `STRIPE_PRICE_ID` | Yes | Recurring Stripe Price used by checkout |
| `STRIPE_WEBHOOK_SECRET` | For webhooks | Signing secret printed by Stripe CLI or configured on the endpoint |
| `ADMIN_EMAIL` | First-admin bootstrap only | Existing registered user to promote when no ADMIN exists |

See [`.env.example`](.env.example). Spring Boot reads process/IDE environment variables and does not automatically load that file.

Generate a suitable JWT secret locally, for example:

```bash
openssl rand -base64 32
```

Store the result in your local secret manager or run configuration, not in source control.

### Mobile

Create `mobile/.env` from `mobile/.env.example`:

```env
EXPO_PUBLIC_API_URL=http://YOUR_DEVELOPMENT_COMPUTER_LAN_IP:8080
```

A physical phone cannot use `localhost` to reach the development computer. The phone and computer must be on a network where that LAN address and port are reachable.

### Barista Web

Create `barista-web/.env`:

```env
VITE_API_URL=http://localhost:8080
```

### Admin Web

Create `admin-web/.env`:

```env
VITE_API_URL=http://localhost:8080
```

## Local setup

### 1. Backend

Configure all required backend environment variables in the shell or IDE, then run from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Flyway applies migrations from `src/main/resources/db/migration`. Never edit an applied migration; add a new version instead.

Verify:

```text
GET http://localhost:8080/api/health
```

### 2. Mobile

```bash
cd mobile
npm install
npm run typecheck
npm start
```

Scan the Expo QR code with Expo Go or launch a configured simulator.

### 3. Barista Web

```bash
cd barista-web
npm install
npm run dev
```

Open the cafe-specific route printed by Vite, for example:

```text
http://localhost:5173/cafe/1
```

If another frontend already occupies port 5173, use the port Vite prints.

### 4. Admin Web

```bash
cd admin-web
npm install
npm run dev
```

Open the URL printed by Vite. Login is followed by `GET /api/profile`; only `role: "ADMIN"` is accepted by the client, and `/api/admin/**` is independently protected by the backend.

## First administrator

1. Start the backend with `ADMIN_EMAIL` blank.
2. Register the intended administrator through `POST /api/auth/register` as a normal user.
3. Stop the backend.
4. Set `ADMIN_EMAIL` to that registered email in the backend process environment.
5. Restart Spring Boot.
6. Login through the normal `POST /api/auth/login` endpoint and use Admin Web.

Bootstrap promotes only an existing user and only while no ADMIN exists. It never creates an account or password.

## Stripe Test Mode

Create a recurring Stripe Price and configure `STRIPE_SECRET_KEY` and `STRIPE_PRICE_ID` with Test Mode values.

Forward signed local webhooks:

```bash
stripe login
stripe listen --forward-to http://localhost:8080/api/webhooks/stripe
```

Copy the CLI's signing secret into the backend process environment as `STRIPE_WEBHOOK_SECRET`, then restart the backend. Do not commit it.

Checkout is created with an authenticated request and no client-supplied price:

```text
POST http://localhost:8080/api/membership/checkout
Authorization: Bearer <member access token>
```

The response contains the Stripe customer, ephemeral-key, subscription, and payment client-secret values needed by a Payment Sheet client. The repository does not yet include the mobile Payment Sheet UI, so complete a Test Mode payment with a Stripe-supported test client/harness.

After payment, verify:

1. Stripe CLI forwards a successful invoice event.
2. `GET /api/membership` returns `ACTIVE` and exactly 30 credits.
3. `GET /api/credits/transactions` contains the cycle grant and any required expiry entry.
4. If the webhook was missed, call authenticated `POST /api/membership/reconcile`.
5. Call reconciliation again and confirm the same invoice creates no additional billing cycle or grant.

`POST /api/membership/demo-activate` remains a development-only fallback for demonstrating redemption without completing Stripe checkout.

## Main demo sequence

1. Register or login in the mobile app.
2. Complete the profile and coffee preferences.
3. Activate membership through Stripe Test Mode, reconciliation, or the explicitly development-only demo endpoint.
4. Confirm membership shows 30 credits.
5. Discover a cafe, open its detail page, select an active drink, and create a redemption session.
6. Confirm the mobile preview shows the future balance while the server balance remains unchanged.
7. Open Barista Web at `/cafe/{cafeId}`, enter the configured cafe PIN, and scan the QR token or enter the six-digit code.
8. Confirm the first validation succeeds and a second scan is rejected as already used.
9. Confirm the mobile polling screen changes to `REDEEMED` and refreshes the authoritative membership balance.
10. Confirm the redemption appears in the barista Today list.
11. Login to Admin Web as ADMIN and confirm the redemption and payout snapshot appear.
12. Confirm a MEMBER token receives `403` from `/api/admin/**`.

## Verification commands

```powershell
# The external context smoke test runs when DB_URL is configured
.\mvnw.cmd clean test

cd mobile
npm install
npm run typecheck
npx expo-doctor

cd ..\barista-web
npm install
npm run build
npm run lint

cd ..\admin-web
npm install
npm run build
npm run lint
```

## Financial and consistency rules

- `credit_accounts.user_id` is the primary key: one account per user.
- `drink_ratings` has a unique `(user_id, drink_id)` constraint.
- Session creation locks the user and cancels/expires the previous live session.
- Barista validation locks the redemption session and credit account.
- `redemptions.session_id` is unique, preventing two redemptions for one session.
- Successful redemption, ledger deduction, session transition, and payout snapshot commit in one transaction.
- Billing cycles use the Stripe invoice ID as a unique idempotency reference.
- A successful billing cycle expires the remaining balance and resets to 30; it never adds 30.
- Historical payout values are stored on the redemption and are not recalculated from the cafe's current rate.

## Prototype limitations

Do not treat the current system as production-ready. Important remaining work includes:

- Production deployment, TLS, domain, CORS, firewall, and secret-management configuration
- A separate staging database and automated PostgreSQL integration tests
- Production Stripe webhook monitoring, retry operations, and removal/disablement of demo endpoints
- Mobile Stripe Payment Sheet integration
- Rate limiting and brute-force protection for login, PIN, backup-code, and reconciliation endpoints
- Short-lived/rotating cafe-device authorization; the current schema supports revocation but not expiry
- Database-backed concurrent integration tests for redemption and billing idempotency
- Centralized observability, alerting, backups, restore drills, and audit-log completion
- Dependency/security update policy and mobile transitive dependency review
- Privacy policy, terms, data-retention policy, accessibility/device/browser UAT, and app-store readiness

No bank transfers, payout-run automation, advanced RBAC, or production identity system are implemented.
