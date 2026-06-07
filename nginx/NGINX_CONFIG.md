# nginx Configuration — Security & Performance Rationale

nginx terminates TLS and is the only component the browser talks to directly.
Spring Boot runs plain HTTP on port 8080 inside the Docker network.

---

## TLS

```nginx
ssl_protocols       TLSv1.2 TLSv1.3;
ssl_ciphers         HIGH:!aNULL:!MD5;
ssl_prefer_server_ciphers on;
```

- TLS 1.0 and 1.1 are disabled — both are deprecated (RFC 8996) and have known attacks (BEAST, POODLE).
- `HIGH:!aNULL:!MD5` excludes anonymous (no auth) and MD5-based suites.
- `ssl_prefer_server_ciphers on` forces the stronger suite when the client offers both a weak and a strong option.

**Self-signed cert**: generated with `gen-cert.sh` into `nginx/certs/` (git-ignored).
Browsers will show an untrusted-cert warning — expected and acceptable for a demo.

**TLS session cache** (`ssl_session_cache shared:SSL:10m`): allows session resumption, saving a
full TLS handshake on reconnects. Performance only, no security impact.

---

## HTTP → HTTPS redirect

```nginx
server {
    listen 80;
    return 301 https://$host$request_uri;
}
```

Hard redirect, no content served over HTTP. Browsers that land on port 80 are
immediately sent to HTTPS. HSTS then trains the browser to never try port 80 again.

---

## Security Headers

### Strict-Transport-Security (HSTS)

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

Tells the browser: for the next year, only connect to this domain over HTTPS — even if the
user types `http://`. Prevents SSL-stripping attacks (an attacker intercepts the first HTTP
request before the redirect happens).

**Why 1 year, not "forever"?**
`max-age` is always finite. The real "forever" mechanism is the HSTS Preload list
(`hstspreload.org`), which hardcodes domains into browser source code. That requires:
a CA-signed cert (not self-signed), `includeSubDomains`, and the `preload` directive.

1 year is the preload minimum and a standard production value. A shorter `max-age` is safer
during development — if the cert breaks or you need to test over HTTP, you are locked out
for the entire duration with no override. Ramp-up recommendation:
- Testing: `max-age=300`
- Stabilising: `max-age=2592000` (30 days)
- Production lock: `max-age=31536000`

We do **not** add `preload` — self-signed certs are not eligible for the preload list.

### X-Content-Type-Options

```nginx
add_header X-Content-Type-Options "nosniff" always;
```

Prevents the browser from guessing a MIME type different from what the server declares.
Without this, a browser might execute a file declared as `text/plain` if it looks like JS.
Mitigates some content-sniffing XSS vectors.

### X-Frame-Options

```nginx
add_header X-Frame-Options "DENY" always;
```

Prevents this page from being embedded in an `<iframe>` on another domain — the classic
clickjacking defence. `DENY` is stricter than `SAMEORIGIN` (blocks framing from all origins,
including our own).

Modern browsers use `frame-ancestors 'none'` in CSP for the same effect — we include both
for compatibility with older browsers that don't parse CSP.

> **Note on `X-XSS-Protection`:** intentionally **not** set. It controls a legacy, non-standard
> XSS auditor that modern browsers have removed, and that could itself introduce XSS in some cases
> (per MDN it is deprecated; CSP is the recommended replacement). Our CSP already disables inline
> scripts (`script-src 'self'`), so the header would add nothing. Spring also emits `X-XSS-Protection: 0`
> on `/api` responses by default.

### Server header (server_tokens)

```nginx
server_tokens off;   # in the http {} block
```

Strips the nginx **version** from the `Server` response header (`Server: nginx` instead of
`Server: nginx/1.27.x`) and from generated error pages. Reduces version fingerprinting that would
let an attacker target known-CVE versions — a small Security-Misconfiguration / information-disclosure
hardening. It does not hide that nginx is in use, only the version.

### Referrer-Policy

```nginx
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

Controls how much of the URL is sent in the `Referer` header when the user navigates to
another site. `strict-origin-when-cross-origin` sends only the scheme+host (not the full path)
on cross-origin requests. Prevents leaking session-related query params or path fragments.

### Permissions-Policy

```nginx
add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;
```

Explicitly disables browser features this application does not use. Even if a future XSS
payload tries to access the camera or geolocation, the browser will refuse.

### Content Security Policy

```nginx
add_header Content-Security-Policy
    "default-src 'self';
     script-src 'self';
     style-src 'self' 'unsafe-inline';
     img-src 'self' data:;
     font-src 'self';
     connect-src 'self';
     object-src 'none';
     base-uri 'self';
     form-action 'self';
     frame-ancestors 'none';
     upgrade-insecure-requests;"
    always;
```

| Directive | Value | Reason |
|---|---|---|
| `default-src` | `'self'` | Deny everything not explicitly listed |
| `script-src` | `'self'` | Only load JS from our own origin; Vite build has no inline scripts |
| `style-src` | `'self' 'unsafe-inline'` | Vuetify injects inline styles at runtime — cannot avoid without CSP nonces + SSR |
| `img-src` | `'self' data:` | Allow inline SVG/base64 images used by Vuetify icons |
| `font-src` | `'self'` | MDI icon font is bundled and served from our origin |
| `connect-src` | `'self'` | `fetch()` calls go to `/api/*` — same origin through nginx |
| `object-src` | `'none'` | Block Flash/plugins entirely |
| `base-uri` | `'self'` | Prevent `<base>` tag injection (could redirect relative URLs) |
| `form-action` | `'self'` | Forms can only submit to same origin |
| `frame-ancestors` | `'none'` | Modern equivalent of `X-Frame-Options: DENY` |
| `upgrade-insecure-requests` | — | Browser upgrades any accidental `http://` subresource requests |

**Known weakness**: `style-src 'unsafe-inline'` weakens the CSP against style-injection attacks.
Fixing it properly requires CSP nonces, which need server-side rendering of the HTML to inject
a per-request nonce into `<style>` tags. For a Vite SPA with a static `index.html` this is not
straightforward. The trade-off is documented and accepted for this demo.

---

## API Proxy

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header X-Forwarded-Proto https;
}
```

- Single-origin design: browser talks to `https://mini-eshop.local`; nginx forwards `/api/*`
  to Spring Boot internally. No CORS configuration needed.
- `X-Forwarded-Proto: https` tells Spring Boot the original request was HTTPS.
  Spring reads this via `server.forward-headers-strategy: native` (Tomcat `RemoteIpValve`),
  which is why the `JSESSIONID` cookie gets `Secure` set correctly even though the
  nginx→Spring hop is plain HTTP.

---

## SPA Fallback

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

Serves the built Vue app. If the path doesn't match a static file (e.g. a deep link like
`/catalog`), falls back to `index.html` so Vue Router can handle client-side routing.

---

## Self-signed Certificate Generation

Run once from the `nginx/` directory before `docker compose up`:

```bash
bash gen-cert.sh
```

The script creates `nginx/certs/server.crt` and `nginx/certs/server.key`.
These are git-ignored (`nginx/certs/*`). The docker-compose mounts `nginx/certs/` read-only
into the nginx container at `/etc/nginx/certs/`.
