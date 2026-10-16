# Implementation Plan

## Goal Description
Create the missing test suite for the backend, execute the Maven test suite to ensure zero failures, generate the Docker Compose configuration, build and launch the full stack (backend, frontend, MySQL, Redis), and run a series of smoke tests using `curl` to verify functionality, including rate limiting, redirection, analytics, and UI availability.

## User Review Required
> [!IMPORTANT]
> The plan includes running network operations (Docker containers, `curl` requests) on your machine. Ensure Docker is installed and running, and that ports 8080 and 5173 are free.
>
> If you have any custom network configurations or proxy settings, let me know before proceeding.

## Open Questions
> [!WARNING]
> 1. **Test Coverage Preferences** – Do you want additional unit tests beyond the three core classes (`Base62Util`, `UrlService`, `UrlControllerIntegrationTest`), e.g., for the RateLimiterFilter or async click recording?
> 2. **Docker Compose File Location** – Should the `docker-compose.yml` be placed at the workspace root (`c:/Users/hp/Documents/URL_shortner/docker-compose.yml`) or inside a `docker/` subdirectory?
> 3. **Smoke Test Assertions** – Do you want the smoke test script to halt on first failure, or continue and report all results?

## Proposed Changes
---
### Backend Tests
- **[NEW] Base62UtilTest.java** – JUnit 5 tests for encoding/decoding edge cases.
- **[NEW] UrlServiceTest.java** – Mockito‑based unit tests for `UrlService` covering short URL creation, custom alias handling, caching, and async click recording.
- **[NEW] UrlControllerIntegrationTest.java** – SpringBootTest with Testcontainers for MySQL & Redis, exercising POST `/api/v1/urls/shorten`, GET redirect, rate‑limit (429), and analytics endpoint.

### Docker Compose
- **[NEW] docker-compose.yml** – Services: `db` (MySQL), `redis`, `backend` (built from Maven source), `frontend` (built from the SPA Dockerfile). Includes environment variables, healthchecks, network `urlnet`, and port mappings (`8080:8080`, `5173:80`).

### Smoke Test Script
- **[NEW] smoke_test.sh** – Bash script (executed via PowerShell) that:
  1. Calls POST shorten, extracts `shortCode`.
  2. Performs GET redirect and checks HTTP 302 + Redis key existence.
  3. Sends 25 rapid POST requests to trigger HTTP 429.
  4. Calls analytics endpoint and verifies `totalClicks > 0`.
  5. Issues a simple `curl http://localhost:5173` to ensure the UI is reachable.
  - Outputs a concise pass/fail report.

## Verification Plan
### Automated Tests
- Run `mvn -B clean test` – expect **0 failures**.
- Run `mvn -B verify` to execute integration tests.

### Manual Verification
- After Docker Compose is up, execute `smoke_test.sh`.
- Review console output for any failed assertions.
- If any step fails, adjust code (e.g., RateLimiterFilter path check, Redis key naming) and repeat relevant tests.
