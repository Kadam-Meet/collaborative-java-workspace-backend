# Collaborative Java Workspace Backend

Spring Boot backend for collaborative Java code analysis, optimization, authentication, dashboard data, and versioning support.

## Overview

This repository contains the backend service under the `BE` directory. A related frontend exists in the sibling `FE` directory and calls this API.

## Tech Stack

- Java 21
- Spring Boot 3.3.5
- Maven
- JavaParser
- Spring Validation
- Spring Actuator
- JUnit (Spring Boot Test)
- PostgreSQL

## Repository Structure

```
BE/
├─ pom.xml
├─ .env.example
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/collab/workspace/
│  │  └─ resources/
│  │     ├─ application.properties
│  │     └─ META-INF/additional-spring-configuration-metadata.json
│  └─ test/
│     └─ java/
├─ tools/
│  └─ apache-maven-3.9.9/
├─ apache-maven-3.9.6/
└─ README.md
```

## Prerequisites

- JDK 21 on PATH
- Maven 3.9+ (system Maven or bundled Maven)
- PostgreSQL running locally

## Quick Setup (Teammates)

1. Create a local backend env file from template:

```powershell
Copy-Item .env.example .env
```

2. Open `.env` and set your local values:

```env
DB_USERNAME=postgres
DB_PASSWORD=your-db-password
APP_JWT_SECRET=replace-with-a-strong-secret-at-least-32-characters-long
APP_JWT_EXPIRATION_SECONDS=86400
```

3. Ensure PostgreSQL database exists:

- Database: `workspace`
- Host: `localhost`
- Port: `5432`

4. Run backend:

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

5. Verify service health:

- `http://localhost:8081/actuator/health`

## Configuration Model

Default config lives in `src/main/resources/application.properties`.
Local secrets are loaded from `.env` via:

- `spring.config.import=optional:file:.env[.properties]`

Important properties:

- `server.port=8081`
- `management.endpoints.web.exposure.include=health,info`
- `spring.datasource.url=jdbc:postgresql://localhost:5432/workspace`
- `spring.datasource.username=${DB_USERNAME:postgres}`
- `spring.datasource.password=${DB_PASSWORD:}`
- `app.jwt.secret=${APP_JWT_SECRET:change-this-secret-for-prod}`
- `app.jwt.expiration-seconds=${APP_JWT_EXPIRATION_SECONDS:86400}`

## Security Guard (Production)

`JwtSecretStartupValidator` prevents startup in `prod`/`production` profile when JWT secret is unsafe.

Startup fails if `app.jwt.secret` is:

- blank
- default value (`change-this-secret-for-prod`)
- shorter than 32 characters

This ensures production deployments do not run with weak JWT configuration.

## Run Locally

### Option 1: System Maven

```bash
mvn clean spring-boot:run
```

### Option 2: Bundled Maven (Windows PowerShell)

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd clean spring-boot:run
```

Backend starts on http://localhost:8081.

## Build

```bash
mvn clean package
```

Build output is generated in `target/`.

## Run Tests

```bash
mvn test
```

## Main Class

- `com.collab.workspace.WorkspaceApplication`

## API Endpoints

### Authentication

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET /api/auth/me`

### Workspace Analysis and Optimization

- `POST /api/v1/optimizer/java`
- `POST /api/v1/analyzer/java`
- `POST /api/v1/analyzer/java/full`
- `GET /api/v1/meta/rules`
- `GET /api/v1/meta/health`

### Room and Workspace Management

- `POST /api/workspaces/rooms`
- `POST /api/workspaces/rooms/join`
- `GET /api/workspaces/rooms`
- `GET /api/workspaces/rooms/by-code/{roomCode}`
- `GET /api/workspaces/rooms/{roomId}/members`
- `POST /api/workspaces/rooms/{roomId}/members`
- `GET /api/workspaces/rooms/{roomId}/files`

### Monitoring

- `GET /actuator/health`
- `GET /actuator/info`

## Development Notes

- `.env` is ignored and must never be committed.
- `.env.example` is committed for teammate onboarding.
- `.gitignore` excludes build output, logs, IDE files, and local tool artifacts.

## Smoke Test (Optional)

After backend startup and signup/login token retrieval, test room flow quickly:

1. Create room: `POST /api/workspaces/rooms`
2. Join room by code: `POST /api/workspaces/rooms/join`
3. Fetch my rooms: `GET /api/workspaces/rooms`
4. Fetch room members: `GET /api/workspaces/rooms/{roomId}/members`
5. Fetch room files: `GET /api/workspaces/rooms/{roomId}/files`

All room endpoints require `Authorization: Bearer <token>`.