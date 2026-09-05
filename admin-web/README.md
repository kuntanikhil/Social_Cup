# Social Cup Admin

Minimum operational admin client for cafes, drinks, barista PINs, recent redemptions, and recent payout visibility.

Login uses `POST /api/auth/login`, then verifies the account through `GET /api/profile`. Only a profile with `role: "ADMIN"` is allowed into protected routes.

## Run locally

1. Set `.env`:

   ```env
   VITE_API_URL=http://localhost:8080
   ```

2. Start Spring Boot on port `8080`.
3. Start the admin client:

   ```bash
   npm install
   npm run dev
   ```

4. Open `http://localhost:5173` (or the port Vite prints if `5173` is busy).

Vite proxies browser `/api` requests to `VITE_API_URL` in development. A production deployment should use a same-origin reverse proxy or an explicit backend CORS policy.

## Checks

```bash
npm run lint
npm run build
```
