# ADR-003: Use Flyway for Database Schema Management

## Status

Accepted

## Decision

All production database schema changes will be managed through versioned
Flyway SQL migrations.

Hibernate schema generation will remain disabled and Hibernate will only
validate mappings against the existing schema.

## Rationale

Explicit database migrations provide deterministic, reviewable and
reproducible schema changes across development, testing and production.

## Consequences

- Schema history is version-controlled.
- Existing migrations must not be modified after being shared.
- Database and JPA mappings must remain synchronized.