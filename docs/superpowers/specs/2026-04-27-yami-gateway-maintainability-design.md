# Yami Gateway Maintainability Design

**Date:** 2026-04-27

## Summary
Restructure `yami-gateway` for maintainability and clarity while keeping external behavior **100% unchanged**. In particular:
- Preserve filter ordering and response semantics.
- Ensure `/ymb/internal/**` is **not externally reachable** and returns **404**.
- Consolidate backend service-to-service calling into a single approach based on `LBClient`.

## Goals
- Improve readability and separation of concerns in the gateway.
- Remove duplicate/competing approaches for backend calls (manual URL resolution vs load-balanced client).
- Keep configuration centralized under `properties` and maintain existing defaults.

## Non-goals
- No behavior changes visible to clients (paths, encryption rules, status codes, response bodies).
- No protocol changes between gateway and backend.
- No performance tuning or feature additions beyond maintainability-oriented refactors.

## Current Context (high-level)
Gateway is a Spring Boot 4 / Servlet stack service with two key servlet filters:
- `CryptoFilter` (`@Order(10)`): decrypt request bodies and encrypt responses for non-skip paths; performs session-key lookup via backend internal API.
- `TokenFilter` (`@Order(20)`): validates JWT token via backend `/auth/checkToken` for non-skip paths.

A load-balanced HTTP calling abstraction exists:
- `com.kingdom.yami.gateway.client.LBClient`.

## Design

### 1) Package responsibilities

**`com.kingdom.yami.gateway.properties`**
- Contains only `@ConfigurationProperties` records.
- Owns defaults and skip-path matching (`AntPathMatcher`).

**`com.kingdom.yami.gateway.client`**
- Owns all gateway → backend HTTP calls.
- Uses `LBClient` as the only infrastructure primitive (POST JSON, deserialize body).
- Provides focused clients:
  - Token validation client (wraps `tokenProperties.checkUrl()`)
  - Session key lookup client/repository (wraps `cryptoProperties.sessionKeyUrl()`)

**`com.kingdom.yami.gateway.filter.crypto`**
- Owns encryption/decryption pipeline, and only calls into session-key retrieval abstraction.

**`com.kingdom.yami.gateway.filter.token`**
- Owns token extraction and validation, and only calls into token-check abstraction.

### 2) Internal API isolation at routing layer (404)
- External traffic uses `/ymb/**` routes to backend.
- Add a higher-priority routing rule that matches `/ymb/internal/**` and returns **404** without forwarding.
- Gateway internal calls to backend internal API go directly to the backend service name (not via `/ymb`).

> Route configuration is managed in Nacos; we will add only the incremental rule(s) needed for the 404 behavior and will not duplicate full routing in code.

### 3) One backend-call strategy
- Remove/avoid any alternate path that tries to resolve `ServiceInstance` manually or bypasses load-balancer.
- All calls use `LBClient`.

### 4) Behavior invariants
- `CryptoFilter` ordering stays at `@Order(10)`.
- `TokenFilter` ordering stays at `@Order(20)`.
- Error handling semantics remain unchanged:
  - `TokenFilter`: 401 + JSON `ApiResponse.fail("请求非法")`.
  - `CryptoFilter`:
    - skip-path: non-2xx becomes 200 + JSON `ApiResponse.fail("系统错误")` (existing behavior).
    - non-skip: encryption error behavior unchanged.

## Verification Plan
- Compile: `./gradlew :yami-gateway:compileJava`
- Runtime smoke checks:
  - `/ymb/internal/**` returns 404.
  - Skip-path endpoints behave unchanged.
  - Missing/invalid token yields 401 JSON.
  - Missing/invalid session id yields same response as before.

## Rollout / Risk
- Refactor-only; no public API or semantics changes.
- Routing change is additive and scoped to `/ymb/internal/**`.
