# REST API Reference

Base URL: `http://localhost:8080/api`

Every failing request returns the same envelope:

```json
{
  "timestamp": "2026-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/articles",
  "fieldErrors": { "title": "Title is required" }
}
```

| Status | When |
|--------|------|
| 400 | Validation failed, unreadable body, keyword shorter than 2 characters, rejected upload |
| 401 | Missing, invalid or expired token; wrong login credentials |
| 403 | Valid token without the ADMIN role |
| 404 | Unknown id or slug, draft requested by a visitor |
| 409 | Duplicate slug, or deleting a category that still has articles |
| 413 | Upload larger than 5 MB |

## Authentication

### POST /api/auth/login

```json
{ "email": "admin@example.com", "password": "Admin@12345" }
```

```json
{
  "token": "JWT",
  "type": "Bearer",
  "role": "ADMIN",
  "username": "admin",
  "email": "admin@example.com",
  "expiresAt": "2026-01-02T10:00:00Z"
}
```

### GET /api/auth/me

Requires a token. Returns `id`, `username`, `email`, `role`.

## Categories

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/api/categories` | Public | Ordered by `displayOrder`, includes `articleCount` |
| GET | `/api/categories/roadmap` | Public | Every category with its articles, one call for the accordion |
| GET | `/api/categories/{id}` | Public | |
| GET | `/api/categories/slug/{slug}` | Public | Category with its articles |
| POST | `/api/categories` | ADMIN | 201 with `Location` |
| PUT | `/api/categories/{id}` | ADMIN | |
| DELETE | `/api/categories/{id}` | ADMIN | 409 while the category still has articles |

Request body:

```json
{
  "name": "Object Oriented Programming",
  "slug": "object-oriented-programming",
  "description": "Classes, objects and the four pillars",
  "displayOrder": 2
}
```

`slug` is optional. When it is empty the backend derives a unique slug from the name.

## Articles

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/api/articles?page=0&size=10` | Public | Published only, newest first |
| GET | `/api/articles/search?keyword=inheritance` | Public | Title, description and tags |
| GET | `/api/articles/manage?keyword=&page=0&size=10` | ADMIN | Drafts included |
| GET | `/api/articles/category/{categorySlug}` | Public | Ordered by `displayOrder` |
| GET | `/api/articles/slug/{slug}` | Public | |
| GET | `/api/articles/slug/{slug}/related` | Public | Up to 6 from the same category |
| GET | `/api/articles/{id}` | Public | Drafts need an ADMIN token |
| POST | `/api/articles` | ADMIN | |
| PUT | `/api/articles/{id}` | ADMIN | |
| DELETE | `/api/articles/{id}` | ADMIN | |

Request body:

```json
{
  "title": "Inheritance in Java",
  "slug": "inheritance-in-java",
  "description": "Understanding inheritance with real world examples.",
  "content": "# Inheritance in Java\n\nMarkdown body...",
  "categoryId": 2,
  "displayOrder": 3,
  "githubUrl": "https://github.com/username/java-examples",
  "youtubeUrl": "https://youtube.com/watch?v=xyz",
  "thumbnailUrl": "/uploads/articles/cover.png",
  "published": true,
  "tags": ["java", "oop", "inheritance"]
}
```

Paged responses use one envelope:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Rules enforced by the service layer:

- Tags are trimmed, lower cased and de-duplicated, so `Java`, ` java ` and `JAVA` share one row.
- An empty `slug` is generated from the title; collisions get `-2`, `-3` and so on.
- A supplied `slug` that is already taken returns 409 instead of silently changing it.
- `keyword` is escaped before it reaches `LIKE`, so `100%` cannot match every row.
- `size` is capped at 50.

## Tags

`GET /api/tags` returns every tag ordered by name. Used by the admin form for suggestions.

## Files

### POST /api/files/upload (ADMIN, multipart)

Field name `file`. PNG, JPEG, GIF, WEBP and SVG up to 5 MB.

```json
{ "url": "/uploads/articles/1f9c....png", "fileName": "1f9c....png", "size": 20480 }
```

The stored file name is a fresh UUID, so a crafted upload name cannot escape the folder.
Images are served from `GET /uploads/articles/{fileName}`.
