# Collaborative Java Workspace Backend

Spring Boot backend for Java code analysis and optimization workflows in a collaborative workspace.

## Tech Stack

- Java 21
- Spring Boot 3.3.5
- Maven
- JavaParser
- JUnit (Spring Boot Test)

## Project Structure

```
BE/
├─ pom.xml
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  └─ resources/
│  └─ test/
│     └─ java/
├─ tools/
│  └─ apache-maven-3.9.9/
├─ apache-maven-3.9.6/
├─ java-workspace-backend/
└─ java-workspace-backend-backup/
```

## Prerequisites

- JDK 21 installed and available on PATH
- Maven 3.9+ (system Maven or bundled Maven in this repository)

## Run Locally

### Option 1: Use your system Maven

```bash
mvn clean spring-boot:run
```

### Option 2: Use bundled Maven (Windows PowerShell)

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd clean spring-boot:run
```

## Build

```bash
mvn clean package
```

The packaged application will be generated in the `target/` directory.

## Run Tests

```bash
mvn test
```

## Main Application Class

Configured in `pom.xml`:

- `com.cjw.CollaborativeJavaWorkspaceBackendApplication`

## Notes

- This repository contains additional backend copies/folders (`java-workspace-backend`, `java-workspace-backend-backup`) used for experimentation and backup.
- Ignore rules are configured in `.gitignore` for build outputs and local IDE artifacts.