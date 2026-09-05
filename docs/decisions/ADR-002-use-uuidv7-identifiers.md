# ADR-002: Use UUIDv7 Identifiers

## Status

Accepted

## Context

Application entities require identifiers that are safe to expose through
REST APIs and perform efficiently as database index keys.

## Decision

Use UUID version 7 for persisted application identifiers.

Hibernate generates UUIDv7 values for JPA entities and PostgreSQL stores
them using its native UUID type.

## Rationale

UUIDv7 provides globally unique identifiers while preserving approximate
time ordering, improving B-tree index locality compared with randomly
ordered UUIDv4 values.

## Consequences

- IDs are not trivially enumerable.
- New IDs remain roughly chronological.
- Database indexes have better locality than random UUIDv4 keys.
- UUID values are larger than numeric sequence identifiers.