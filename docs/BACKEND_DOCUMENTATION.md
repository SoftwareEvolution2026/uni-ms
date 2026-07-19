# Backend Documentation

**Project:** University Student Management System (uni-ms)

**Author:** Umar-Faruq Salihu, 23/378/BSSE-M

---

## Overview

This document describes the backend implementation of the uni-ms project.
The backend is built with Spring Boot, Java 17, Spring Security, Spring Data JPA, and Flyway.

## Architecture

- **Framework:** Spring Boot 3.3
- **Language:** Java 17
- **Database:** MySQL / MariaDB
- **Migrations:** Flyway
- **Security:** JWT access token + refresh token

## Folder structure

- `src/main/java/com/uni/ms/`
  - `auth/`
  - `common/`
  - `student/`
  - `user/`
  - `result/`

## Key backend features

- JWT-based authentication with rotating refresh tokens
- Role-based authorization for admin, lecturer, staff, and student
- Global exception handling and consistent API error responses
- Audit logging for security-sensitive actions
- Student management module
- User management module
- Results module for Team 4 academic records

## Important files

- `src/main/java/com/uni/ms/auth/` — auth controllers, services, DTOs, and refresh token handling
- `src/main/java/com/uni/ms/common/security/SecurityConfig.java` — HTTP security configuration
- `src/main/java/com/uni/ms/common/audit/AuditService.java` — audit logging service
- `src/main/java/com/uni/ms/user/` — user entity, repository, service, controller
- `src/main/java/com/uni/ms/student/` — student entity, repository, service, controller
- `src/main/java/com/uni/ms/result/` — result entity, repository, service, controller
- `src/main/resources/db/migration/` — Flyway database migrations

## Configuration

- `backend/src/main/resources/application.yml`
  - database connection
  - Flyway settings
  - JWT properties
  - CORS origins
  - server port

## How to run

```bash
cd backend
mvn spring-boot:run
```

## Testing

- Backend unit tests can be executed with:

```bash
cd backend
mvn test
```

## Notes

- Flyway migrations are append-only. Do not edit already-merged migrations.
- Use the `develop` branch for integration work.

## Additional details

(Leave this section blank for later completion.)

- Name:
- Role:
- Date:
- Comments:
