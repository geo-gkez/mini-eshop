# mini-eshop — Documentation

Secure e-shop. 

**Stack**: 
- **Spring Boot 4** (REST backend) 
- **Vue 3 / Vuetify** SPA ·
- **nginx** (TLS termination + reverse proxy + static host) 
- **PostgreSQL** (Flyway)
- **Redis** (Spring Session). 

The app is run with the `make up` command, which builds the images and starts all
containers via Docker Compose; the compose files live in `deployments/`, full instructions in `README.md`.

Every claim cites the real source location (`path:line`).

---

## §1 — Functionality of each source file

### Backend — `auth/` (authentication, login, rate limiting)
| File | Responsibility |
|------|----------------|
| `AuthController.java` | REST login/logout-adjacent endpoints: `POST /api/auth/login` (rate-limit → authenticate → rotate session → save context) and `GET /api/auth/me` (returns current user; resolving `CsrfToken` forces the `XSRF-TOKEN` cookie to be written). |
| `AppUserDetailsService.java` | `UserDetailsService` — loads a user by username from the DB; throws `UsernameNotFoundException` if absent. |
| `AppUser.java` | JPA entity for the `users` table (username, `password_hash`, name, email). |
| `AppUserRepository.java` | Spring Data repository: `findByUsername`. |
| `LoginRateLimiter.java` | Redis-backed login throttle: per-username and per-IP failure counters with a sliding window. |
| `LoginRateLimitProperties.java` | Typed config (`app.login-rate-limit`): thresholds + window. |
| `LoginRequest.java` / `UserInfo.java` | Login request / response DTOs. |
| `CurrentUser.java` | `@CurrentUser` annotation → resolves the authenticated principal into controller arguments. |

### Backend — `catalog/` (product listing & search)
| File | Responsibility |
|------|----------------|
| `CatalogController.java` | `GET /api/products` — paged list + optional `search` term (`@Size(max=100)`). |
| `CatalogService.java` | Chooses between "list all" and "search by name"; maps to `ProductSummary`. |
| `ProductRepository.java` | Spring Data queries: `findByNameContainingIgnoreCase`, `findByReference`. |
| `Product.java` | JPA entity for `products` (incl. UUID `reference`, price, currency). |
| `ProductSummary.java` / `CatalogResponse.java` | Read-only DTOs (never expose the numeric DB id; use `reference`). |

### Backend — `cart/` (server-side cart)
| File | Responsibility |
|------|----------------|
| `CartController.java` | `POST /api/cart/items`, view/update/remove — the cart lives server-side, keyed to the user. |
| `CartService.java` | Cart business rules: resolve product by `reference`, add/update quantity, enforce policy. |
| `CartEntity.java` / `CartItemEntity.java` | JPA entities (`carts`, `cart_items`). **Items store only `productReference` + `quantity` — never price.** |
| `CartRepository.java` | `findByUser_Id`. |
| `CartPolicy.java` | Limits (max distinct items / max quantity). |
| `AddItemRequest.java` / `UpdateItemRequest.java` / `CartItem.java` / `CartResponse.java` | Cart DTOs. |
| `CartFullException.java` / `CartItemQuantityException.java` | Domain exceptions → mapped to 4xx. |

### Backend — `order/` (checkout, review, admin email)
| File | Responsibility |
|------|----------------|
| `OrderController.java` | `POST /api/order/submit` (build review) and `POST /api/order/confirm` (place order). |
| `OrderService.java` | Re-reads prices from the DB at submit time (anti price-tampering), persists a `PENDING` order, on confirm marks it confirmed and publishes an event. |
| `OrderEmailSender.java` | After commit, emails the order to the shop admin (plaintext `SimpleMailMessage`). |
| `ShippingAddress.java` | Address DTO with validation incl. CRLF guard (`@Pattern(^[^\r\n]*$)`) — email-header-injection defense. |
| `OrderReview.java` / `OrderLine.java` | Review payload + per-line price (price taken from the `Product` entity, not the client). |
| `OrderEntity.java` / `OrderLineEntity.java` | JPA entities (`orders`, `order_lines`). |
| `OrderRepository.java` | `findByUser_IdAndStatus`. |
| `OrderConfirmedEvent.java` | Event published after the checkout transaction commits. |
| `OrderProperties.java` | Typed config: admin/from email addresses (validated `@Email`). |

### Backend — `config/` (security, session, web, logging)
| File | Responsibility |
|------|----------------|
| `SecurityConfig.java` | The security filter chain: authorization (default-deny), CSRF (`.spa()`), session concurrency, logout, password encoder, `AuthenticationManager`, session-authentication strategy. |
| `SessionConfig.java` | Redis session serializer — JSON (not JDK), restricted by a `PolymorphicTypeValidator`. |
| `SessionProperties.java` | Typed config: absolute session timeout. |
| `AbsoluteSessionTimeoutInterceptor.java` | Invalidates a session older than the absolute cap regardless of activity. |
| `JsonAuthHandlers.java` | `AuthenticationEntryPoint` + `AccessDeniedHandler` → JSON 401/403 (no redirects for the SPA). |
| `ProblemDetailWriter.java` | Writes RFC-9457 `ProblemDetail` JSON responses. |
| `GlobalExceptionHandler.java` | Maps exceptions to responses; collapses all `AuthenticationException`s to a generic 401. |
| `WebConfig.java` | Registers the absolute-timeout interceptor and the `@CurrentUser` argument resolver. |
| `SecurityEventLogger.java` | Structured logging of Spring Security auth success/failure events. |

### Backend — `shared/` & entry point
| File | Responsibility |
|------|----------------|
| `EshopApplication.java` | Spring Boot entry point. |
| `shared/LogFields.java` | Constants for structured log keys/events. |
| `shared/PageMeta.java` | Pagination metadata DTO. |

### Frontend (`frontend/src/`)
| File | Responsibility |
|------|----------------|
| `App.vue` | Shell + SPA view switch (Login / Catalog / Cart / Order); app-bar cart badge; handles `auth:expired`. |
| `views/LoginView.vue` | Login form → `POST /auth/login`. |
| `views/CatalogView.vue` | Product grid, search, "all vs search results" indicator, add-to-cart, cart-count summary. |
| `views/CartView.vue` | Cart contents view. |
| `views/OrderView.vue` | Checkout step machine: address form → review → done (no page change). |
| `api/client.js` | `fetch` wrapper: injects the CSRF header on mutating requests, handles 401 → `auth:expired`. |
| `main.js` / `plugins/vuetify.js` | App bootstrap and Vuetify setup. |

### Infrastructure & database
| File | Responsibility |
|------|----------------|
| `nginx/nginx.conf` | TLS termination, HTTP→HTTPS redirect, security headers/CSP, edge rate limiting, reverse proxy to backend, SPA fallback. |
| `nginx/gen-cert.sh` | Generates the self-signed certificate. |
| `nginx/Dockerfile` | nginx image build. |
| `deployments/` (`docker-compose*.yml`) + `Makefile` | Service orchestration (nginx, backend, postgres, redis, mail): base stack plus dev/prod overlays, driven by `make up`/`dev`/`prod`. |
| `db/migration/V1__schema.sql` | Tables: `users`, `products`, `orders`, `order_lines`. |
| `db/migration/V2__seed.sql` | Product seed data (via Flyway). |
| `db/migration/V3__cart.sql` | `carts` / `cart_items` tables. |
| `scripts/seed-users.sql` | Manual demo-user seed (`alice`/`bob`), applied via `make seed` — not run by Flyway. |

---

## §2 — Session-data management

Authentication is **stateful, server-side session** backed by **Spring Session + Redis**: the
session state (the `SecurityContext`) is stored server-side in Redis, and the browser holds only an
opaque session cookie named **`SESSION`** that references it. There is no JWT (stateless token).

**Session-ID generation & rotation.** Login does not use a built-in filter — `formLogin` and
`httpBasic` are disabled (`SecurityConfig.java:103-104`). `AuthController.login` therefore
**explicitly** invokes the `SessionAuthenticationStrategy` (`AuthController.java:52`), which is a
`CompositeSessionAuthenticationStrategy` (`SecurityConfig.java:164-173`) running, in order:
1. `ConcurrentSessionControlAuthenticationStrategy` — enforces the one-session cap;
2. `ChangeSessionIdAuthenticationStrategy` — **rotates the session id on login (session-fixation defense)**;
3. `RegisterSessionAuthenticationStrategy` — registers the new session.

The session id itself is generated by the servlet container / Spring Session (cryptographically
random), not by application code.

**Cookie attributes** (`application.yml:76-78`): `http-only: true`, `secure: true`,
`same-site: strict`. So the cookie is unreadable to JavaScript, sent only over HTTPS, and not
attached to cross-site requests.

**Timeouts — two independent caps:**
- **Idle timeout 15m** — `server.servlet.session.timeout: 15m` (`application.yml:79`); resets on activity.
- **Absolute timeout 8h** — `app.session.absolute-timeout: 8h` (`application.yml:6`), enforced by
  `AbsoluteSessionTimeoutInterceptor.java:37-42`, which invalidates a session older than the cap
  **regardless of activity** (caps the value of a stolen cookie).

**Concurrent sessions.** `maximumSessions(1)` (`SecurityConfig.java:75-87`) with a
`SpringSessionBackedSessionRegistry` (`SecurityConfig.java:153-157`, requires the indexed Redis
repository, `application.yml:22-28`). A second login **expires the oldest** session; that session's
next request gets a 401 ("logged in from another device", `SecurityConfig.java:86`).

**Logout** (`SecurityConfig.java:90-102`): `POST /api/auth/logout` invalidates the session and
emits `Clear-Site-Data: cookies`.

**Serialization.** Session data is stored in Redis as **JSON, not JDK-serialized**
(`SessionConfig.java:20-23`), and deserialization is constrained by a
`BasicPolymorphicTypeValidator` plus the Spring Security Jackson modules
(`SessionConfig.java:25-32`) — closing the deserialization-gadget surface.

---

## §3 — Preventing user enumeration on the Login page

The login response is **identical whether the username exists or not**, on three axes:

1. **Same error, same status.** `AppUserDetailsService` throws `UsernameNotFoundException` for an
   unknown user (`AppUserDetailsService.java:22-25`). The stock `DaoAuthenticationProvider`
   (`SecurityConfig.java:128`) keeps **`hideUserNotFoundExceptions` at its default `true`**, so an
   unknown user is reported as `BadCredentialsException` — the same exception as a wrong password.
   `GlobalExceptionHandler.java:20-28` then maps **every** `AuthenticationException` to a generic
   `401 Unauthorized` ProblemDetail with no distinguishing detail. *(This is the framework default,
   not an explicit setter.)*

2. **Constant-time-ish response.** The same stock provider performs a **dummy password hash when
   the user is not found** (Spring's `mitigateAgainstTimingAttack`), so "no such user" and "wrong
   password" take comparable time — no timing oracle.

3. **The 429 response does not reveal whether an account exists.** `LoginRateLimiter` is checked **before** authentication
   (`AuthController.java:38`) and, on failure, increments **both** the per-username and per-IP
   counters **symmetrically — regardless of whether the username exists**
   (`LoginRateLimiter.java:43-45`, called from `AuthController.java:62`). An attacker therefore
   cannot use throttling behavior to tell real usernames from fake ones. Thresholds: 10/username,
   20/IP, 15-minute window (`LoginRateLimitProperties.java:13-15`).

---

## §4 — How user passwords are protected in the database

Passwords are stored **only as Argon2id hashes**, never in plaintext or reversible form.

- **Encoder** (`SecurityConfig.java:142-150`): a `DelegatingPasswordEncoder` whose default id is
  `argon2@SpringSecurity_v5_8`, backed by `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`.
  These are the **Spring Security v5.8 default Argon2id parameters** (no custom memory/iterations/
  parallelism were set). The `{argon2@SpringSecurity_v5_8}` prefix on each stored hash makes future
  algorithm upgrades possible without touching existing rows.
- **Storage** (`db/migration/V1__schema.sql:4`): `password_hash VARCHAR(255) NOT NULL`. Argon2
  embeds a random salt in each generated hash (inside the encoded string), so no separate salt
  column is needed.
- **Verification**: login goes through `DaoAuthenticationProvider` (`SecurityConfig.java:128-129`),
  which calls `PasswordEncoder.matches(raw, storedHash)` — the raw password is hashed and compared;
  the stored hash is never decrypted.

---

## §5 — SQL parameters and SQL-injection protection

**Every** database query in the app is a **Spring Data derived query** (method-name query). There
is **no hand-written SQL, no `@Query`, and no native query** anywhere in the repositories — so
Spring Data / Hibernate generates **parameterized prepared statements** and binds every value;
user input is never concatenated into SQL text.

| Repository (`file:line`) | Method | User-controlled parameter | How it's bound / safe |
|--------------------------|--------|---------------------------|-----------------------|
| `auth/AppUserRepository.java:8` | `findByUsername(String)` | login `username` | bound parameter in a derived query |
| `catalog/ProductRepository.java:12` | `findByNameContainingIgnoreCase(String, Pageable)` | search `term` (`@Size(max=100)`) | bound `LIKE` parameter; the `%…%` wrapping is applied by Hibernate, not by string concat |
| `catalog/ProductRepository.java:14` | `findByReference(UUID)` | product `reference` | typed as `UUID` — a non-UUID input is rejected before any query runs |
| `cart/CartRepository.java:9` | `findByUser_Id(Long)` | (not request input) `userId` from the authenticated principal | bound parameter |
| `order/OrderRepository.java:9` | `findByUser_IdAndStatus(Long, String)` | `userId` from principal; `status` is the **application constant `"PENDING"`** (`OrderService.java:26,59,76`), never request input | bound parameters |

So the only **request-supplied** values reaching SQL are the **login username**, the **search
term**, and the **product reference (UUID)** — all bound as prepared-statement parameters.

---

## §6 — CSRF protection

CSRF protection is **enabled** via Spring Security's SPA preset: `.csrf(CsrfConfigurer::spa)`
(`SecurityConfig.java:89`). That configures:

- a **`CookieCsrfTokenRepository`** — the token is delivered to the browser in a readable
  `XSRF-TOKEN` cookie, and
- a **`SpaCsrfTokenRequestHandler`** — the server expects the token echoed back in the
  **`X-XSRF-TOKEN` request header** (the classic double-submit-cookie pattern for SPAs).

**Frontend side** (`api/client.js:1-25`): for every non-GET/HEAD request, the client reads the
`XSRF-TOKEN` cookie and sends its value as the `X-XSRF-TOKEN` header; if the token is missing it
refuses to send the request. `GET /api/auth/me` resolves a `CsrfToken` argument
(`AuthController.java:68-69`) specifically to force the cookie to be (re)issued.

**Why this is safe even though the XSRF cookie is JavaScript-readable:** a cross-site attacker
**cannot read** the victim's `XSRF-TOKEN` cookie (Same-Origin Policy) and therefore cannot set the
matching `X-XSRF-TOKEN` header. The defense is reinforced by the session cookie being
**`SameSite=strict`** (`application.yml:78`), which already prevents the browser from attaching
credentials to cross-site requests. Mutating endpoints are POST-only, so simple `GET` CSRF cannot
trigger them.

---

## §7 — XSS protection (which functionality, and the measures)

**Where user/data-driven content is rendered** — the catalog/search results, the cart, and the
order-review screen (`CatalogView.vue`, `CartView.vue`, `OrderView.vue`) — output is protected by:

1. **Framework auto-escaping.** All dynamic content uses Vue text interpolation (`{{ }}` /
   `v-text`), which HTML-escapes by default. **There is no `v-html` anywhere in the frontend**
   (verified: `grep -rn "v-html" frontend/src` → no matches), so there is no raw-HTML sink.
2. **Content-Security-Policy + hardening headers** (`nginx/nginx.conf:47-53`), sent on every
   response: `default-src 'self'`, `script-src 'self'` (no inline/remote scripts; blocks injected
   `<script>`), `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`,
   `upgrade-insecure-requests`, plus `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY`.
   (`style-src` allows `'unsafe-inline'` only because Vuetify injects runtime styles — it does **not**
   permit inline scripts.)
3. **Output into the admin email — header-injection defense.** The order email is the one place
   user data leaves the web context. The shipping-address fields are validated with
   `@Pattern(regexp = "^[^\\r\\n]*$")` on all four fields (`order/ShippingAddress.java:8-11`),
   preventing CR/LF email-header injection, and the email **subject** is additionally stripped of
   CR/LF (`OrderEmailSender.java:75,85-87`). The email body is plaintext, not HTML, so it is not an
   HTML-XSS sink.

---

## §8 — Significant risks with no implemented mitigation (honest disclosure)

These are known, conscious residual risks — acknowledged rather than silently ignored:

1. **Order email transport is not TLS-enforced in the default (dev) profile.** The admin email is a
   plaintext `SimpleMailMessage` (`OrderEmailSender.java:52-78`). In the default profile the target
   is the local **Mailpit** dev sink, which has no TLS, so the hop is unencrypted — by design for the
   demo. The `prod` profile (`application-prod.yml`, activated via `make prod`) **enforces**
   `mail.smtp.auth=true` and `mail.smtp.starttls.required=true`, so the app *refuses* to send unless
   the SMTP server negotiates TLS. The residual risk is therefore confined to the dev profile.
2. **No per-account lockout / CAPTCHA.** Brute-force defense is rate-limiting only
   (`LoginRateLimiter`) plus nginx edge limits. There is no durable account lockout or human-
   verification challenge, so a slow distributed attack under the thresholds is not stopped.
3. **Rate-limiter `INCR`/`EXPIRE` edge case.** A crash between the Redis `INCR` and `EXPIRE` could
   leave a counter without a TTL (already noted in `LoginRateLimiter.java`). Acceptable for the
   single-instance scope; not hardened.
4. **Self-signed certificate.** Per the assignment, TLS uses a self-generated certificate
   (`nginx/gen-cert.sh`), which is not CA-trusted — browsers warn, and it does not protect against
   an active MITM who can present their own untrusted cert that a user clicks through.
5. **Single-instance assumptions.** Some controls (the in-memory-style session/rate-limit reasoning)
   assume one backend instance; horizontal scaling would need review (Redis already helps, but this
   was not load-tested).
6. **No app-level anti-automation on read endpoints, and no WAF.** Beyond the nginx edge limits,
   read surfaces (e.g. catalog/search) have no application-level throttling, and there is no WAF —
   out of scope for this assignment but worth stating.

---

### Appendix — running the app

The app runs in containers via Docker Compose, with a helper `Makefile`. Short version (full steps
in `README.md`):

1. `cp .env.example .env` and fill in `DB_PASSWORD` / `REDIS_PASSWORD`.
2. `make certs` — generate the self-signed certificate.
3. `make up` — build and start the stack: nginx (443, self-signed), backend, PostgreSQL (Flyway
   migrations + *product* seed), Redis, and the local Mailpit sink.
4. `make seed` — load the demo users (`alice`/`alicepass`, `bob`/`bobpass`); these are **not**
   created by Flyway.

Browse `https://mini-eshop.local/` (accept the self-signed cert) and log in with a demo account.

`make` is not required — each target maps to a single `docker compose` command (see the equivalence
table in `README.md`).
