# Git Transport

## Smart HTTP

`GitHttpController` handles clone, push, and pull over HTTPS.

Routes (matching both `/{owner}/{name}` and `/{owner}/{name}.git`):

```
GET  info/refs?service=git-upload-pack|git-receive-pack   (advertise refs)
POST git-upload-pack                                      (fetch/clone)
POST git-receive-pack                                     (push)
```

Implementation: invokes `git-upload-pack` or `git-receive-pack` in
stateless-RPC mode via `ProcessBuilder`. Streams the request body to the
process stdin and the process stdout to the response without buffering.

Permission checks:
- `git-upload-pack` (fetch): requires READ.
- `git-receive-pack` (push): requires WRITE. Also checks protected branches
  before allowing the push.

## SSH

`GitSshServer` (Apache MINA SSHD) handles Git over SSH on port 2222.

Flow:
```
git push (SSH)
  -> SSH connection
  -> PublickeyAuthenticator (fingerprint lookup in ssh_keys table)
  -> CommandFactory (parse "git-upload-pack 'owner/repo.git'")
  -> resolve repository
  -> spawn git-upload-pack / git-receive-pack
  -> pipe SSH streams to/from the subprocess
```

Only key auth is supported over SSH. No passwords.
