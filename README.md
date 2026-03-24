# Collaborative Java Workspace Backend

Spring Boot backend for collaborative Java code analysis, optimization, authentication, dashboard data, and versioning support.

## Overview

This repository contains the backend service under the `BE` directory. A related frontend exists in a sibling folder (`../FE/java-workspace-frontend`) and can call this API.

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
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/collab/workspace/
│  │  └─ resources/
│  │     └─ application.properties
│  └─ test/
│     └─ java/
├─ tools/
│  └─ apache-maven-3.9.9/
├─ apache-maven-3.9.6/
├─ java-workspace-backend/
└─ java-workspace-backend-backup/
```

## Prerequisites

- JDK 21 on PATH
- Maven 3.9+ (system Maven or bundled Maven)
- PostgreSQL running locally

## Configuration

Default config is in `src/main/resources/application.properties`.

Current important defaults:

- `server.port=8081`
- `management.endpoints.web.exposure.include=health,info`
- `spring.datasource.url=jdbc:postgresql://localhost:5432/workspace`
- `spring.datasource.username=postgres`
- `spring.datasource.password=your_password`

Before running in your environment, update database credentials as needed.

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

### Monitoring

- `GET /actuator/health`
- `GET /actuator/info`

## Development Notes

- `.gitignore` excludes build output, logs, IDE files, and backup folders.
- Additional folders like `java-workspace-backend` and `java-workspace-backend-backup` are retained for backup or experimental workflows.