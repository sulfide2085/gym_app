# Gym App Cloudflare Worker

This Worker stores the Android app's workout state in Cloudflare D1.

## Setup

1. Install dependencies:

   ```powershell
   npm install
   ```

2. Log in and create the D1 database:

   ```powershell
   npx wrangler login
   npx wrangler d1 create gym_app_db
   ```

3. Copy the returned `database_id` into `wrangler.jsonc`.

4. Apply the migration and deploy:

   ```powershell
   npm run db:migrate:remote
   npm run deploy
   ```

5. Put the deployed Worker URL into `CloudflareConfig.API_BASE_URL` in the Android app.

Optional: set `APP_SYNC_TOKEN` as a Worker secret and put the same value into the Android config for a personal build:

```powershell
npx wrangler secret put APP_SYNC_TOKEN
```
