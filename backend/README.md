# Skill Discovery Backend

FastAPI backend for the Skill Discovery Android app. It keeps the existing `/api/v1` Retrofit routes and stores data in SQLite by default. The SQLAlchemy models also support PostgreSQL through `DATABASE_URL`.

## Run locally

From this directory:

```powershell
python -m pip install -e ".[test]"
Copy-Item .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The API is available locally at `http://127.0.0.1:8000/api/v1`. For an Android emulator or phone on the same network, use the PC's IPv4 address, for example `http://10.244.192.82:8000/api/v1/`. OpenAPI documentation is at `/docs` and the health check is `/api/v1/health`.

For a phone without USB, the phone and PC must be on the same non-isolated Wi-Fi network. If Windows marks the network as Public, create the inbound rule from an Administrator PowerShell:

```powershell
New-NetFirewallRule -DisplayName "Skill Discovery API 8000" -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow -Profile Any
```

Then use the PC's current IPv4 URL in the app, such as `http://10.244.192.82:8000/api/v1/`. ADB reverse is only a USB development fallback and is not required after LAN access is allowed.

A local database is created at `skill_disco.db` on first startup. The seeded demo account is:

- Email: `demo@skilldiscovery.app`
- Password: `password123`
- Class code: `CSE2026`

Delete `skill_disco.db` to reset local data.

## Configuration

Copy `.env.example` to `.env` and set:

- `DATABASE_URL`: `sqlite:///./skill_disco.db` locally or a PostgreSQL URL in deployment.
- `JWT_SECRET`: a long random secret outside local development.
- `GEMINI_API_KEY`: optional. When set, achievement creation and `/skills/extract` use Gemini server-side. When absent, deterministic local extraction keeps development and tests usable.
- `CORS_ORIGINS`: comma-separated origins, or `*` for local development.

The Gemini key is never sent to the Android client.

## Implemented behavior

- JWT login and registration with class membership.
- Achievement persistence and skill extraction.
- Skill records, graph edges, activity events, and weekly metrics.
- Class-scoped discussion visibility.
- Upvote toggle with updated count/state.
- One poll vote per user, including changing an option.
- `DELETE /api/v1/discussions/{id}/vote` clears only the signed-in user's vote.

## Tests

```powershell
pytest
```

## Android Connectivity And Troubleshooting

### Confirm the backend first

Keep this process running from `backend`:

```powershell
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Check the backend from the PC:

```powershell
Invoke-WebRequest http://127.0.0.1:8000/api/v1/health -UseBasicParsing
Invoke-WebRequest http://10.244.192.82:8000/api/v1/health -UseBasicParsing
```

Both requests should return HTTP `200` and:

```json
{"status":"ok","service":"Skill Discovery API"}
```

The Uvicorn messages below are normal startup messages, not errors:

```text
Uvicorn running on http://0.0.0.0:8000
Waiting for application startup.
Application startup complete.
```

### LAN connection without USB

The Android app must use the exact URL below, including the port:

```text
http://10.244.192.82:8000/api/v1/
```

Do not use:

- `http://10.244.192.82/api/v1/` because it defaults to port 80.
- `https://10.244.192.82:8000/api/v1/` because the local server is HTTP, not HTTPS.
- `http://127.0.0.1:8000/api/v1/` unless an ADB reverse tunnel is active.

The phone and PC must be on the same non-isolated Wi-Fi network. Guest Wi-Fi, mobile-hotspot client isolation, VPNs, and corporate Wi-Fi policies can block phone-to-PC traffic even when both devices have addresses in the same range.

This project was tested with the PC at `10.244.192.82` and the phone at `10.244.192.55`. A direct phone TCP test failed while the PC responded locally, which indicates host firewall or Wi-Fi isolation rather than a FastAPI or Retrofit problem.

If Windows reports the active network as `Public`, create the rule from **Administrator PowerShell**. A normal PowerShell will return `Access is denied` and the rule will not be created:

```powershell
New-NetFirewallRule `
	-DisplayName "Skill Discovery API 8000" `
	-Direction Inbound `
	-Protocol TCP `
	-LocalPort 8000 `
	-Action Allow `
	-Profile Any
```

Verify that the rule is applicable:

```powershell
Get-NetConnectionProfile
Get-NetFirewallRule -DisplayName "Skill Discovery API 8000"
```

The rule must be enabled and applicable to the active profile. A rule with `Profile Private` is not used when the active Wi-Fi network is `Public`.

### USB development fallback

ADB reverse is reliable for development, but it requires the phone to remain connected by USB:

```powershell
C:\sdk\platform-tools\adb.exe reverse tcp:8000 tcp:8000
C:\sdk\platform-tools\adb.exe reverse --list
```

Use this URL in the app only while the tunnel exists:

```text
http://127.0.0.1:8000/api/v1/
```

The expected tunnel output is:

```text
UsbFfs tcp:8000 tcp:8000
```

### Android settings

In **More > Backend Server Integration**:

1. Set the LAN URL with `:8000`, or set `127.0.0.1:8000` when using ADB reverse.
2. Turn **Use Sandbox Backend** off.
3. Tap **Save Config**.
4. Tap **Sync Now**.

The app allows local HTTP traffic for development. The stored URL can be inspected with:

```powershell
C:\sdk\platform-tools\adb.exe shell run-as com.aistudio.skiller cat shared_prefs/skill_discovery_session_prefs.xml
```

Check `api_base_url` and `sandbox_backend`. A real session should contain a JWT beginning with `eyJ`; `bearer_jwt_token_alex_2026` is a sandbox token and is not valid for the real backend.

### JWT authentication

`JWT_SECRET` stays on the backend. It signs tokens during login and verifies them on protected endpoints. It is never copied into the Android app.

After changing `JWT_SECRET`, restart Uvicorn and log in again. Existing tokens will return `401 Unauthorized`:

```powershell
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

To verify a real token manually:

```powershell
$base = "http://127.0.0.1:8000/api/v1"
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" `
	-ContentType "application/json" `
	-Body (@{email="demo@skilldiscovery.app"; password="password123"} | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri "$base/users/me" -Headers $headers
Invoke-RestMethod -Uri "$base/users/me/metrics" -Headers $headers
```

### Achievements, AI extraction, and profile updates

Creating an achievement performs this server-side sequence:

1. The backend sends the title and description to Gemini when `GEMINI_API_KEY` is configured.
2. Without Gemini, deterministic local extraction is used.
3. Extracted skills are stored on the achievement.
4. Existing user skills are increased or new skills are created.
5. Skill graph edges are created.
6. An activity event is recorded.
7. `/users/me/metrics` returns skills, graph edges, weekly activity, and consistency.

The Android app refreshes profile, achievements, and metrics after creating an achievement. The achievement list is authoritative when data comes from the backend. Image-backed cards are selected from the three newest records with a non-empty `proof_url`; records without images are not displayed in that image-backed section.

If the three original demo image cards are needed, they are local Android placeholder data. They are not backend records unless they are posted with a real `proof_url`.

### Polls, upvotes, and counts

Poll and upvote counts are calculated by the backend from `PollVote` and `DiscussionUpvote` rows. The Android app must not permanently calculate or trust local counts.

Relevant endpoints:

- `POST /api/v1/discussions/{id}/upvote` toggles the signed-in user's upvote and returns the authoritative count/state.
- `POST /api/v1/discussions/{id}/vote` records or changes one user's poll option and returns all option counts.
- `DELETE /api/v1/discussions/{id}/vote` removes only the signed-in user's vote and returns updated counts.
- `GET /api/v1/discussions` reloads authoritative poll and upvote values.

The Android client prevents duplicate rapid submissions while a vote request is pending. After a failed request, it keeps the previous displayed state and emits an error instead of inventing a new count.

### Why the UI can look stale

Room is a local display cache. A failed network request can otherwise leave old data visible. The app now replaces the current user's achievement cache and the full discussion cache on successful unfiltered sync. It also bounds a full sync so the loading indicator cannot run forever.

If Logcat shows:

```text
HTTP FAILED: SocketTimeoutException ... port 80
```

the URL is missing `:8000`.

If it shows:

```text
HTTP FAILED: SocketTimeoutException ... port 8000
```

the app has the right URL, but the phone cannot reach the PC because of firewall or Wi-Fi isolation.

If it shows:

```text
401 Unauthorized
```

the URL works but the token is expired, signed with an old `JWT_SECRET`, or still a sandbox token. Log in again.

Messages such as `avc: denied ... /proc/fas/render`, `ProfileInstaller`, and Vivo `ro.vendor` property warnings are device-vendor rendering/runtime messages. They are unrelated to backend connectivity, JWT authentication, polls, or achievement synchronization.
