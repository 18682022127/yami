# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yami is a Spring Boot microservices application using Spring Cloud for service orchestration. The project consists of three modules:

- **yami-backend**: Main business logic service with authentication, REST APIs
- **yami-gateway**: API Gateway using Spring Cloud Gateway MVC for routing and request filtering
- **yami-common**: Shared library module for common utilities (currently minimal)

## Technology Stack

- Java 17
- Spring Boot 4.0.5
- Spring Cloud 2025.1.1
- Spring Cloud Alibaba 2025.1.0.0 (Nacos for service discovery and config)
- MyBatis for database access
- MySQL database
- Redis for session storage
- JWT (jjwt 0.12.6) for authentication tokens
- Lombok for boilerplate reduction
- Gradle with Kotlin DSL

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :yami-backend:build
./gradlew :yami-gateway:build

# Run tests
./gradlew test

# Run tests for specific module
./gradlew :yami-backend:test

# Clean build artifacts
./gradlew clean

# Create bootable JAR (for backend/gateway)
./gradlew :yami-backend:bootJar
./gradlew :yami-gateway:bootJar

# Assemble without tests
./gradlew assemble
```

## Running Services

Each service requires Nacos running locally on port 8848 with credentials configured in application.properties.

```bash
# Run backend service
./gradlew :yami-backend:bootRun

# Run gateway service
./gradlew :yami-gateway:bootRun
```

Services pull their main configuration from Nacos at startup. Local application.properties only contains Nacos connection details.

## Architecture

### Service Communication

- Gateway routes requests to backend services via Spring Cloud LoadBalancer
- Services register with Nacos for discovery
- Gateway uses path-based routing: `/ymb/**` routes to yami-backend (prefix stripped)

### Authentication Flow

1. Client calls `/ymb/auth/login` (bypasses auth via gateway skip-auth-paths)
2. Backend validates phone + code (currently fixed code "123456")
3. Backend generates JWT token + session ID + session encryption key
4. Session key stored in Redis with 7-day TTL under key pattern `sess:{sessionId}:key`
5. Client receives token, sessionId, and sessionEncKey in response

### Configuration Management

- Nacos groups: `yami-backend-dev`, `yami-gateway-dev`
- Namespace: `dev01`
- Each service imports `application.properties` from its Nacos group
- Local properties only contain Nacos bootstrap config

### Module Structure

**yami-backend** structure:
- `common/aop`: Cross-cutting concerns (ControllerRequestAop for REST controllers)
- `common/config`: Configuration properties (JwtProperties)
- `common/annotation`, `common/handler`, `common/interceptor`, `common/security`, `common/tools`: Infrastructure packages (currently empty or minimal)
- `login`: Authentication endpoints and service
- `index`: Basic index controller

**yami-gateway** structure:
- Minimal Spring Boot application
- Routes configured via properties (both custom `gateway.routes` and native Spring Cloud Gateway MVC)
- Skip-auth paths configured for public endpoints

**yami-common**:
- Shared library module
- Published as Maven artifact
- Currently contains minimal code (YamiConnon class)

## Development Notes

### Gradle Configuration

- Root build.gradle.kts uses `configure(listOf(...))` to apply Spring Boot plugin only to backend and gateway
- Common module is a plain Java library without Spring Boot
- Dependency management uses Spring Boot BOM, Spring Cloud BOM, and Alibaba Cloud BOM
- All subprojects share common dependencies (test frameworks, Lombok)

### Testing

- JUnit Platform with AssertJ for assertions
- Spring Boot Test for integration tests
- Run single test class: `./gradlew :yami-backend:test --tests "com.kingdom.yami.backend.YamiBackendApplicationTests"`

### Code Conventions

- Uses Java 17 features (records for DTOs)
- Lombok for reducing boilerplate
- Constructor injection for dependencies
- Record classes for request/response DTOs
- Configuration properties use `@ConfigurationProperties` with records

## External Dependencies

- **Nacos**: Must be running on localhost:8848 with username `nacos` and password `qk0ZxHcegXWy`
- **MySQL**: Connection details in Nacos config
- **Redis**: Connection details in Nacos config

## Security Notes

- JWT secret and TTL configured via Nacos (`security.jwt.secret`, `security.jwt.ttlMs`)
- Session encryption keys generated per-session and stored in Redis
- Gateway skip-auth-paths controls which endpoints bypass authentication
- Current implementation uses fixed verification code "123456" for development
