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

### Start PostgreSQL

```bash
docker compose up -d postgres
```

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

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

