# Security Policy

## Reporting a vulnerability

If you discover a security vulnerability in GitKoo, **please do not open a
public issue**. Instead, report it privately:

1. Go to the [GitHub Security Advisories](https://github.com/furimeo/gitkoo/security/advisories)
   page for this repository.
2. Click "New draft security advisory" and describe the vulnerability.
3. Alternatively, email the maintainer directly via GitHub.

We will acknowledge your report within 48 hours and work with you to
understand and fix the issue before any public disclosure.

## Supported versions

| Version | Supported |
|---------|-----------|
| 0.x     | ✅ (current development) |

GitKoo is pre-1.0; only the latest `main` is supported.

## Security measures

GitKoo implements the following security baseline (DESIGN.md §78):

- **CSRF protection** on all form POSTs (Spring Security default)
- **XSS protection** - Markdown rendered through CommonMark + Jsoup sanitization
- **SQL injection** - Spring Data JDBC uses parameterized queries
- **Password hashing** - BCrypt (Argon2 planned for Phase 3)
- **Session security** - server-side sessions, no plaintext passwords stored
- **SSH key verification** - public key fingerprints checked against database
- **Path traversal** - repository paths are ID-based, not user-controlled
- **Command injection** - `ProcessBuilder` with argument lists, never shell
  string concatenation (unless workflow DSL explicitly requests `run shell`)
- **Workflow isolation** - environment whitelist, secret masking in logs,
  TRUSTED/UNTRUSTED execution modes (DESIGN.md §33)
- **Secret encryption** - workflow secrets encrypted at rest (AES-GCM)

## Known limitations (MVP)

- CI runs on the host process (no container isolation yet, DESIGN.md §33)
- No rate limiting implemented (planned for Phase 2)
- SSH host key is auto-generated on first boot (stored in `data/.ssh_host_key`)
