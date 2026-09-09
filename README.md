# Weekly Report Platform

A full-stack weekly reporting and team analytics platform.

## Technology Stack

### Frontend
- Next.js 16.3.3
- React 19.2.8
- TypeScript
- Tailwind CSS 4

### Backend
- Java 25
- Spring Boot 4.1.1
- Spring Security
- Spring Data JPA
- Flyway

### Database
- PostgreSQL 18

## Prerequisites

- Java 25
- Node.js 24.20 or newer 24.x LTS release
- Docker
- Docker Compose

## Local Development

Create a local environment file and replace every placeholder value. The `.env`
file is ignored by Git.

```bash
cp .env.example .env
```

Generate the JWT signing secret locally and paste the output into
`JWT_SECRET_BASE64`. Use a separate high-entropy database password for
`POSTGRES_PASSWORD` and `DATABASE_PASSWORD` (both values must match in local
Docker development).

```bash
openssl rand -base64 32
```

Never commit `.env`, demo passwords, invitation tokens, access/refresh tokens or
an AI provider key.

### Start PostgreSQL

```bash
docker compose up -d postgres
```

### Start Backend

```bash
cd backend
set -a
source ../.env
set +a
./mvnw spring-boot:run
```

### Optional Demo Data

To create the assessment demonstration dataset, set these values in `.env`:

```dotenv
DEMO_SEED_ENABLED=true
DEMO_PASSWORD=choose-a-password-with-at-least-12-characters
```

Then start the backend with the development profile:

```bash
cd backend
set -a
source ../.env
set +a
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The seed runs only when both the `dev` profile and the explicit enable flag are
present. It does not overwrite or duplicate the dataset on later restarts. All
demo accounts use the password supplied through `DEMO_PASSWORD`.

- Admin: `demo.admin@weekly.local`
- Manager: `demo.manager@weekly.local`
- Members: `alex.morgan@weekly.local`, `priya.shah@weekly.local`,
  `sam.perera@weekly.local`, `mei.chen@weekly.local`, and
  `jordan.silva@weekly.local`

### API Documentation

The `dev` Spring profile also enables the generated OpenAPI contract and Swagger
UI. They are disabled by default outside development so a deployed production
instance does not publish its endpoint catalogue accidentally.

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To call a protected endpoint in Swagger UI, log in and copy the returned access
token into **Authorize → bearerAuth**. Authentication POST requests also use the
application's CSRF protection: first call `GET /api/v1/auth/csrf`, then copy the
response token into **Authorize → csrfToken**. Swagger UI keeps the matching
same-origin cookie set by the CSRF endpoint.

### Optional Manager AI Assistant

The manager assistant is disabled by default. To enable the OpenAI Responses API
adapter, keep the API key only in the ignored local `.env` file or your deployment
secret store:

```dotenv
AI_ASSISTANT_ENABLED=true
OPENAI_API_KEY=your-secret-api-key
```

Restart the backend after changing these values. The default model, reasoning
effort, connection timeout, request timeout and all request-size/rate limits are
listed in `.env.example` and can be changed without rebuilding the application.
The backend sends only bounded data from submitted report versions, explicitly
sets provider storage to false, exposes no model tools and validates returned
source keys before creating report links. Each successful model request can incur
provider usage charges. If the adapter is disabled or unavailable, the rest of
the platform continues to work and the assistant endpoint returns a sanitized
service-unavailable response.
Managers and administrators access the interface at `/manager/assistant`. Its
conversation history remains only in the current browser tab and disappears when
the page is refreshed.

### Start Frontend

```bash
cd frontend
npm ci
npm run dev
```

## Local URLs

- Frontend : [http://localhost:3000](http://localhost:3000)
- Backend : [http://localhost:8080](http://localhost:8080)
- Health : [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI (dev profile): [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Verification

Backend tests use PostgreSQL 18 through Testcontainers, so Docker must be
running:

```bash
cd backend
./mvnw test
```

Run the frontend static checks and production build:

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run build
```

The assessment browser scenarios, expected results and production checks are in
[the manual test plan](docs/manual-test-plan.md). Run at least every P0 case and
the full submit → correction → resubmit → approve workflow before recording or
deploying.

### Continuous integration

[GitHub Actions CI](.github/workflows/ci.yml) runs on pushes to `main`, pull
requests and manual dispatches. It runs all PostgreSQL-backed backend tests and
the frontend ESLint, TypeScript and production-build checks. After a successful
push to `main`, it also verifies that both production Docker images build.
Documentation-only changes skip CI, and pull requests skip the duplicate Docker
packaging jobs to conserve hosted-runner minutes. The workflow has read-only
repository permissions and receives no application secrets.


## Production Configuration

Use HTTPS and a deployment secret manager. Set `REFRESH_COOKIE_SECURE=true`, keep
`REFRESH_COOKIE_SAME_SITE=Strict` when the frontend and API deployment topology
allows it, and set `FRONTEND_URL` to the single exact public frontend origin. Do
not enable the demo seed or development profile in production. Leave Swagger and
the AI assistant disabled unless they are intentionally required.

After deployment, verify the health endpoint, TLS, login/refresh/logout,
credentialed CORS, CSRF protection, cookie flags and role/resource boundaries
using the production section of the manual test plan.
