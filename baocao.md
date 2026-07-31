# Báo cáo tiến độ — Xác thực liên hệ thống bằng API Key + HMAC + Redis

---

## Phần 1 — Bối cảnh và vấn đề cần giải quyết

Hệ thống Trục Văn Bản là một trung gian kết nối nhiều cơ quan, đơn vị (gọi là Agency) với nhau. Khi một Agency gọi API vào hệ thống, cần phải biết chắc:

1. **Đây có phải Agency đã đăng ký không?** (xác thực danh tính)
2. **Request này có bị giả mạo trên đường truyền không?** (xác thực tính toàn vẹn)
3. **Request này có bị kẻ tấn công bắt lại rồi gửi lại không?** (chống replay attack)

Trước đây, các API gửi văn bản (`POST /exchange`) dùng **chữ ký số RSA** — phù hợp vì văn bản hành chính cần tính không thể phủ nhận (non-repudiation). Nhưng với các API tra cứu trạng thái và xác nhận nhận văn bản, RSA là quá nặng. Vì vậy, tuần này triển khai cơ chế nhẹ hơn: **API Key + HMAC-SHA256 + Redis**.

---

## Phần 2 — Các khái niệm cần hiểu

### 2.1. API Key là gì?

API Key là một cặp gồm:
- **`keyId`** — mã định danh công khai, giống như "tên đăng nhập". Truyền trong header `X-Api-Key`.
- **`secret`** — mật khẩu bí mật, **không bao giờ truyền trên mạng**. Chỉ dùng để tính toán chữ ký.

Mỗi Agency được cấp một hoặc nhiều cặp key. Key có thể bị thu hồi (`REVOKED`) khi nghi ngờ lộ lọt.

### 2.2. HMAC là gì?

**HMAC (Hash-based Message Authentication Code)** là cách chứng minh "tôi biết secret" mà không cần gửi secret đi.

Cách hoạt động:
1. Client lấy các thông tin của request (method, path, timestamp, nonce, hash của body...) ghép lại thành một chuỗi gọi là **canonical string**.
2. Client dùng `secret` để tính `HMAC-SHA256(secret, canonical_string)` → ra một chuỗi hex gọi là **signature**.
3. Client gửi signature trong header `X-Signature`.
4. Server nhận request, tự tính lại canonical string từ các thành phần của request, rồi tính lại signature bằng secret lưu trong DB.
5. Nếu signature của client == signature server tính → request hợp lệ.

Nếu kẻ tấn công sửa bất kỳ thứ gì trong request (body, path, timestamp...) thì canonical string thay đổi → signature sai → bị chặn.

### 2.3. Canonical String là gì?

Là chuỗi chuẩn hóa được ghép từ các thành phần của request theo đúng thứ tự, để cả client và server đều tính ra giống nhau. Cấu trúc:

```
HTTP_METHOD
PATH
CANONICAL_QUERY   (query params sắp xếp theo A→Z)
X-Api-Key
X-Timestamp
X-Nonce
SHA256(body_bytes)
```

Ví dụ với `POST /api/v1/ack`:
```
POST
/api/v1/ack

tvb_live_abc123
1753855000
9f2c8e51-abcd-...
a1b2c3d4... (SHA-256 của body JSON)
```

> Dòng query rỗng nếu không có query param — nhưng vẫn phải có dòng đó để cấu trúc nhất quán.

### 2.4. Nonce là gì? Tại sao cần?

**Nonce** (Number used ONCE) là một chuỗi ngẫu nhiên duy nhất cho mỗi request (dùng UUID v4).

Vấn đề nếu không có nonce: kẻ tấn công bắt được một request hợp lệ rồi gửi lại — signature vẫn đúng, server vẫn chấp nhận → **replay attack**.

Giải pháp: Server lưu nonce vào Redis với TTL 300 giây. Nếu cùng nonce xuất hiện lần 2 → từ chối ngay.

```
Redis key: nonce:{keyId}:{nonce}  →  TTL 300s
Lệnh:      SET NX EX (chỉ set nếu chưa tồn tại, atomic)
```

### 2.5. Timestamp để làm gì?

Client gửi thời gian hiện tại (Unix epoch, tính bằng giây) trong header `X-Timestamp`. Server kiểm tra:

```
|thời_gian_server - timestamp_client| ≤ 300 giây
```

Nếu lệch quá 5 phút → từ chối. Mục đích: giới hạn thời gian sống của một request hợp lệ. Kết hợp với nonce, đảm bảo không có request nào có thể dùng lại sau 5 phút.

### 2.6. Redis đóng vai trò gì?

Redis được dùng cho 2 mục đích trong luồng này:

| Mục đích | Redis key | TTL |
|---|---|---|
| Cache thông tin API key (tránh query DB mỗi request) | `apikey:{keyId}` | 30 phút |
| Negative cache (key không tồn tại, tránh DB spam) | `apikey:miss:{keyId}` | 60 giây |
| Chống replay attack bằng nonce | `nonce:{keyId}:{nonce}` | 300 giây |
| Danh sách key của một agency (để thu hồi hàng loạt) | `agency:keys:{agencyId}` | Không TTL |

Chiến lược khi Redis lỗi:
- **Cache API key**: fail-open → đọc thẳng từ PostgreSQL (chậm hơn nhưng vẫn hoạt động)
- **Nonce store**: fail-closed → trả về HTTP 503 (vì không có nonce store thì không thể chống replay, cho qua là lỗ hổng)

### 2.7. Secret được lưu thế nào trong DB?

Secret không lưu dạng plaintext trong DB. Nó được mã hóa bằng **AES-256-GCM** trước khi lưu.

- **Master key** (khóa để mã hóa secret) được lưu trong biến môi trường (`HMAC_MASTER_KEY`), không lưu trong DB.
- Khi cần xác thực, server giải mã secret bằng master key, rồi dùng để tính HMAC.
- Nếu lộ DB dump mà không có master key → không giải mã được secret → an toàn.

---

## Phần 3 — Thứ tự xác thực (fail fast)

Server kiểm tra theo thứ tự từ rẻ đến đắt, dừng ngay khi gặp lỗi đầu tiên:

```
Bước 1 — Có đủ 4 header không?
         (X-Api-Key, X-Timestamp, X-Nonce, X-Signature)
         Chi phí: ~0   |   Lỗi: 401

Bước 2 — Timestamp có trong cửa sổ ±300s không?
         Chi phí: ~0   |   Lỗi: 401

Bước 3 — Tra Redis/DB lấy thông tin API key
         Kiểm tra: key tồn tại, status ACTIVE, chưa hết hạn, agency ACTIVE
         Chi phí: 1 Redis GET   |   Lỗi: 401 / 403

Bước 4 — Tính lại HMAC, so sánh với signature trong header
         (so sánh constant-time để tránh timing attack)
         Chi phí: CPU ~µs   |   Lỗi: 401

Bước 5 — SET nonce vào Redis (NX = chỉ set nếu chưa có)
         Nếu đã tồn tại → replay attack
         Chi phí: 1 Redis SET   |   Lỗi: 401
```

> **Tại sao nonce kiểm tra sau signature?**
> Nếu kiểm tra nonce trước, kẻ tấn công spam nonce ngẫu nhiên mà không cần biết secret → làm phình Redis. Kiểm tra signature trước đảm bảo chỉ request hợp lệ mới tốn tài nguyên nonce.

---

## Phần 4 — Kiến trúc các thành phần đã xây dựng

```
Request đến /ack, /{code}/transactions/**
         │
         ▼
 HmacAuthenticationFilter         ← OncePerRequestFilter, chạy trước Controller
         │                           Bọc body bằng ContentCachingRequestWrapper
         │                           (chỉ với POST /ack để đọc body 2 lần)
         ▼
 HmacAuthenticationService        ← Orchestrate 5 bước xác thực
   ├── ApiKeyCacheService          ← Cache-aside: Redis → DB fallback
   ├── SignatureCalculator         ← Tính canonical string + HMAC-SHA256
   └── RedisNonceStore             ← SET NX EX 300 giây
         │
   Thành công → set request.setAttribute("verified_org_id", ...)
   Thất bại   → ghi response lỗi JSON trực tiếp (không qua Spring MVC)
```

### Các file đã tạo

| File | Vai trò |
|---|---|
| `HmacProperties.java` | Bind config từ `application.yml` (clock-skew, TTL, paths...) |
| `SignatureCalculator.java` | Tính canonical string và HMAC-SHA256 |
| `HmacAuthenticationService.java` | Điều phối 5 bước xác thực |
| `HmacAuthenticationFilter.java` | Filter Spring Security, áp dụng cho 3 path cụ thể |
| `ApiKeyCacheService.java` | Cache-aside Redis ↔ PostgreSQL, negative cache |
| `AesGcmEncryptionService.java` | Mã hóa/giải mã secret bằng AES-256-GCM |
| `RedisNonceStore.java` | Lưu nonce vào Redis, chống replay |
| `ApiKeyWarmupRunner.java` | Load toàn bộ active key lên Redis khi app khởi động |
| `HmacAuthenticationException.java` | Phân loại lỗi: Missing, Skew, Invalid, Expired, Replay, Unavailable |
| `V6__create_api_keys.sql` | Migration Flyway tạo bảng `api_keys` trong PostgreSQL |
| `ApiKeyManagementService.java` | Service tạo / thu hồi API key |
| `ApiKeyController.java` | REST API để admin tạo và thu hồi key |

---

## Phần 5 — API quản lý key (dành cho admin hệ thống)

### Tạo API key cho Agency

```
POST /api/v1/registry/agencies/{agencyCode}/api-keys
```

Server sinh ngẫu nhiên `keyId` và `secret`, mã hóa secret bằng AES-GCM, lưu vào DB. Response trả về secret plaintext **duy nhất lần này** — sau không lấy lại được.

### Thu hồi API key

```
DELETE /api/v1/registry/agencies/api-keys/{keyId}
```

Đánh dấu `status = REVOKED` trong DB và xóa cache Redis ngay lập tức. Mọi request dùng key này sau đó đều bị từ chối.

---

## Phần 6 — Các API được bảo vệ

| API | Method | Mô tả |
|---|---|---|
| `/api/v1/ack` | POST | Xác nhận đã nhận văn bản |
| `/api/v1/{senderCode}/transactions/sended/{transactionCode}` | GET | Tra trạng thái giao dịch gửi |
| `/api/v1/{receiverCode}/transactions/received` | GET | Danh sách văn bản đã nhận |

Các API khác (`/exchange`, `/registry/**`) không đi qua filter này.

---

## Phần 7 — Phản hồi lỗi chuẩn hóa

Tất cả lỗi xác thực trả về cùng một format, không tiết lộ nguyên nhân cụ thể ra ngoài:

```json
{
  "success": false,
  "message": "Xác thực thất bại. Vui lòng kiểm tra API Key và chữ ký.",
  "data": null
}
```

Ngoại lệ với lỗi timestamp (vì thường là đồng hồ máy client bị lệch, không phải tấn công):

```json
{
  "success": false,
  "message": "Xác thực thất bại do timestamp lệch quá giới hạn. Vui lòng đồng bộ đồng hồ và thử lại.",
  "data": null
}
```

Lý do chi tiết (`SIGNATURE_INVALID`, `REPLAY_DETECTED`...) chỉ ghi vào log nội bộ, không trả ra ngoài để tránh hỗ trợ kẻ tấn công dò khóa.

---

## Phần 8 — Tóm tắt tiến độ các subtask

| Subtask | Trạng thái | Ghi chú |
|---|---|---|
| 1. Thiết kế flow xác thực | ✅ Hoàn thành | 5 bước fail-fast, tài liệu thiết kế đã có |
| 2. HMAC signature verification | ✅ Hoàn thành | `SignatureCalculator`, so sánh constant-time |
| 3. Đồng bộ API Key từ DB lên Redis | ✅ Hoàn thành | `ApiKeyWarmupRunner` + cache-aside + invalidation khi revoke |
| 4. Kiểm tra API Key trên Redis + nonce | ✅ Hoàn thành | `ApiKeyCacheService` + `RedisNonceStore` |
| 5. Chuẩn hóa phản hồi và logging | ✅ Hoàn thành | Filter ghi response trực tiếp, log chi tiết nội bộ |
| 6. Kiểm thử và xác minh | 🔄 Đang làm | Script test PowerShell đã có, unit test chưa viết |

---

## Phần 9 — Những gì chưa làm và lý do

- **Unit test cho `SignatureCalculator`**: chưa viết, dự kiến làm song song khi team test tích hợp.
- **`ApiKeyController` quản lý key**: đã implement trong tuần này, chưa có trong kế hoạch ban đầu nhưng cần thiết để tạo key phục vụ test.
- **Outbound (Trục → Agency)**: chưa triển khai theo đúng phạm vi đã chốt — sẽ thiết kế riêng ở giai đoạn sau.
- **Rate limiting**: không thuộc phạm vi giai đoạn này, có thể bổ sung như một filter độc lập sau mà không ảnh hưởng thiết kế hiện tại.

---

## Phần 10 — So sánh với RSA (giải thích tại sao dùng 2 cơ chế)

| Tiêu chí | RSA (POST /exchange) | HMAC-SHA256 (3 API còn lại) |
|---|---|---|
| Mục đích | Văn bản hành chính, cần bằng chứng pháp lý | Tra cứu trạng thái, xác nhận nhận |
| Non-repudiation | ✅ Có (chỉ người giữ private key mới ký được) | ❌ Không (ai biết secret đều ký được) |
| Hiệu năng | Chậm hơn (phép tính khóa bất đối xứng) | Nhanh hơn (~µs, khóa đối xứng) |
| Phù hợp cho | Nội dung văn bản cần lưu trữ, kiểm toán | API call thường xuyên, cần tốc độ |
