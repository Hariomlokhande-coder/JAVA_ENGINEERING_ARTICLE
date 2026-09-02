# Technical Blog Platform

A personal technical blog for Java, OOP, Spring Boot, LLD and System Design notes.

Visitors browse a roadmap of sections, open a topic and read an article with syntax highlighted
code, images, an embedded video and links to the source on GitHub. They can tick topics off and
star the ones worth revisiting, with or without an account. A single administrator writes the
content in a visual editor, and the admin area is invisible to everyone else.

**Stack:** Angular 18 + Spring Boot 3.3 + Spring Security/JWT + Spring Data JPA + PostgreSQL
(H2 for local development).

```
PERSONAL_BLOG_PLATFORM/
├── backend/     Spring Boot REST API (69 Java files)
├── frontend/    Angular single page application (51 TypeScript files)
└── docs/        API reference and sample article content
```

---

## 1. What it does

### For a visitor

- **Roadmap home page** — every section with a progress bar, expand all, and a table of topics
  showing status, video link, code link and a difficulty pill
- **Article page** — embedded YouTube video, tags, GitHub link, rendered Markdown with copy
  buttons on code blocks, reading progress bar and previous/next navigation
- **Search** across titles, descriptions and tags
- **Progress tracking without signing up** — ticks are kept in the browser
- **Optional account** — sign up and the same progress follows you on any device
- **Light and dark theme**, light by default

### For the administrator

- Dashboard with article list, filter, inline publish/unpublish toggle and delete
- **Visual article editor** — what you see is the finished article. Paste a screenshot straight
  into the text and it uploads and appears in place; Ctrl+K adds a link at the cursor
- Category management, tags, difficulty, display order, drafts
- The whole admin area is hidden from visitors and enforced server side

---

## 2. Prerequisites

| Tool       | Version                              |
|------------|--------------------------------------|
| JDK        | 17+                                  |
| Maven      | 3.9+                                 |
| Node.js    | 18.19+ or 20+                        |
| PostgreSQL | 14+ (only for the default profile)   |

---

## 3. Running it

### Backend

**Option A — dev profile, nothing to install.** Uses a file based H2 database in `backend/data/`.

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Option B — PostgreSQL, the production setup.** Create the database once:

```sql
CREATE DATABASE technical_blog;
```

```bash
cd backend
mvn spring-boot:run
```

**Option C — PostgreSQL on another machine on the same network.** Copy
`backend/run-local.ps1.example` to `backend/run-local.ps1`, fill in the values and run it.
That file is git ignored, so the password stays off GitHub.

On the machine running PostgreSQL, three things have to be true before it will accept a
connection from another PC:

| Where | Change |
|-------|--------|
| `postgresql.conf` | `listen_addresses = '*'` |
| `pg_hba.conf`     | `host all all <your subnet>/24 scram-sha-256` |
| Windows Firewall  | inbound TCP 5432, restricted to that subnet |

Restart the PostgreSQL service afterwards. `netstat -ano | findstr ":5432"` should then show
`0.0.0.0:5432` rather than only `127.0.0.1`. Give that PC a fixed address in the router, or the
connection breaks the day DHCP hands it a different one.

Either way the API starts on http://localhost:8080. Tables are created by Hibernate on first
start, and the seeder adds the admin account plus the starter categories while the tables are
still empty.

> **On the very first start, set `ADMIN_PASSWORD` yourself.** The committed default is a
> placeholder. The seeder only runs while the user table is empty, so this is your one chance
> to choose the password without touching the database afterwards.
>
> ```bash
> ADMIN_EMAIL=you@example.com ADMIN_PASSWORD='YourStrongPassword1' mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
> ```
>
> In PowerShell: `$env:ADMIN_PASSWORD="YourStrongPassword1"; mvn spring-boot:run "-Dspring-boot.run.profiles=dev"`

### Frontend

```bash
cd frontend
npm install
npm start          # or: npx ng serve --port 4300
```

Opens on http://localhost:4200 (4300 also allowed by the CORS config).

> On Windows PowerShell, the execution policy may block `npm.ps1`. Use `npm.cmd` and `npx.cmd`
> instead, or run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` once.

---

## 4. Configuration

Everything in `backend/src/main/resources/application.yml` can be overridden with an environment
variable.

| Variable            | Default                                             | Purpose                          |
|---------------------|-----------------------------------------------------|----------------------------------|
| `DB_URL`            | `jdbc:postgresql://localhost:5432/technical_blog`   | Database URL                     |
| `DB_USERNAME`       | `postgres`                                          | Database user                    |
| `DB_PASSWORD`       | `postgres`                                          | Database password                |
| `JWT_SECRET`        | development placeholder                             | HS256 key, minimum 32 characters |
| `JWT_EXPIRATION_MS` | `86400000`                                          | Token lifetime                   |
| `CORS_ORIGINS`      | `http://localhost:4200,http://localhost:4300`       | Allowed frontend origins         |
| `UPLOAD_DIR`        | `uploads`                                           | Folder for uploaded images       |
| `ADMIN_EMAIL`       | `admin@example.com`                                 | Seeded admin account             |
| `ADMIN_PASSWORD`    | placeholder                                         | Seeded admin password            |
| `ADMIN_SEED_ENABLED`| `true`                                              | Turn the seeder off in production |

**The application refuses to start outside the dev profile** while `JWT_SECRET` or
`ADMIN_PASSWORD` are still the committed defaults. That check lives in `SecurityStartupCheck`.

---

## 5. API

Base URL `http://localhost:8080/api`. Full reference with request and response bodies:
[docs/API.md](docs/API.md).

| Group      | Endpoints                                                                                          | Access        |
|------------|----------------------------------------------------------------------------------------------------|---------------|
| Auth       | `POST /auth/register`, `POST /auth/login`                                                           | Public        |
|            | `GET /auth/verify-email`, `POST /auth/resend-verification`                                          | Public        |
|            | `POST /auth/forgot-password`, `POST /auth/reset-password`                                           | Public        |
|            | `GET /auth/me`                                                                                      | Authenticated |
| Categories | `GET /categories`, `/categories/roadmap`, `/categories/{id}`, `/categories/slug/{slug}`             | Public        |
|            | `POST`, `PUT`, `DELETE /categories/**`                                                              | ADMIN         |
| Articles   | `GET /articles`, `/articles/search`, `/articles/category/{slug}`, `/articles/slug/{slug}`, `/{id}`  | Public        |
|            | `GET /articles/manage`                                                                              | ADMIN         |
|            | `POST`, `PUT`, `PATCH /{id}/publish`, `DELETE /articles/**`                                         | ADMIN         |
| Tags       | `GET /tags`                                                                                         | Public        |
| Files      | `POST /files/upload`                                                                                | ADMIN         |
| Reader     | `GET /me/progress`, `PUT /me/progress/{articleId}`                                                   | Authenticated |

Drafts are invisible to visitors: article and category reads return published articles only,
unless the caller presents an ADMIN token.

---

## 6. Security

Spring Security is the real protection; the Angular guards only keep the admin screens out of sight.

- **Roles** — `POST /auth/register` always creates a `USER`. Only the seeder creates `ADMIN`, so an
  open sign up form can never hand out administrator rights.
- **Brute force** — five failed logins per email and IP trigger a 15 minute block (HTTP 429).
- **JWT** — every request re-reads the account, so a deleted or demoted admin loses access
  immediately rather than when the token expires.
- **Headers** — CSP, `X-Frame-Options: DENY`, `nosniff`, Referrer-Policy, Permissions-Policy, HSTS.
- **Uploads** — the file signature must match the declared type, so a script cannot be stored
  behind an image name. SVG is rejected because it can carry script on this origin. Stored names
  are fresh UUIDs, which rules out path traversal.
- **Passwords** — BCrypt strength 12. A completed reset ends every session that was opened
  with the old password, and clears any brute force lockout.
- **Account flows** — sign up, email verification and password reset answer the same way for a
  known and an unknown address, so none of them can be used to discover who has an account.
  Verification and reset tokens are stored as SHA-256 hashes, expire, and work once.
- **Content** — Markdown is sanitized with DOMPurify before it is rendered.

---

## 7. Project layout

### Backend — `com.technicalblog`

```
config/       SecurityConfig, WebConfig, DataSeeder, SecurityStartupCheck, *Properties
controller/   Auth, Article, Category, Tag, File, Me
dto/          request/ and response/ records
entity/       User, Role, Category, Article, Tag, Difficulty, ArticleProgress
repository/   Spring Data interfaces
service/      Auth, Article, Category, Tag, FileStorage, Progress
mapper/       entity to DTO conversion
security/     JwtService, JwtAuthenticationFilter, CustomUserDetailsService,
              LoginAttemptService, entry point, access denied handler
exception/    typed exceptions + GlobalExceptionHandler
util/         SlugUtils, TextUtils
```

### Frontend — `src/app`

```
core/services/    auth, article, category, tag, file, progress, progress-api,
                  markdown, markdown-html, theme, confirm, prompt
core/guards/      admin, login-redirect, unsaved-changes
core/interceptors/ auth, error
shared/           header, footer, article-card, loading, empty-state, markdown,
                  rich-editor, confirm-dialog, prompt-dialog, back-to-top
pages/            home (+ roadmap-section), category, article, search, not-found,
                  auth/login, auth/register,
                  admin/dashboard, admin/article-form, admin/categories
models/           article, category, tag, auth, page, api-error
```

All colours, spacing, radii and fonts are design tokens in `src/styles.css`. Both themes are
defined there; component stylesheets only consume the tokens.

---

## 8. Still to do

Honest list of what is missing, roughly in priority order:

- [ ] **Tests** — there are none, on either side
- [ ] **Deployment** — no Dockerfile or hosting config; it runs locally only
- [ ] **Database migrations** — currently `ddl-auto=update`, which is risky in production
- [ ] **Uploaded images are never deleted** — removing an article leaves its files on disk
- [ ] **Admin cannot change their own password from the UI**
- [ ] **Draft autosave** — a token expiring mid-write loses the draft
- [ ] Table button missing in the visual editor; image alt text cannot be edited
- [ ] `robots.txt`, `sitemap.xml`, RSS feed, `og:image` banner
- [ ] ESLint / Prettier, and CI

---

## 9. Notes for the next session

- The dev database lives in `backend/data/` and is git ignored, so a fresh clone starts empty and
  seeds itself.
- Uploaded images live in `backend/uploads/`, also git ignored. Content that references them will
  show broken images on a different machine.
- Reader progress falls back to `localStorage` when nobody is signed in, and is merged into the
  account on the next sign in.
