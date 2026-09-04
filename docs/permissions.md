# Permissions

`RepositoryPermissionService` centralizes all permission checks. It is
wired into every controller and both Git transport layers (HTTP + SSH).

## Permission levels

```
NONE < READ < WRITE < ADMIN
```

## How permissions are resolved

1. If the user is the repository owner (USER-owned repo), they get ADMIN.
2. If the user is an explicit `repository_members` grant, use that
   permission (READ, WRITE, or ADMIN).
3. If the repo is TEAM-owned, check the user's team role:
   - OWNER -> ADMIN
   - MAINTAINER -> WRITE
   - MEMBER -> READ
4. Fall back to visibility:
   - PUBLIC -> READ for everyone (including anonymous)
   - INTERNAL -> READ for logged-in users only
   - PRIVATE -> NONE (only via grants or ownership)

The highest permission from any source wins.

## Protected branches

A branch can be marked as protected with `require_pr = true`. When
enforced, direct pushes to that branch are rejected with HTTP 403. Users
must open a pull request instead.

Enforcement happens in `GitHttpController` before `git-receive-pack`
runs, by parsing the ref update from the request body.
