# mini-eshop

A secure e-shop (Assignment 3): authenticated product catalog, cart, and an order/checkout flow
that emails the shop administrator. Built with defense-in-depth at every layer.

**Stack:** Spring Boot 4 (REST backend) · Vue 3 / Vuetify (SPA) · nginx (TLS termination, reverse
proxy, static hosting) · PostgreSQL (Flyway) · Redis (Spring Session). Everything runs in
containers via Docker Compose, driven by a `Makefile`.

> Security documentation (the 8 required points) is in [`DOCUMENTATION.pdf`](DOCUMENTATION.pdf)
> (source: `DOCUMENTATION.tex` / `DOCUMENTATION.md`).

---

## Prerequisites

- **Docker Engine + Docker Compose v2** (`docker compose`, not the old `docker-compose`).
- **make** and **openssl** (for the self-signed cert).
- Free ports on the host: **443** and **80** (app), **8025** (Mailpit), and for `make dev` also
  **5540** / **5050**.

---

## Quick start

```sh
# 1. Configuration: copy the template and fill in the secrets.
cp .env.example .env
#    Required: DB_PASSWORD, REDIS_PASSWORD.  Recommended: SHOP_ADMIN_EMAIL.

# 2. Hostname: the TLS cert is issued for mini-eshop.local — point it at localhost.
echo '127.0.0.1  mini-eshop.local' | sudo tee -a /etc/hosts

# 3. Generate the self-signed TLS certificate (once).
make certs

# 4. Build the images and start the full stack (detached).
make up

# 5. Load the demo users.
make seed
```

Then open **https://mini-eshop.local** and accept the self-signed-certificate warning
(`localhost` also works). Log in with one of the seeded accounts:

| Username | Password |
|----------|----------|
| `alice`  | `alicepass` |
| `bob`    | `bobpass`   |

To stop: `make down` (keeps data) or `make clean` (removes the Postgres + Redis volumes).

---

## Make targets

Run `make` (or `make help`) to list them:

| Target | What it does |
|--------|--------------|
| `make certs` | Generate the self-signed TLS cert for nginx (run once). |
| `make up`    | Build images and start the full stack, detached. |
| `make seed`  | Load demo users (`alice`/`bob`) — run after `make up`. |
| `make dev`   | Start the stack **plus** dev UIs (RedisInsight, pgAdmin). |
| `make prod`  | Start the stack with the real-email (`prod`) profile. |
| `make logs`  | Tail the backend logs. |
| `make ps`    | Show service status. |
| `make down`  | Stop the stack (data preserved). |
| `make clean` | Stop and **delete** the Postgres + Redis volumes. |

---

## URLs & ports

| Service | URL | Notes |
|---------|-----|-------|
| App (SPA + API) | `https://mini-eshop.local` | HTTP `:80` redirects to HTTPS `:443`. |
| Mailpit (order emails, dev) | `http://localhost:8025` | Web inbox catching the admin notification. |
| RedisInsight (`make dev`) | `http://localhost:5540` | |
| pgAdmin (`make dev`) | `http://localhost:5050` | Login = `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` from `.env`. |

With the default stack, the order-confirmation email lands in **Mailpit** (`:8025`) — that's how you
verify the checkout flow without sending real mail.

---

## Real email (`make prod`)

`make up` sends order emails to Mailpit (no real delivery). To send **real, TLS-enforced** email,
use the `prod` profile, which activates `application-prod.yml` (forces SMTP auth + STARTTLS, so the
app refuses to send unless the server negotiates TLS).

For Gmail:

1. On the Google account: turn on **2-Step Verification**, then create a 16-character **App
   Password** (Google Account → Security → App passwords). Your normal password will not work.
2. Add to `.env`:
   ```sh
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USER=youraddress@gmail.com
   MAIL_PASSWORD=your-16-char-app-password
   SHOP_ADMIN_EMAIL=where-order-notifications-go@example.com
   ```
3. `make prod` (run `make certs` and `make seed` as in the quick start if you haven't).

> Because STARTTLS is **required** in this profile, a wrong app password or a blocked port 587 makes
> the order email fail loudly instead of silently — correct, but worth knowing for a live demo.

---

## Project layout

```
backend/        Spring Boot REST API (auth, catalog, cart, order, config)
frontend/       Vue 3 / Vuetify SPA
nginx/          TLS termination, reverse proxy, CSP/security headers, gen-cert.sh
deployments/    Docker Compose files: base stack + dev/prod overlays
scripts/        seed-users.sql (demo accounts)
Makefile        build & run helpers
DOCUMENTATION.* security documentation (PDF + sources)
.env.example    configuration template — copy to .env
```

---

## Troubleshooting

- **Browser cert warning** — expected for a self-signed cert; proceed past it.
- **Port already in use** — stop whatever holds `:443`/`:80`/`:8025`, or change the published ports
  in `deployments/docker-compose.yml`.
- **Login fails right after `make up`** — run `make seed` (users aren't created automatically).
- **Running compose by hand** — always pass `--project-directory .` from the repo root, or the
  relative `build:` and volume paths (and `.env`) won't resolve. The Makefile does this for you.
