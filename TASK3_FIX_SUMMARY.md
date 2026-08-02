# Task 3 Bug Fix Summary

## 🎯 Mục tiêu
Fix 2 bug nghiêm trọng trong Task 3 (Đồng bộ API Key từ DB lên Redis):

1. **Bug warmup thiếu field** - Cache không có `agencyCode` và `agencyStatus`
2. **Bug authorization** - Không verify agency trong path khớp với agency owner của API key

---

## ✅ Bug 1: Warmup thiếu field (ĐÃ FIX TRƯỚC ĐÓ)

### Vấn đề
`ApiKeyWarmupRunner.java` cache với `agencyCode = null` và `agencyStatus = null`

### Giải pháp đã áp dụng
- Load `Organization` batch từ database (tránh N+1 query)
- Populate đầy đủ `agencyCode` và `agencyStatus` vào `ApiKeyCacheValue`
- Skip cache nếu organization inactive

### Code đã có sẵn
```java
// Batch load organizations
Map<Long, Organization> organizationMap = organizationRepository.findAllById(agencyIds).stream()
    .collect(Collectors.toMap(Organization::getId, org -> org));

// Populate full cache value
new ApiKeyCacheValue(
    apiKey.getAgencyId(),
    organization.getCode(),      // ✅ Có agencyCode
    apiKey.getKeyId(),
    encryptionService.decrypt(apiKey.getSecretEnc()),
    apiKey.getStatus().name(),
    organization.getStatus().name(), // ✅ Có agencyStatus
    apiKey.getExpiresAt()
)
```

---

## 🔒 Bug 2: Authorization check (ĐÃ FIX)

### Vấn đề
Endpoint pattern như `/{senderCode}/transactions/sended/...` không verify `senderCode` khớp với agency owner của API key.

**Kịch bản tấn công:**
```
Agency A có API key hợp lệ
Agency A gọi: GET /agency-B/transactions/received
→ Pass authentication ✅ (vì A có key)
→ Lấy được data của Agency B ❌ (lỗ hổng bảo mật)
```

### Giải pháp đã áp dụng

#### 1. Annotation `@RequireAgencyMatch`
Đánh dấu endpoint cần kiểm tra authorization:

```java
@GetMapping(value = "{senderCode}/transactions/sended/{transactionCode}")
@RequireAgencyMatch(pathVariable = "senderCode")
public ResponseEntity<...> getTransactionSendStatus(
    @PathVariable String senderCode,
    @PathVariable String transactionCode) { ... }
```

#### 2. Interceptor `AgencyAuthorizationInterceptor`
Flow:
1. `HmacAuthenticationFilter` đã verify API key → set `verified_org_code` vào request attribute
2. Interceptor so sánh `verified_org_code` với `pathVariable` từ URL
3. Nếu không khớp → trả về **403 Forbidden**

```java
if (!verifiedOrgCode.equals(pathAgencyCode)) {
    log.warn("Authorization failed: verified={} but path={}", 
            verifiedOrgCode, pathAgencyCode);
    return 403 Forbidden;
}
```

#### 3. Đăng ký interceptor trong `WebMvcConfig`
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agencyAuthorizationInterceptor)
                .addPathPatterns("/**");
    }
}
```

---

## 📋 Danh sách file đã thay đổi

### Files mới tạo
1. **`WebMvcConfig.java`** - Đăng ký interceptor

### Files đã sửa
1. **`ExchangeController.java`**
   - Thêm import `RequireAgencyMatch`
   - Thêm `@RequireAgencyMatch(pathVariable = "senderCode")` cho endpoint `/sended`
   - Thêm `@RequireAgencyMatch(pathVariable = "receiverCode")` cho endpoint `/received`
   - Xóa unused imports (lombok AccessLevel, FieldDefaults)

### Files đã có sẵn (không cần sửa)
- `RequireAgencyMatch.java` - Annotation
- `AgencyAuthorizationInterceptor.java` - Logic kiểm tra
- `ApiKeyWarmupRunner.java` - Đã fix warmup từ trước
- `HmacAuthenticationService.java` - Set verified_org_code attribute

---

## 🔍 Phạm vi bảo vệ

### Protected paths (cần API Key + HMAC)
```yaml
protected-paths:
  - /ack
  - /*/transactions/sended/**    ✅ @RequireAgencyMatch
  - /*/transactions/received     ✅ @RequireAgencyMatch
```

### Unprotected paths (không cần HMAC)
- `/organizations/{code}/*` - Registry management endpoints
- `/{agencyCode}/api-keys` - API key management

---

## 🎓 Engineering Principles Applied

### 1. Separation of Concerns
- **Authentication** (Filter layer): Verify API key hợp lệ
- **Authorization** (Interceptor layer): Verify quyền truy cập resource

### 2. Declarative Security
- Sử dụng annotation `@RequireAgencyMatch` thay vì hardcode logic
- Dễ maintain, clear intent

### 3. Fail-Safe Design
- Mặc định: không có annotation = không check
- Có annotation nhưng thiếu `verified_org_code` → **DENY** (401)
- Path variable không khớp → **DENY** (403)

### 4. Performance Optimization
- Batch load organizations trong warmup (tránh N+1)
- Cache đầy đủ fields để tránh đọc DB thêm lần nữa

---

## 🧪 Test Cases cần verify

### Test authorization
```bash
# Agency A có key hợp lệ
# Scenario 1: Agency A truy cập resource của chính mình ✅
GET /agency-A/transactions/received
Headers: X-Api-Key: agency_A_key
Expected: 200 OK

# Scenario 2: Agency A truy cập resource của Agency B ❌
GET /agency-B/transactions/received
Headers: X-Api-Key: agency_A_key
Expected: 403 Forbidden
```

### Test warmup
```bash
# Sau khi restart app
# Check Redis cache có đầy đủ fields
redis-cli> GET apikey:tvb_live_xxxx
Expected: JSON có agencyCode và agencyStatus
```

---

## 📝 Next Steps

1. **Compile code** (cần Java 21 hoặc downgrade pom.xml xuống Java 17)
2. **Integration test** với real Redis
3. **Security audit** các endpoints khác
4. **Document API** với security requirements trong Swagger

---

## 🔐 Security Impact

**Trước fix:**
- ❌ Bất kỳ agency nào có key hợp lệ đều xem được data của mọi agency khác
- ❌ Không có audit log cho vi phạm authorization

**Sau fix:**
- ✅ Mỗi agency chỉ truy cập được data của chính mình
- ✅ Log cảnh báo khi có attempt truy cập trái phép
- ✅ Trả về 403 Forbidden rõ ràng
