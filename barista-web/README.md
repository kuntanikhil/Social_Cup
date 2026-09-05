# Social Cup Barista Check-In

Lightweight cafe-device interface for validating Social Cup member redemptions.

## Run locally

1. Copy `.env.example` to `.env` and set the Spring Boot URL:

   ```env
   VITE_API_URL=http://localhost:8080
   ```

2. Start the backend on port `8080`.
3. Install and run the web app:

   ```bash
   npm install
   npm run dev
   ```

4. Open `http://localhost:5173/cafe/1`.

During development Vite proxies same-origin `/api` requests to `VITE_API_URL`, avoiding cross-origin browser failures without changing the backend. A production deployment should place the web app and API behind a same-origin reverse proxy, or add an explicit backend CORS policy later.

Camera scanning requires browser camera permission and a secure context. `localhost` is accepted by modern browsers; other HTTP addresses may not be. The six-digit backup-code flow remains available in every case.

## Checks

```bash
npm run lint
npm run build
```
