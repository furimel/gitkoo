# Security

## Baseline

GitKoo implements these security measures:

| Measure | How |
|---------|-----|
| CSRF | Spring Security default, on for all form POSTs |
| XSS | Markdown rendered via CommonMark + Jsoup sanitization |
| SQL injection | Spring Data JDBC parameterized queries |
| Password hashing | BCrypt (Argon2 planned post-MVP) |
| Session security | Server-side sessions, no plaintext passwords |
| SSH key verification | Fingerprint matched against database |
| Permission checks | RepositoryPermissionService on every controller + transport |
| Path traversal | Repository paths are ID-based, not user-controlled |
| Command injection | ProcessBuilder with argument lists, never shell strings |
| Workflow isolation | Environment whitelist, secret masking in logs |
| Secret encryption | Workflow secrets encrypted at rest (AES-GCM) |

## What must never happen

- Git repository paths assembled from raw user input.
- Artifact paths assembled from raw user input.
- Workflow commands concatenated into a shell string (unless the DSL
  explicitly says `run shell`).
- Raw HTML in Markdown output without sanitization.

## Known limitations (MVP)

- CI runs on the host process (no container isolation yet).
- No rate limiting (planned).
- SSH host key is auto-generated on first boot.
