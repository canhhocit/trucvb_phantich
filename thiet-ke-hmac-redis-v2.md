# Thiết kế kiến trúc — API Key \+ HMAC signature \+ Redis

**Phạm vi áp dụng (đã chốt):**

- `POST /ack`  
- `GET /{senderCode}/transactions/sended/{transactionCode}`  
- `GET /{receiverCode}/transactions/received`

**Không thuộc phạm vi:**

- `POST /exchange` — giữ nguyên RSA (`SignatureVerificationFilter` hiện có), vì cần tính non-repudiation cho văn bản hành chính.  
- `registry/**` — sẽ dùng JWT, làm ở phần khác, không đụng tới trong tài liệu này.  
- Outbound (trục → Agency) — ghi nhận hướng đi ở mục 10, chưa triển khai ở giai đoạn này.

Ngữ cảnh: TrucVanban, Spring Boot 3.5.15 / Java 21, Spring Security, Spring Data Redis, PostgreSQL \+ Flyway, đã có `SignatureVerificationFilter` (RSA) và cache Caffeine cho certificate.

Nguyên tắc: `secret` **không bao giờ** đi trên đường truyền. Client chứng minh mình biết secret bằng cách ký request; server tính lại chữ ký và so sánh.

---

## 1\. Hợp đồng giao tiếp (contract)

### Headers bắt buộc

| Header | Ví dụ | Ý nghĩa |
| :---- | :---- | :---- |
| `X-Api-Key` | `tvb_live_a1b2c3d4` | keyId — công khai, dùng để tra secret |
| `X-Timestamp` | `1753855000` | Unix epoch **giây**, UTC |
| `X-Nonce` | `9f2c8e51-...` (UUIDv4) | duy nhất trong cửa sổ TTL |
| `X-Signature` | `v1=<hex64>` | HMAC-SHA256, có prefix version để nâng cấp thuật toán về sau |

### Canonical string (chuẩn hoá)

canonical \=

  HTTP\_METHOD            (uppercase, vd "POST")                       \+ "\\n" \+

  PATH                   (đã url-decode, vd "/api/v1/ack")            \+ "\\n" \+

  CANONICAL\_QUERY        (sort key A→Z, url-encode, rỗng nếu không có) \+ "\\n" \+

  X-Api-Key                                                            \+ "\\n" \+

  X-Timestamp                                                          \+ "\\n" \+

  X-Nonce                                                              \+ "\\n" \+

  LOWERCASE\_HEX(SHA-256(raw\_body\_bytes))

- `POST /ack` có body JSON nhỏ (`ReceiveDocumentRequest`) → hash toàn bộ raw body bytes.  
- 2 API `GET` không có body → hash của chuỗi rỗng (`e3b0c442...`), **không** bỏ dòng đó đi (giữ canonical string đồng nhất cấu trúc dù có body hay không).  
- `signature = HEX(HMAC_SHA256(secret_bytes, canonical_utf8))`

>   
> Phần lớn lỗi tích hợp HMAC đến từ canonicalization. Bắt buộc: viết tài liệu có **ví dụ đầy đủ request \+ secret \+ canonical string \+ signature mong đợi** để đối tác tự đối chiếu.

---

## 2\. Kiến trúc tổng thể

Filter này chỉ áp dụng cho `/ack` và 2 path `GET` transaction status — **không** chạy chung với `SignatureVerificationFilter` (path đó là `/exchange`, tách biệt hoàn toàn qua `shouldNotFilter`). Không có request nào đi qua cả 2 filter.

Request đến /ack, /{code}/transactions/\*\*

        │

        ▼

\[HmacAuthenticationFilter\]  (OncePerRequestFilter, dùng ContentCachingRequestWrapper

        │                    cho /ack vì có body JSON nhỏ; 2 API GET không cần wrap)

        ▼

HmacAuthenticationService

 ├─▶ ApiKeyCacheService ──▶ Redis  apikey:{keyId}

 │        └─ (miss) ─────▶ Postgres api\_keys  ─▶ ghi lại Redis

 ├─▶ ClockSkewValidator  (|now \- ts| \<= 300s)

 ├─▶ NonceStore ─────────▶ Redis  SET NX EX 300   (bắt buộc)

 └─▶ SignatureCalculator (HMAC-SHA256, so sánh constant-time)

        │

   thành công → set request attribute "verified\_org\_id", "verified\_org\_code"

                (theo đúng pattern SignatureVerificationFilter đang dùng,

                 không cần SecurityContext/Authentication vì SecurityConfig

                 hiện permitAll() toàn bộ, chưa có phân quyền theo scope)

   thất bại   → viết response lỗi chuẩn hoá trực tiếp, không qua GlobalException

                (vì filter chạy ngoài DispatcherServlet)

### Package đề xuất

shared/security/hmac/

├── HmacAuthenticationFilter.java     (OncePerRequestFilter)

├── HmacAuthenticationService.java    (orchestrate các bước xác thực)

├── SignatureCalculator.java          (canonical string \+ HMAC)

├── CanonicalRequest.java             (record: method, path, query, keyId, ts, nonce, bodyHash)

├── NonceStore.java (interface) \+ RedisNonceStore.java

├── ApiKeyCacheService.java           (cache-aside Redis ⇄ DB)

├── HmacProperties.java               (@ConfigurationProperties("security.hmac"))

└── exception/  ApiKeyException, SignatureException, ReplayException, ClockSkewException

registry/  (quản trị key: tạo / xoay / thu hồi — dùng chung hạ tầng key cho Agency,

            tách biệt với JWT sẽ làm ở registry API)

├── controller/ApiKeyController.java

├── service/ApiKeyManagementService.java

└── entity/ApiKey.java, repository/ApiKeyRepository.java

---

## 3\. Thứ tự các bước xác thực (fail fast, rẻ trước đắt sau)

| \# | Bước | Chi phí | Lỗi trả về |
| :---- | :---- | :---- | :---- |
| 1 | Có đủ 4 header bắt buộc? | \~0 | 401 `AUTH_HEADER_MISSING` |
| 2 | Timestamp trong cửa sổ ±300s? | \~0 | 401 `TIMESTAMP_SKEW` |
| 3 | Tra `apikey:{keyId}` trên Redis → agency \+ secret; check `keyStatus=ACTIVE`, chưa hết hạn, `agencyStatus=ACTIVE` | 1 Redis GET | 401 `API_KEY_INVALID` / `API_KEY_EXPIRED` / 403 `AGENCY_INACTIVE` |
| 4 | Tính lại HMAC, so sánh `MessageDigest.isEqual` | CPU (µs) | 401 `SIGNATURE_INVALID` |
| 5 | `SET nonce:{keyId}:{nonce} 1 NX EX 300` → false nghĩa là đã dùng | 1 Redis SET | 401 `REPLAY_DETECTED` |

**Lưu ý quan trọng:** bước 5 (nonce) đặt **sau** bước 4 (verify signature). Nếu làm ngược lại, kẻ tấn công chỉ cần spam nonce ngẫu nhiên là làm phình Redis mà không cần biết secret.

*(Rate limiting không nằm trong phạm vi giai đoạn này — có thể bổ sung sau như một filter độc lập nếu cần, không ảnh hưởng thiết kế xác thực.)*

---

## 4\. Đọc body cho `/ack`

Khác với `/exchange` (multipart, file lớn — không thuộc phạm vi tài liệu này), `/ack` chỉ có body JSON nhỏ (`ReceiveDocumentRequest`). Vì vậy **không cần** cơ chế streaming digest hay tách upload-ticket:

- Dùng `ContentCachingRequestWrapper` (có sẵn trong Spring, không cần viết wrapper riêng) đặt trước `HmacAuthenticationFilter`, để filter và Controller đều đọc được body.  
- Giới hạn kích thước qua `security.hmac.max-body-size` chỉ mang tính phòng vệ (request nhỏ), không phải mối lo hiệu năng ở đây.  
- 2 API `GET` không có body → không cần wrap request, hash cố định của chuỗi rỗng.

>   
> Nếu sau này mở rộng HMAC sang các API có upload file, tham khảo lại hướng streaming digest / upload-ticket 2 bước — nhưng **không cần thiết kế trước cho phạm vi hiện tại**.

---

## 5\. Lưu trữ

### Postgres — migration mới `V6__create_api_keys.sql`

CREATE TABLE api\_keys (

    id              BIGSERIAL PRIMARY KEY,

    agency\_id       BIGINT NOT NULL REFERENCES organizations(id),

    key\_id          VARCHAR(64)  NOT NULL UNIQUE,   \-- công khai, đi trong header

    secret\_enc      TEXT         NOT NULL,          \-- secret mã hoá AES-GCM bằng master key từ env

    secret\_hint     VARCHAR(8)   NOT NULL,          \-- 4 ký tự cuối, để đối chiếu khi hỗ trợ

    algorithm       VARCHAR(32)  NOT NULL DEFAULT 'HMAC\_SHA256',

    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', \-- ACTIVE | REVOKED

    expires\_at      TIMESTAMPTZ,

    last\_used\_at    TIMESTAMPTZ,

    created\_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    revoked\_at      TIMESTAMPTZ

);

CREATE INDEX idx\_api\_keys\_agency ON api\_keys(agency\_id, status);

**Điểm cốt lõi:** secret phải lưu ở dạng **mã hoá có thể giải** (AES-GCM), không phải hash một chiều — vì HMAC cần khôi phục secret để tính lại chữ ký. Master key nằm trong env/KMS, **không** trong DB. Ai chiếm được cả DB dump lẫn master key sẽ giả mạo được mọi agency — đây là đánh đổi đã chấp nhận khi chọn HMAC thay vì RSA cho nhóm API này.

> Không thêm cột `scopes`/phân quyền chi tiết ở giai đoạn này — mục tiêu hiện tại chỉ là "biết Agency nào đang gọi và có hợp lệ không", chưa phân quyền theo API. Có thể bổ sung sau nếu cần.

### Redis key layout

| Key | Type | TTL | Mục đích |
| :---- | :---- | :---- | :---- |
| `apikey:{keyId}` | String (JSON) | 30m (refresh khi hit) | `{agencyId, agencyCode, secret, keyStatus, agencyStatus, expiresAt}` |
| `apikey:miss:{keyId}` | String | 60s | negative cache, chống cache-penetration |
| `agency:keys:{agencyId}` | Set | – | danh sách keyId, để thu hồi hàng loạt khi khoá agency |
| `nonce:{keyId}:{nonce}` | String | 300s (= cửa sổ skew) | chống replay, **atomic bằng SET NX** |

### Warm-up & invalidation

- `ApplicationRunner` nạp key theo trang, ghi Redis bằng `executePipelined`, không chặn startup nếu Redis lỗi.  
- Thay đổi runtime (tạo/xoay/thu hồi key, khoá agency): cập nhật/xoá Redis ngay sau khi transaction commit.  
- `@Scheduled` reconcile định kỳ để hàn gắn lệch DB↔Redis (tần suất cấu hình qua `application.yml`, không hardcode).

---

## 6\. Chiến lược khi Redis lỗi

| Thành phần | Redis down | Lý do |
| :---- | :---- | :---- |
| Cache api key | **fail-open** → đọc Postgres | chỉ là tối ưu hiệu năng |
| Nonce store | **fail-closed** → 503 `AUTH_STORE_UNAVAILABLE` | không có nonce store thì không thể chống replay; fail-open ở đây tạo lỗ hổng |

Đặt timeout Redis ngắn cho các lệnh xác thực, tránh Redis chậm kéo sập toàn bộ API.

---

## 7\. Chuẩn hoá phản hồi lỗi

**Quyết định cần chốt trước khi code:** `ResponseData<T>` hiện tại (`{success, message, data}`) đang dùng chung cho toàn hệ thống. Có 2 lựa chọn:

- **(a)** Tái sử dụng `ResponseData` nguyên trạng, không thêm field — đơn giản, không ảnh hưởng schema JSON hiện có ở nơi khác. Chi tiết lỗi (mã lỗi cụ thể, timestamp, traceId) chỉ ghi vào log \+ `audit_logs`, không trả ra ngoài.  
- **(b)** Thêm field tuỳ chọn (`errorCode`) vào `ResponseData` nếu về sau muốn đối tác code logic theo mã lỗi thay vì parse message tiếng Việt.

→ **Khuyến nghị giai đoạn này: dùng (a)**, giữ nguyên `ResponseData`, vì phạm vi API không lớn và đối tác hiện đang xử lý theo `message`. Có thể nâng cấp lên (b) sau nếu số lượng đối tác tích hợp tăng và cần phân biệt lỗi theo mã.

Vì filter chạy **ngoài** `DispatcherServlet`, `@RestControllerAdvice` (`GlobalException`) không bắt được lỗi từ filter → cần viết response trực tiếp trong `HmacAuthenticationFilter`, theo đúng cách `SignatureVerificationFilter.writeErrorResponse()` đang làm.

{ "success": false, "message": "Xác thực thất bại. Vui lòng kiểm tra API Key và chữ ký." }

- Message ra ngoài giữ mức chung chung (tránh oracle giúp dò khóa); lý do chi tiết (`API_KEY_INVALID` vs `SIGNATURE_INVALID` vs `REPLAY_DETECTED`) chỉ ghi log nội bộ \+ `AuditLogService`.  
- Ngoại lệ: `TIMESTAMP_SKEW` nên nói rõ hơn, kèm giờ server, vì đây thường là lỗi vận hành (đồng hồ đối tác lệch) chứ không phải tấn công.  
- Không bao giờ log secret, canonical string đầy đủ hay signature đầy đủ (nếu cần debug, chỉ log 8 ký tự đầu).

---

## 8\. Xoay khóa (key rotation) không downtime

1. Tạo key mới cho agency → agency có **2 key ACTIVE** cùng lúc (bảng `api_keys` cho phép nhiều dòng/agency).  
2. Đối tác chuyển dần sang key mới; theo dõi `last_used_at` của key cũ.  
3. Khi key cũ không còn traffic → `status = REVOKED` \+ xoá khỏi Redis.

`last_used_at`: cập nhật throttle (không quá 1 lần/5 phút/key, ghi async) để không tạo 1 UPDATE mỗi request.

---

## 9\. Cấu hình (`application.yml`)

security:

  hmac:

    enabled: true

    header:

      api-key: X-Api-Key

      timestamp: X-Timestamp

      nonce: X-Nonce

      signature: X-Signature

    algorithm: HmacSHA256

    clock-skew: 300s

    nonce-ttl: 300s

    cache-ttl: 30m

    negative-cache-ttl: 60s

    max-body-size: 256KB

    protected-paths:

      \- /ack

      \- /\*/transactions/sended/\*\*

      \- /\*/transactions/received

Không hardcode bất kỳ giá trị nào ở trên trong code (theo AGENTS.md).

---

## 10\. Ghi chú cho các hướng chưa triển khai (để tham khảo sau)

- **Outbound (trục → Agency):** nếu làm sau, secret cho chiều này cần một bảng/cột riêng (vd `outbound_credentials` hoặc cột trong `organizations`), **không** dùng lại `system_config` — bảng đó đã bị xoá ở `V5__drop_system_config.sql` và không còn tồn tại trong schema. Retry (`retry_jobs`) phải sinh nonce \+ timestamp mới cho mỗi lần thử, giữ nguyên `idempotencyKey` trong body để bên nhận không xử lý trùng.  
- **Upload file có ký HMAC:** nếu mở rộng cơ chế này sang API có multipart lớn, cần quay lại thiết kế streaming digest hoặc tách upload-ticket 2 bước (không áp dụng cho `/ack` hiện tại).  
- **Phân quyền theo scope:** nếu sau này cần giới hạn 1 Agency chỉ được gọi 1 số API nhất định, bổ sung cột `scopes` vào `api_keys` và chuyển sang dùng `SecurityContext`/`Authentication` thật của Spring Security thay vì `request.setAttribute`.

---

## 11\. Thứ tự triển khai đề xuất

1. `HmacProperties` \+ `SignatureCalculator` \+ test vector cố định (không phụ thuộc hạ tầng, làm trước).  
2. Migration `api_keys` \+ entity/repository \+ `ApiKeyManagementService` (tạo/xoay/thu hồi).  
3. `ApiKeyCacheService` (cache-aside) \+ warm-up \+ invalidation sau commit.  
4. `RedisNonceStore` (SET NX EX).  
5. `HmacAuthenticationFilter` \+ đăng ký trong `SecurityConfig` (chỉ áp dụng cho `/ack` và 2 path query status) \+ xử lý lỗi trực tiếp trong filter.  
6. Integration test (Testcontainers: Postgres \+ Redis) \+ tài liệu đặc tả canonical string cho đối tác.

## 12\. Kiểm thử

- Happy path cho `/ack` và 2 API `GET`.  
- Thiếu từng header bắt buộc.  
- Timestamp lệch quá ±300s (cả quá khứ và tương lai).  
- Signature sai 1 byte; body bị sửa sau khi ký.  
- Gửi lại đúng request 2 lần → lần 2 phải bị chặn (`REPLAY_DETECTED`).  
- Key `REVOKED`; key hết hạn; agency `INACTIVE`.  
- Query param đảo thứ tự vẫn phải hợp lệ (canonical query đã sort).  
- Redis down: cache fail-open (vẫn xác thực được qua DB), nonce fail-closed (503).  
- Xoay khóa: 2 key cùng hoạt động cho 1 agency.  
- Unit test riêng cho `SignatureCalculator` với bộ vector cố định (input → canonical → signature mong đợi).

