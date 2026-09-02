# Deploying

Backend on Render, frontend on Vercel, database on Supabase. Everything below is on the free tier.

The frontend never calls Render directly. `vercel.json` proxies `/api` and `/uploads` through to
the backend, so the browser only ever sees one origin: no CORS setup, and no API URL baked into
the built JavaScript.

---

## 1. Database - Supabase

1. Create a project and choose a database password. Keep it, it is not shown again.
2. Open the project's database connection settings and copy the **pooler** connection string,
   session mode, port `5432`.

**Use the pooler, not the direct connection.** Supabase gives new projects an IPv6 only direct
address, and Render cannot reach IPv6. The pooler is reachable over IPv4.

The pieces you need:

| Value         | Looks like                                                            |
|---------------|-----------------------------------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>`  (not plain `postgres`)                       |
| `DB_PASSWORD` | the password from step 1                                               |

No tables to create. Hibernate builds the schema on the first start.

---

## 2. Backend - Render

New Web Service, connect the repository, then:

| Setting        | Value    |
|----------------|----------|
| Branch         | `deploy` |
| Root Directory | `backend` |
| Runtime        | Docker   |
| Instance type  | Free     |

Environment variables:

| Name                  | Value                                          |
|-----------------------|------------------------------------------------|
| `DB_URL`              | from step 1                                    |
| `DB_USERNAME`         | from step 1                                    |
| `DB_PASSWORD`         | from step 1                                    |
| `JWT_SECRET`          | any private sentence, 32 characters or more    |
| `ADMIN_EMAIL`         | your email                                     |
| `ADMIN_PASSWORD`      | the password you want for the admin account    |
| `ADMIN_SEED_ENABLED`  | `true` for the first deploy only               |
| `FRONTEND_URL`        | the Vercel URL, once you have it               |

Do not set `PORT`. Render sets it, and the app reads it.

The first build takes a few minutes. When it finishes, note the service URL,
`https://something.onrender.com`.

**After the first successful start, set `ADMIN_SEED_ENABLED` to `false`.** The seeder only runs
while the user table is empty, so leaving it on achieves nothing and only risks surprises.

---

## 3. Frontend - Vercel

Import the same repository:

| Setting        | Value      |
|----------------|------------|
| Branch         | `deploy`   |
| Root Directory | `frontend` |

Build command and output directory come from `vercel.json`, leave them alone.

Then put the Render URL into `frontend/vercel.json`, replacing both
`REPLACE-WITH-YOUR-RENDER-URL.onrender.com` placeholders, and push. Vercel redeploys by itself.

Finally set `FRONTEND_URL` on Render to the Vercel URL, so the links inside account emails point
back at the right place.

---

## 4. Email

Without SMTP the account links are only written to the Render log, which means nobody can finish
signing up. To send them for real, add these on Render:

| Name                    | Value for Gmail                    |
|-------------------------|------------------------------------|
| `SPRING_MAIL_HOST`      | `smtp.gmail.com`                   |
| `SPRING_MAIL_PORT`      | `587`                              |
| `SPRING_MAIL_USERNAME`  | your Gmail address                 |
| `SPRING_MAIL_PASSWORD`  | a Google **app password**, not your normal one |
| `MAIL_FROM`             | the same Gmail address             |

An app password needs two step verification switched on in the Google account.

---

## 5. What the free tier costs you

- **Render sleeps after 15 minutes of no traffic.** The next visitor waits roughly a minute for
  the container to wake. Nothing is lost, it is just slow.
- **Supabase pauses a project after a week of no activity** and has to be resumed by hand.
- The database is separate from the one at home, so the two hold different content.
