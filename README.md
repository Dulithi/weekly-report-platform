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
