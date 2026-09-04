# Authentication

## First-run setup

When the database has no admin user, all requests redirect to `/setup`.
The user creates the first administrator account (username, email,
password). After that, setup is disabled.

## Registration

After setup, new users self-register at `/register`. No admin approval
required (MVP). Created users are non-admin by default.

## Login

Session-based via Spring Security form login. BCrypt password hashing.
CSRF protection on all form POSTs.

## Personal access tokens

For API access and Git over HTTP:
- Token format: `gitkoo_` + 32 random bytes hex-encoded.
- Stored as BCrypt hash (raw token shown once at creation).
- Used via `Authorization: Bearer gitkoo_...` header.
- Accepted by `/api/**` and Git smart-HTTP endpoints.

## SSH keys

For Git over SSH:
- Registered at `/settings/keys` (paste public key, fingerprint computed
  via `KeyUtils.getFingerPrint`).
- SSH server (MINA SSHD) matches connecting key fingerprint against the
  `ssh_keys` table.
- The SSH username must match the key's owner.
- Only key auth over SSH (no passwords).
