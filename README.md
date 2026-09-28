
# Gatekeeper: multi-tenant API gateway control plane

Manage gateway routes, header policies and access across organizations, with role-based login.

```
gatekeeper/
├── api-management-service/   Spring Boot 8081, auth, tenants, routes, audit  (MySQL + Flyway)
├── smart-gateway-service/    Spring Boot 8080, gateway skeleton (health only, see "Scope")
├── analytics-service/        Spring Boot 8082, analytics skeleton (health only, see "Scope")
├── frontend/                 React (Vite) web UI
├── scripts/smoke-test.sh     end-to-end auth/authorization checks
└── docker-compose.yml        MySQL + management API + web UI
```

## Roles

| Role | Who | Can do |
|---|---|---|
| `PLATFORM_ADMIN` | You / operators | Everything: all organizations, users, routes (per organization), audit log |
| `TENANT_ADMIN` | A customer organization | Manage **only their own** routes, view their own audit log |
| `GATEWAY_SERVICE` | The gateway (machine account) | Read the active route table from `/internal/routes`. Nothing else |

Self-service sign-up (`/register`) creates an organization and its `TENANT_ADMIN`. Platform admin and gateway accounts come from the Flyway seed migration.

---

## 1. Run it with Docker (fastest)

Requires Docker Desktop.

```bash
cd gatekeeper
cp .env.example .env
docker compose up --build
```

First build takes a few minutes (Maven downloads dependencies). When you see `Started ApiManagementServiceApplication`, open **http://localhost:3000**.

| Account | Username | Password |
|---|---|---|
| Platform admin | `platform-admin` | `Admin@12345` |
| Demo tenant admin | `demo-tenant` | `Tenant@12345` |
| Gateway service | `gateway-service` | `Gateway@12345` |

Stop with `Ctrl+C`; `docker compose down -v` also wipes the database.

## 2. Run it locally (IntelliJ + npm)

**Prerequisites:** JDK 21, MySQL 8 running on `localhost:3306`, Node 20+.

**a) Database**: create the schema (Flyway builds the tables on first start):
```sql
CREATE DATABASE gatekeeper_management;
```
The default connection is `root` / `123456` (your existing setting). Override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

**b) Backend (IntelliJ)**
1. `File → Open` → select the `gatekeeper` folder, and let IntelliJ import the Maven projects.
2. Open `ApiManagementServiceApplication` → `Edit Configurations` → set **Active profiles** to `dev`.
   The `dev` profile sets the demo passwords above and creates the demo tenant. **Never use it in production.**
3. Run it. Wait for `Started ApiManagementServiceApplication` on port 8081.

Or from a terminal: `cd api-management-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

**c) Frontend**: run `npm install` **inside the `frontend` folder** (the one containing `package.json`):
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**. The dev server proxies `/api` to `localhost:8081`, so no CORS setup is needed.

**d) Optional**: start `smart-gateway-service` (8080) and `analytics-service` (8082) so their dashboard health cards turn green. (`analytics-service` needs MongoDB and Kafka available.)

---

## 3. Test it

### Automated (backend)
With the backend running using demo passwords:
```bash
./scripts/smoke-test.sh            # or: ./scripts/smoke-test.sh http://localhost:8081
```
Checks 401/403/404/409 behaviour, weak-password rejection, tenant isolation, role boundaries and route lifecycle. Expect `Result: N passed, 0 failed`.

### Manual UI walkthrough
1. **Auth**: open the app while signed out and you land on Sign in. Wrong password shows an error; 5 wrong attempts lock that username for ~10 minutes.
2. **Tenant flow**: sign in as `demo-tenant`. Sidebar shows Dashboard, Routes, Audit log only. Create a route (it starts *inactive*), toggle it on, edit it, open **History** to see versions.
3. **Isolation**: register a second organization at `/register`. It must not see Acme's routes.
4. **Platform admin**: sign in as `platform-admin`. You also see Organizations and Users. Suspend an organization, then try signing in as its user and you should be refused. Reactivate it.
5. **Gateway account**: sign in as `gateway-service`. You only see **Active config**: the JSON the gateway would consume.
6. **Audit**: every login, sign-up, route change and admin action appears in the Audit log.

### Manual API (curl)
```bash
TOKEN=$(curl -s -X POST localhost:8081/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"demo-tenant","password":"Tenant@12345"}' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
curl -s localhost:8081/routes -H "Authorization: Bearer $TOKEN"
```

## API reference (api-management-service)

| Method & path | Roles | Purpose |
|---|---|---|
| `GET /health` | public | Service + DB status |
| `POST /auth/login` | public | Returns a JWT + user |
| `POST /auth/register` | public | Create organization + tenant admin |
| `GET /auth/me` | any signed-in | Current user |
| `GET /routes` | tenant, platform | List routes (`?organizationId=` for platform) |
| `POST /routes` | tenant, platform | Create (inactive) |
| `GET/PUT/DELETE /routes/{routeId}` | tenant, platform | Read / update / delete |
| `POST /routes/{routeId}/activate` `/deactivate` | tenant, platform | Toggle |
| `GET /routes/{routeId}/versions` | tenant, platform | Change history |
| `GET /stats`, `GET /audit` | tenant, platform | Dashboard counts, audit trail (tenant sees own org) |
| `GET /admin/organizations`, `PATCH /admin/organizations/{id}/status` | platform | List / suspend / reactivate |
| `GET /admin/users`, `PATCH /admin/users/{id}/enabled` | platform | List / enable / disable |
| `GET /internal/routes` | gateway | Active routes of active organizations |

Platform admins must pass `?organizationId=` when creating/updating a route, because route IDs are only unique within an organization. Errors share one shape: `{timestamp, status, error, message, details[]}`.

## Security notes and production checklist

Implemented: BCrypt password hashing, signed JWT access tokens (HS256, 60 min), stateless API, role checks enforced server-side (the UI hiding menu items is convenience only), tenant scoping derived from the token and never from client input, login brute-force lockout, disabled-user and suspended-organization enforcement, input validation, audit trail, non-root Docker image.

Before going live:
- [ ] Set a strong `GATEKEEPER_JWT_SECRET` (`openssl rand -base64 48`). Changing it signs everyone out.
- [ ] Set `PLATFORM_ADMIN_PASSWORD` / `GATEWAY_SERVICE_PASSWORD` to strong values, then clear them and `GATEKEEPER_DEMO_DATA` after first start.
- [ ] Change the MySQL root password; use a dedicated DB user.
- [ ] Serve over **HTTPS** (a load balancer or reverse proxy in front of nginx).
- [ ] Lock `GATEKEEPER_CORS_ORIGINS` to your real frontend origin (only matters if the UI is on a different origin from the API).
- [ ] The access token is kept in `sessionStorage`, which is XSS-exposed. For higher assurance move to short-lived tokens plus an HttpOnly refresh cookie.
- [ ] The login lockout is in-memory (per instance). With multiple instances, move it to Redis.
- [ ] Tenants can point routes at any `http(s)` URL. Add an allow/deny list for internal address ranges (SSRF) once the gateway starts forwarding traffic.
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (already the default) and manage schema only through Flyway.

## Scope: what is and isn't built

Built and wired end to end: authentication, authorization, tenants, users, route management with versioning, audit log, admin tools, the web UI, Docker deployment.

**Not built yet:** `smart-gateway-service` and `analytics-service` remain the health-only skeletons from your course. The gateway does not yet proxy traffic, validate tokens at the edge, or enforce the `requests_per_minute` limits stored on routes. It is meant to log in as `gateway-service` and pull `/internal/routes`, which is ready for it. Analytics ingestion and usage charts are the next slice.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Communications link failure` on backend start | MySQL isn't running / wrong `DB_URL` |
| `Schema-validation: wrong column type` | Database was created by an older schema. Drop and recreate `gatekeeper_management`, and Flyway rebuilds it |
| Can't log in as `platform-admin` | Start with the `dev` profile (or set `PLATFORM_ADMIN_PASSWORD`). The seeded hash's plaintext password isn't documented |
| UI says "Cannot reach the server" | Backend isn't up on 8081, or `npm run dev` wasn't restarted after config changes |
| `Too many failed sign-in attempts` | Wait ~10 minutes, or restart the backend |
| Gateway/Analytics cards show "down" | Expected until those services are running |
