# Yami Gateway Maintainability Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `yami-gateway` for clearer boundaries (properties/client/filters) and enforce `/ymb/internal/**` external 404 at the routing layer while keeping external behavior 100% unchanged.

**Architecture:** Keep `CryptoFilter` and `TokenFilter` behavior/order unchanged, consolidate all backend calls behind `LBClient`, and remove any alternate manual service-resolution codepaths.

**Tech Stack:** Java 17, Spring Boot 4.0.5 (Servlet), Spring Cloud Gateway Server WebMVC, Spring Cloud LoadBalancer, RestClient

---

## File structure map (after refactor)

**Modify (gateway):**
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/client/LBClient.java`
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/token/TokenCheckClient.java`
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/token/TokenFilter.java`
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyRepository.java`
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/CryptoFilter.java` (only if needed to keep behavior identical after client cleanup)

**Potential delete (gateway, if unused after consolidation):**
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyClient.java`

**Create (gateway tests):**
- `yami-gateway/src/test/java/com/kingdom/yami/gateway/routing/InternalRouteBlockTest.java`

**Nacos config change (out-of-repo runtime config):**
- Add a higher priority WebMVC gateway route that matches `/ymb/internal/**` and returns 404 without forwarding.

---

### Task 1: Lock in current observable behaviors with tests

**Files:**
- Create: `yami-gateway/src/test/java/com/kingdom/yami/gateway/routing/InternalRouteBlockTest.java`

- [ ] **Step 1: Add a failing test skeleton for internal-path block**

```java
package com.kingdom.yami.gateway.routing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalRouteBlockTest {

	private final MockMvc mockMvc;

	InternalRouteBlockTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void internalPathShouldReturn404() throws Exception {
		mockMvc.perform(get("/ymb/internal/auth/getEncKeyBySessionId"))
			.andExpect(status().isNotFound());
	}
}
```

- [ ] **Step 2: Run the test to confirm current behavior (expected FAIL until routing config exists)**

Run:
```bash
./gradlew :yami-gateway:test --tests "com.kingdom.yami.gateway.routing.InternalRouteBlockTest"
```

Expected: FAIL (route block not yet in place in local test context).

- [ ] **Step 3: Decide where to implement route-block for tests**

Because your production routing is in Nacos, but tests run locally without that config, add a minimal in-code WebMVC gateway route config ONLY for the internal-block rule, gated by a property so it doesn't conflict with Nacos.

- [ ] **Step 4: Add minimal route config class in gateway to enforce 404 for `/ymb/internal/**`**

Create:
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/config/InternalRouteBlockConfig.java`

```java
package com.kingdom.yami.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
class InternalRouteBlockConfig {

	@Bean
	@ConditionalOnProperty(prefix = "gateway.routes", name = "blockInternal", havingValue = "true", matchIfMissing = true)
	RouterFunction<ServerResponse> blockInternalRoutes() {
		return route(path("/ymb/internal/**"), req -> ServerResponse.notFound().build());
	}
}
```

- [ ] **Step 5: Re-run the test and confirm it passes**

Run:
```bash
./gradlew :yami-gateway:test --tests "com.kingdom.yami.gateway.routing.InternalRouteBlockTest"
```
Expected: PASS.

- [ ] **Step 6: Commit Task 1**

```bash
git add yami-gateway/src/main/java/com/kingdom/yami/gateway/config/InternalRouteBlockConfig.java \
  yami-gateway/src/test/java/com/kingdom/yami/gateway/routing/InternalRouteBlockTest.java

git commit -m "test: enforce 404 for /ymb/internal routes"
```

---

### Task 2: Consolidate gateway→backend call paths behind LBClient

**Files:**
- Modify: `yami-gateway/src/main/java/com/kingdom/yami/gateway/client/LBClient.java`
- Modify: `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/token/TokenCheckClient.java`
- Modify: `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyRepository.java`

- [ ] **Step 1: Add a focused overload in LBClient for ApiResponse**

In `LBClient.java`, add a helper:

```java
public <T> T postJson(Object body, String url, Class<T> clazz) {
	return restClient.post()
		.uri(url)
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		.body(body)
		.retrieve()
		.body(clazz);
}
```

Keep the existing `call(...)` method for now (compatibility), but implement it by delegating to `postJson(...)` to avoid duplication.

- [ ] **Step 2: Re-run compile**

Run:
```bash
./gradlew :yami-gateway:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ensure TokenCheckClient only depends on LBClient + TokenProperties**

Confirm `TokenCheckClient.checkToken(...)` uses `tokenProperties.checkUrl()` and `new CheckTokenRequest(token)`.

- [ ] **Step 4: Ensure SessionKeyRepository posts correct request DTO**

Confirm `SessionKeyRepository.getSessionKey(sessionId)` posts `new GetEncKeyBySessionIdRequest(sessionId)` and safely handles null `ApiResponse` or null `data()`.

- [ ] **Step 5: Commit Task 2**

```bash
git add yami-gateway/src/main/java/com/kingdom/yami/gateway/client/LBClient.java \
  yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/token/TokenCheckClient.java \
  yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyRepository.java

git commit -m "refactor: unify gateway backend calls via LBClient"
```

---

### Task 3: Remove alternate session-key fetching implementation (if unused)

**Files:**
- Potential delete: `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyClient.java`

- [ ] **Step 1: Verify if SessionKeyClient is referenced**

Run:
```bash
./gradlew :yami-gateway:compileJava
```
Then search (ripgrep):
```bash
rg "SessionKeyClient" -n yami-gateway/src/main/java
```
Expected: ideally no references.

- [ ] **Step 2: Delete SessionKeyClient if unused**

Remove file:
- `yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto/SessionKeyClient.java`

- [ ] **Step 3: Run compile + tests**

```bash
./gradlew :yami-gateway:compileJava :yami-gateway:test
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Task 3**

```bash
git add -A yami-gateway/src/main/java/com/kingdom/yami/gateway/filter/crypto

git commit -m "chore: remove unused session key client"
```

---

### Task 4: Nacos routing change for production parity (manual step)

**Files:**
- Out-of-repo (Nacos): `yami-gateway-dev` group `application.properties`

- [ ] **Step 1: Add a higher priority route to block `/ymb/internal/**`**

Add a route before forwarding routes, with a handler returning 404.

Because WebMVC Gateway route property names can be finicky, verify on your environment by:
1) adding the route
2) restarting gateway
3) calling `/ymb/internal/anything` and confirming 404

- [ ] **Step 2: Verify external behavior remains unchanged**

Smoke checks:
- `/ymb/auth/login` still works
- normal `/ymb/**` forwarding works
- `/ymb/internal/**` returns 404

(No commit; this is configuration.)

---

## Self-review checklist
- Spec coverage: internal route block (404), backend-call consolidation via LBClient, removal of alternate code paths.
- Placeholder scan: none.
- Type consistency: request DTOs are `CheckTokenRequest` and `GetEncKeyBySessionIdRequest` (no inner classes).

---

## Execution options
Plan complete and saved to `docs/superpowers/plans/2026-04-27-yami-gateway-maintainability-refactor.md`.

Two execution options:
1) **Subagent-Driven (recommended)** — use `superpowers:subagent-driven-development`
2) **Inline Execution** — use `superpowers:executing-plans`

Which approach?