# Weekly Report Platform

A full-stack weekly reporting and team analytics platform.

## Technology Stack

### Frontend
- Next.js
- React
- TypeScript
- Tailwind CSS

### Backend
- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway

### Database
- PostgreSQL 18

## Prerequisites

- Java 25
- Node.js 24 LTS
- Docker
- Docker Compose

## Local Development

Create a local environment file and replace every placeholder value. The `.env`
file is ignored by Git.

```bash
cp .env.example .env
```

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

### Start Frontend

```bash
cd frontend
npm install
npm run dev
```

## Local URLs

- Frontend : [http://localhost:3000](http://localhost:3000)
- Backend : [http://localhost:8080](http://localhost:8080)
- Health : [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI (dev profile): [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
