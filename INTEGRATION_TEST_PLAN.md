# Integration Test Plan - Task 3 Bug Fixes

## 🎯 Objective
Verify authorization mechanism works end-to-end with real API Key, HMAC signature, and Redis cache.

---

## 📋 Prerequisites

### 1. Start Redis
```bash
docker-compose up -d redis
```

### 2. Setup Test Data
Create 2 agencies with API keys:

```sql
-- Agency A
INSERT INTO organizations (code, name, status) 
VALUES ('AGENCY_A', 'Agency A Test', 'ACTIVE');

-- Agency B  
INSERT INTO organizations (code, name, status)
VALUES ('AGENCY_B', 'Agency B Test', 'ACTIVE');
```

### 3. Generate API Keys
```bash
# Agency A
POST http://localhost:8080/AGENCY_A/api-keys
Response: { keyId: "tvb_live_xxx", secret: "yyy..." }

# Agency B
POST http://localhost:8080/AGENCY_B/api-keys
Response: { keyId: "tvb_live_zzz", secret: "www..." }
```

---

## 🧪 Test Scenarios

### Test 1: Warmup Cache Verification

**Goal:** Verify cache có đầy đủ `agencyCode` và `agencyStatus`

```bash
# 1. Restart application
./mvnw spring-boot:run

# 2. Check logs
# Expected: [ApiKeyWarmupRunner] Loading active API keys into Redis
# Expected: [ApiKeyWarmupRunner] Warmup complete - loaded 2 keys

# 3. Verify Redis cache
redis-cli> GET apikey:tvb_live_xxx
Expected JSON:
{
  "agencyId": 1,
  "agencyCode": "AGENCY_A",     ✅ Not null
  "keyId": "tvb_live_xxx",
  "secret": "...",
  "keyStatus": "ACTIVE",
  "agencyStatus": "ACTIVE",      ✅ Not null
  "expiresAt": null
}
```

**Pass criteria:**
- ✅ Cache entry exists
- ✅ `agencyCode` is not null
- ✅ `agencyStatus` is "ACTIVE"

---

### Test 2: Valid Authorization - Own Resource Access

**Goal:** Agency A truy cập resource của chính mình

```bash
# Setup
AGENCY=AGENCY_A
API_KEY=tvb_live_xxx
SECRET=yyy...
TIMESTAMP=$(date +%s)
NONCE=$(uuidgen)

# Calculate signature
METHOD=GET
PATH=/AGENCY_A/transactions/received
CANONICAL_STRING="${METHOD}\n${PATH}\n\n${API_KEY}\n${TIMESTAMP}\n${NONCE}\n"
SIGNATURE=$(echo -n "$CANONICAL_STRING" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

# Request
curl -X GET "http://localhost:8080/AGENCY_A/transactions/received" \
  -H "X-Api-Key: $API_KEY" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIGNATURE"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Hoàn thành lấy danh sách thông tin trạng thái của các giao dịch đã nhận được",
  "data": []
}
```

**Pass criteria:**
- ✅ HTTP 200 OK
- ✅ Request passes HMAC filter
- ✅ Request passes authorization interceptor
- ✅ Returns transaction data

**Logs to verify:**
```
[HmacAuthenticationService] Authentication successful
[AgencyAuthorizationInterceptor] Authorization passed for agency: AGENCY_A
```

---

### Test 3: Forbidden - Cross-Agency Access

**Goal:** Agency A cố truy cập resource của Agency B → 403 Forbidden

```bash
# Setup: Sử dụng API key của Agency A
AGENCY=AGENCY_A
API_KEY=tvb_live_xxx
SECRET=yyy...
TIMESTAMP=$(date +%s)
NONCE=$(uuidgen)

# Calculate signature cho path của Agency B
METHOD=GET
PATH=/AGENCY_B/transactions/received  # ❌ Agency B's path
CANONICAL_STRING="${METHOD}\n${PATH}\n\n${API_KEY}\n${TIMESTAMP}\n${NONCE}\n"
SIGNATURE=$(echo -n "$CANONICAL_STRING" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

# Request with Agency A's key but Agency B's path
curl -X GET "http://localhost:8080/AGENCY_B/transactions/received" \
  -H "X-Api-Key: $API_KEY" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIGNATURE" \
  -v
```

**Expected Response:**
```json
{
  "success": false,
  "message": "Bạn không có quyền truy cập tài nguyên của tổ chức này",
  "data": null
}
```

**Pass criteria:**
- ✅ HTTP 403 Forbidden
- ✅ HMAC authentication passes (key is valid)
- ✅ Authorization interceptor blocks request

**Logs to verify:**
```
[HmacAuthenticationService] Authentication successful
[AgencyAuthorizationInterceptor] Authorization failed: verified=AGENCY_A but path=AGENCY_B
```

---

### Test 4: Transaction Sended Endpoint

**Goal:** Verify `senderCode` path variable authorization

```bash
# Agency A access own transaction ✅
GET /AGENCY_A/transactions/sended/TX001
Headers: X-Api-Key: agency_a_key
Expected: 200 OK

# Agency A access Agency B's transaction ❌
GET /AGENCY_B/transactions/sended/TX002
Headers: X-Api-Key: agency_a_key
Expected: 403 Forbidden
```

---

### Test 5: Cache Expiration & Refresh

**Goal:** Verify cache-aside pattern works

```bash
# 1. Clear cache
redis-cli> DEL apikey:tvb_live_xxx

# 2. Make request
GET /AGENCY_A/transactions/received
Headers: X-Api-Key: tvb_live_xxx

# 3. Verify cache was repopulated
redis-cli> GET apikey:tvb_live_xxx
Expected: Cache entry exists with full fields

# 4. Check logs
Expected: [ApiKeyCacheService] Cache miss, loading from database
```

---

### Test 6: Negative Cache

**Goal:** Verify invalid key is cached as miss

```bash
# 1. Request with invalid key
GET /AGENCY_A/transactions/received
Headers: X-Api-Key: invalid_key_xxx
Expected: 401 Unauthorized

# 2. Verify negative cache
redis-cli> GET apikey:miss:invalid_key_xxx
Expected: "1"

# 3. Retry same request - should hit negative cache
GET /AGENCY_A/transactions/received
Headers: X-Api-Key: invalid_key_xxx
Expected: 401 (faster response, no DB query)
```

---

### Test 7: Organization Suspended

**Goal:** Verify suspended organization cannot access

```bash
# 1. Suspend Agency A
UPDATE organizations SET status = 'SUSPENDED' WHERE code = 'AGENCY_A';

# 2. Clear cache to force reload
redis-cli> DEL apikey:tvb_live_xxx

# 3. Try to access
GET /AGENCY_A/transactions/received
Headers: X-Api-Key: tvb_live_xxx
Expected: 401 Unauthorized
Message: "Agency or API key is not active"
```

---

## 📊 Test Result Matrix

| Test # | Scenario | Agency | Path | Expected | Status |
|--------|----------|--------|------|----------|--------|
| 1 | Cache warmup | - | - | Full fields in cache | ⏳ |
| 2 | Own resource | A | /A/... | 200 OK | ⏳ |
| 3 | Cross-agency | A | /B/... | 403 Forbidden | ⏳ |
| 4 | Sended endpoint | A | /A/sended/... | 200 OK | ⏳ |
| 5 | Cache refresh | A | /A/... | Cache repopulated | ⏳ |
| 6 | Negative cache | - | Invalid key | 401, cached miss | ⏳ |
| 7 | Suspended org | A (suspended) | /A/... | 401 | ⏳ |

Legend: ⏳ Pending | ✅ Pass | ❌ Fail

---

## 🔍 Debugging Commands

### Check Redis Cache
```bash
# List all API keys
redis-cli> KEYS apikey:*

# Get specific key
redis-cli> GET apikey:tvb_live_xxx

# Check negative cache
redis-cli> KEYS apikey:miss:*

# Check agency key set
redis-cli> SMEMBERS agency:keys:1
```

### Check Database
```sql
-- List all active API keys
SELECT k.key_id, k.status, o.code, o.status as org_status
FROM api_keys k
JOIN organizations o ON k.agency_id = o.id
WHERE k.status = 'ACTIVE';
```

### Enable Debug Logs
```yaml
logging:
  level:
    com.TrucVanban.shared.security.hmac: DEBUG
```

---

## ✅ Success Criteria

All tests must pass:
- ✅ Cache contains `agencyCode` and `agencyStatus`
- ✅ Valid agency can access own resources
- ✅ Cross-agency access is blocked with 403
- ✅ Logs show authorization checks
- ✅ Cache refresh works correctly
- ✅ Negative cache prevents DB queries
- ✅ Suspended organizations are blocked

---

## 🚨 Common Issues

### Issue: 401 with valid signature
**Cause:** Cache missing `agencyCode` or `agencyStatus`  
**Fix:** Restart app to re-warmup cache

### Issue: 200 when should be 403
**Cause:** Interceptor not registered  
**Fix:** Check `WebMvcConfig` bean is loaded

### Issue: 500 Internal Error
**Cause:** Path variable name mismatch  
**Fix:** Verify `@RequireAgencyMatch(pathVariable = "...")` matches `@PathVariable` name
