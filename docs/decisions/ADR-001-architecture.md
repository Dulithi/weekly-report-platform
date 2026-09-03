# ADR-001: Use a Modular Monolith

## Status

Accepted

## Context

The system contains closely related domains including authentication,
weekly reports, review workflows, projects, analytics, and user management.

## Decision

Use a modular monolith consisting of:

- Next.js frontend
- Spring Boot REST API
- PostgreSQL database

The backend will be organized by business feature rather than by technical
layer alone.

## Rationale

The application is a cohesive internal business system and does not require
the operational complexity of independently deployed microservices.

A modular monolith provides clear domain boundaries while preserving simple
deployment, transactions, testing, and development.

## Consequences

- Simpler deployment and local development
- Strong transactional consistency
- Easier live coding and maintenance
- Domain modules can be extracted later if future scale requires it