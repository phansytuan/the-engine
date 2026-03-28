#!/usr/bin/env bash
# =============================================================================
# TaskFlow API Test Suite
# Tests all 19 endpoints across Tasks, Users, and Analytics controllers
# Usage: ./test_api.sh [BASE_URL]
#   BASE_URL defaults to http://localhost:8080
# =============================================================================

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
TOTAL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

print_header() {
  echo ""
  echo -e "${CYAN}${BOLD}══════════════════════════════════════════${RESET}"
  echo -e "${CYAN}${BOLD}  $1${RESET}"
  echo -e "${CYAN}${BOLD}══════════════════════════════════════════${RESET}"
}

assert() {
  local description="$1"
  local expected_status="$2"
  local actual_status="$3"
  local body="$4"
  TOTAL=$((TOTAL + 1))
  if [ "$actual_status" -eq "$expected_status" ]; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}✔${RESET}  $description"
    echo -e "     ${GREEN}→ $actual_status (expected $expected_status)${RESET}"
  else
    FAIL=$((FAIL + 1))
    echo -e "  ${RED}✘${RESET}  $description"
    echo -e "     ${RED}→ $actual_status (expected $expected_status)${RESET}"
    [ -n "$body" ] && echo -e "     ${YELLOW}Body: $(echo "$body" | head -c 300)${RESET}"
  fi
}

request() {
  local method="$1"
  local url="$2"
  shift 2
  local raw
  raw=$(curl -s -w "\n__STATUS__%{http_code}" -X "$method" "$url" \
    -H "Content-Type: application/json" "$@")
  HTTP_STATUS=$(printf '%s' "$raw" | tail -n1 | sed 's/.*__STATUS__//')
  HTTP_BODY=$(printf '%s' "$raw" | sed '$d' | sed 's/__STATUS__[0-9]*//')
}

extract() {
  printf '%s' "$1" | grep -o "\"$2\":[^,}]*" | head -1 | sed 's/.*://' | tr -d ' "'
}

skip_if_empty() {
  local id="$1" label="$2"
  if [ -z "$id" ]; then
    TOTAL=$((TOTAL + 1))
    echo -e "  ${YELLOW}–${RESET}  $label"
    echo -e "     ${YELLOW}SKIPPED — prerequisite ID not available${RESET}"
    return 1
  fi
  return 0
}

echo -e "${BOLD}TaskFlow API Test Suite${RESET}"
echo -e "Target: ${CYAN}$BASE_URL${RESET}"
echo ""
echo -n "Waiting for server"
for i in $(seq 1 20); do
  if curl -s --max-time 2 "$BASE_URL/api/users" > /dev/null 2>&1; then
    echo -e " ${GREEN}ready!${RESET}"; break
  fi
  echo -n "."; sleep 1
  if [ "$i" -eq 20 ]; then
    echo -e " ${RED}timed out. Is the server running?${RESET}"; exit 1
  fi
done

# =============================================================================
print_header "Users Controller"

request GET "$BASE_URL/api/users"
assert "GET /api/users — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/users/1"
assert "GET /api/users/1 — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/users/9999"
assert "GET /api/users/9999 — returns 404" 404 "$HTTP_STATUS" "$HTTP_BODY"

request POST "$BASE_URL/api/users" \
  --data-raw '{"email":"tester@taskflow.com","name":"Test User","password":"secret","role":"DEVELOPER"}'
assert "POST /api/users — create user returns 201" 201 "$HTTP_STATUS" "$HTTP_BODY"
NEW_USER_ID=$(extract "$HTTP_BODY" "id")
echo -e "     ${CYAN}↳ new user id: $NEW_USER_ID${RESET}"

request POST "$BASE_URL/api/users" \
  --data-raw '{"email":"tester@taskflow.com","name":"Dup User","password":"secret","role":"DEVELOPER"}'
assert "POST /api/users — duplicate email returns 409" 409 "$HTTP_STATUS" "$HTTP_BODY"

request POST "$BASE_URL/api/users" \
  --data-raw '{"email":"bad-email","name":"","password":""}'
assert "POST /api/users — invalid fields returns 400" 400 "$HTTP_STATUS" "$HTTP_BODY"

if skip_if_empty "$NEW_USER_ID" "PUT /api/users/? — skipped (no user id)"; then
  request PUT "$BASE_URL/api/users/$NEW_USER_ID" \
    --data-raw '{"name":"Updated Tester","role":"MANAGER"}'
  assert "PUT /api/users/$NEW_USER_ID — update user returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_USER_ID" "DELETE /api/users/? — skipped (no user id)"; then
  request DELETE "$BASE_URL/api/users/$NEW_USER_ID"
  assert "DELETE /api/users/$NEW_USER_ID — deactivate returns 204" 204 "$HTTP_STATUS" "$HTTP_BODY"
fi

# =============================================================================
print_header "Tasks Controller"

request GET "$BASE_URL/api/tasks"
assert "GET /api/tasks — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks?status=TODO"
assert "GET /api/tasks?status=TODO — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks?status=INVALID_STATUS"
assert "GET /api/tasks?status=INVALID_STATUS — returns 400" 400 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks/1"
assert "GET /api/tasks/1 — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks/9999"
assert "GET /api/tasks/9999 — returns 404" 404 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks/assignee/2"
assert "GET /api/tasks/assignee/2 — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/tasks/overdue"
assert "GET /api/tasks/overdue — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request POST "$BASE_URL/api/tasks" -H "X-User-Id: 1" \
  --data-raw '{"title":"Integration Test Task","description":"Created by test suite","priority":"HIGH"}'
assert "POST /api/tasks — create task returns 201" 201 "$HTTP_STATUS" "$HTTP_BODY"
NEW_TASK_ID=$(extract "$HTTP_BODY" "id")
echo -e "     ${CYAN}↳ new task id: $NEW_TASK_ID${RESET}"

request POST "$BASE_URL/api/tasks" -H "X-User-Id: 1" \
  --data-raw '{"description":"No title provided"}'
assert "POST /api/tasks — missing title returns 400" 400 "$HTTP_STATUS" "$HTTP_BODY"

if skip_if_empty "$NEW_TASK_ID" "PUT /api/tasks/? — skipped"; then
  request PUT "$BASE_URL/api/tasks/$NEW_TASK_ID" -H "X-User-Id: 1" \
    --data-raw '{"title":"Updated Task Title","description":"Updated desc","priority":"URGENT"}'
  assert "PUT /api/tasks/$NEW_TASK_ID — update returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PUT /api/tasks/? unauthorized — skipped"; then
  request PUT "$BASE_URL/api/tasks/$NEW_TASK_ID" -H "X-User-Id: 2" \
    --data-raw '{"title":"Hacked","description":"x","priority":"LOW"}'
  assert "PUT /api/tasks/$NEW_TASK_ID — unauthorized returns 403" 403 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PATCH status IN_PROGRESS — skipped"; then
  request PATCH "$BASE_URL/api/tasks/$NEW_TASK_ID/status?status=IN_PROGRESS" -H "X-User-Id: 1"
  assert "PATCH /api/tasks/$NEW_TASK_ID/status=IN_PROGRESS — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PATCH status COMPLETED invalid — skipped"; then
  request PATCH "$BASE_URL/api/tasks/$NEW_TASK_ID/status?status=COMPLETED" -H "X-User-Id: 1"
  assert "PATCH /api/tasks/$NEW_TASK_ID/status=COMPLETED (invalid transition) — returns 409" 409 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PATCH status IN_REVIEW — skipped"; then
  request PATCH "$BASE_URL/api/tasks/$NEW_TASK_ID/status?status=IN_REVIEW" -H "X-User-Id: 1"
  assert "PATCH /api/tasks/$NEW_TASK_ID/status=IN_REVIEW — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PATCH assign developer — skipped"; then
  request PATCH "$BASE_URL/api/tasks/$NEW_TASK_ID/assign?assigneeId=2" -H "X-User-Id: 2"
  assert "PATCH /api/tasks/$NEW_TASK_ID/assign — developer returns 403" 403 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "PATCH assign admin — skipped"; then
  request PATCH "$BASE_URL/api/tasks/$NEW_TASK_ID/assign?assigneeId=2" -H "X-User-Id: 1"
  assert "PATCH /api/tasks/$NEW_TASK_ID/assign — admin assigns returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "POST comments — skipped"; then
  request POST "$BASE_URL/api/tasks/$NEW_TASK_ID/comments" -H "X-User-Id: 1" \
    --data-raw '{"content":"This is a test comment from the test suite."}'
  assert "POST /api/tasks/$NEW_TASK_ID/comments — returns 201" 201 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "POST comments blank — skipped"; then
  request POST "$BASE_URL/api/tasks/$NEW_TASK_ID/comments" -H "X-User-Id: 1" \
    --data-raw '{"content":""}'
  assert "POST /api/tasks/$NEW_TASK_ID/comments — blank content returns 400" 400 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "DELETE task unauthorized — skipped"; then
  request DELETE "$BASE_URL/api/tasks/$NEW_TASK_ID" -H "X-User-Id: 2"
  assert "DELETE /api/tasks/$NEW_TASK_ID — unauthorized returns 403" 403 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "DELETE task by creator — skipped"; then
  request DELETE "$BASE_URL/api/tasks/$NEW_TASK_ID" -H "X-User-Id: 1"
  assert "DELETE /api/tasks/$NEW_TASK_ID — creator deletes returns 204" 204 "$HTTP_STATUS" "$HTTP_BODY"
fi

if skip_if_empty "$NEW_TASK_ID" "GET deleted task — skipped"; then
  request GET "$BASE_URL/api/tasks/$NEW_TASK_ID"
  assert "GET /api/tasks/$NEW_TASK_ID — after delete returns 404" 404 "$HTTP_STATUS" "$HTTP_BODY"
fi

# =============================================================================
print_header "Analytics Controller"

request GET "$BASE_URL/api/analytics/dashboard"
assert "GET /api/analytics/dashboard — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/by-status"
assert "GET /api/analytics/by-status — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/by-priority"
assert "GET /api/analytics/by-priority — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/overdue"
assert "GET /api/analytics/overdue — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/completion-rate"
assert "GET /api/analytics/completion-rate — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/top-assignees"
assert "GET /api/analytics/top-assignees — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

request GET "$BASE_URL/api/analytics/top-assignees?limit=1"
assert "GET /api/analytics/top-assignees?limit=1 — returns 200" 200 "$HTTP_STATUS" "$HTTP_BODY"

# =============================================================================
echo ""
echo -e "${CYAN}${BOLD}══════════════════════════════════════════${RESET}"
echo -e "${CYAN}${BOLD}  Results${RESET}"
echo -e "${CYAN}${BOLD}══════════════════════════════════════════${RESET}"
echo ""
echo -e "  Total  : ${BOLD}$TOTAL${RESET}"
echo -e "  ${GREEN}Passed : $PASS${RESET}"
if [ "$FAIL" -gt 0 ]; then
  echo -e "  ${RED}Failed : $FAIL${RESET}"
else
  echo -e "  Failed : $FAIL"
fi
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}All tests passed!${RESET}"
  exit 0
else
  echo -e "  ${RED}${BOLD}$FAIL test(s) failed.${RESET}"
  exit 1
fi
