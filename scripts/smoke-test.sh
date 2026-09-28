#!/usr/bin/env bash
# End-to-end check of authentication, authorization and tenant isolation.
# Usage: ./scripts/smoke-test.sh [base-url]   (default http://localhost:8081)
# Needs the backend running with demo passwords (dev profile or docker compose defaults).
set -u
BASE="${1:-http://localhost:8081}"
ADMIN_PW="${PLATFORM_ADMIN_PASSWORD:-Admin@12345}"
GW_PW="${GATEWAY_SERVICE_PASSWORD:-Gateway@12345}"
PASS=0; FAIL=0
SUFFIX="$(date +%s)"

status() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
token()  { curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
             -d "{\"username\":\"$1\",\"password\":\"$2\"}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'; }
check()  { # name expected actual
  if [ "$2" = "$3" ]; then echo "  PASS  $1"; PASS=$((PASS+1)); else echo "  FAIL  $1 (expected $2, got $3)"; FAIL=$((FAIL+1)); fi; }

ROUTE='{"routeId":"smoke-route","name":"Smoke","pathPattern":"/smoke/**","targetBaseUrl":"https://httpbin.org","targetPathPrefix":"/anything","requestsPerMinute":60,"responseTimeoutMs":5000,"idempotencyEnabled":false,"allowedMethods":["GET"],"headerRules":[]}'

echo "Health & authentication"
check "GET /health is public"                200 "$(status "$BASE/health")"
check "GET /routes without token -> 401"     401 "$(status "$BASE/routes")"
check "Login with wrong password -> 401"    401 "$(status -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"platform-admin","password":"nope"}')"
check "Tampered token -> 401"                401 "$(status "$BASE/routes" -H 'Authorization: Bearer abc.def.ghi')"

ADMIN="$(token platform-admin "$ADMIN_PW")"
check "Platform admin can log in"            yes "$([ -n "$ADMIN" ] && echo yes || echo no)"
GW="$(token gateway-service "$GW_PW")"
check "Gateway service account can log in"   yes "$([ -n "$GW" ] && echo yes || echo no)"

echo "Tenant onboarding & isolation"
REG() { curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
          -d "{\"organizationName\":\"$1\",\"username\":\"$2\",\"password\":\"Passw0rdX\"}"; }
check "Register tenant A -> 201"             201 "$(REG "Smoke A $SUFFIX" "smoke-a-$SUFFIX")"
check "Register tenant B -> 201"             201 "$(REG "Smoke B $SUFFIX" "smoke-b-$SUFFIX")"
check "Weak password rejected -> 400"        400 "$(status -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d '{"organizationName":"Weak Co","username":"weakuser","password":"short"}')"
A="$(token "smoke-a-$SUFFIX" Passw0rdX)"; B="$(token "smoke-b-$SUFFIX" Passw0rdX)"

echo "Routes (tenant A)"
check "Create route -> 201"                  201 "$(status -X POST "$BASE/routes" -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d "$ROUTE")"
check "Duplicate route id -> 409"            409 "$(status -X POST "$BASE/routes" -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d "$ROUTE")"
check "Invalid target URL -> 400"            400 "$(status -X POST "$BASE/routes" -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -d '{"routeId":"bad-url","name":"x","pathPattern":"/x","targetBaseUrl":"not a url","allowedMethods":["GET"]}')"
check "Activate route -> 200"                200 "$(status -X POST "$BASE/routes/smoke-route/activate" -H "Authorization: Bearer $A")"
check "Version history -> 200"               200 "$(status "$BASE/routes/smoke-route/versions" -H "Authorization: Bearer $A")"

echo "Authorization boundaries"
check "Tenant B cannot see A's route -> 404" 404 "$(status "$BASE/routes/smoke-route" -H "Authorization: Bearer $B")"
check "Tenant cannot use /admin -> 403"      403 "$(status "$BASE/admin/users" -H "Authorization: Bearer $A")"
check "Tenant cannot use /internal -> 403"   403 "$(status "$BASE/internal/routes" -H "Authorization: Bearer $A")"
check "Gateway cannot manage routes -> 403"  403 "$(status "$BASE/routes" -H "Authorization: Bearer $GW")"
check "Gateway reads /internal/routes -> 200" 200 "$(status "$BASE/internal/routes" -H "Authorization: Bearer $GW")"
check "Platform admin lists users -> 200"    200 "$(status "$BASE/admin/users" -H "Authorization: Bearer $ADMIN")"
check "Platform admin needs organizationId -> 400" 400 "$(status "$BASE/routes/smoke-route" -H "Authorization: Bearer $ADMIN")"

echo "Cleanup"
check "Delete route -> 204"                  204 "$(status -X DELETE "$BASE/routes/smoke-route" -H "Authorization: Bearer $A")"

echo; echo "Result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
