# Hướng dẫn gửi văn bản — POST /exchange

> **Yêu cầu trước khi bắt đầu:**
> - App đang chạy (`http://localhost:8080`)
> - MinIO đang chạy (`http://localhost:9001`)
> - Tổ chức gửi (`000.00.00.H41`) và tổ chức nhận (`000.00.00.H42`) đã `ACTIVE`
> - Certificate `SN-2027-ABC999` đã đăng ký (UC05 trong `run.md`)
> - File `private_key.pem` và `sign.java` nằm tại thư mục project

---

## Bước 1 — Upload file lên MinIO

1. Mở `http://localhost:9001` → đăng nhập `minioadmin` / `minioadmin`
2. Vào bucket **trucvanban** → bấm **Upload** → chọn file bất kỳ
3. Ghi lại tên file, ví dụ: `part6.pdf` → storagePath sẽ là `trucvanban/part6.pdf`

---

## Bước 2 — Tính SHA-256 của file

Mở **PowerShell**, chạy (thay đường dẫn file thật):

```powershell
(Get-FileHash -Algorithm SHA256 "D:\English\ÔN HÈ CT\đề tháng4\part7_1.pdf").Hash.ToLower()
```

Kết quả ví dụ:
```
3ad42948e662a3caecb36a5104fafb06d3bce6ec10386775c0a567ba7eaf259c
```

Ghi lại — đây là `payloadChecksum`.

---

## Bước 3 — Sửa 3 dòng đầu trong `sign.java` rồi chạy

Mở file `sign.java`, sửa **3 dòng này**:

```java
String documentCode    = "VB-2026-01-15";        // đổi mã mới, chưa tồn tại trong DB
String payloadChecksum = "3ad42948e662...";       // SHA-256 từ Bước 2
String storagePath     = "trucvanban/part6.pdf";  // đường dẫn file trong MinIO
```

> ⚠️ `documentCode` phải là giá trị mới mỗi lần gửi — nếu trùng sẽ báo lỗi "Mã văn bản đã tồn tại".

Chạy bằng PowerShell:

```powershell
cd d:\code\TeamthayCuong\TRUCVB-project\HeThongTrucVanBan
& 'C:\Users\Canh\.kiro\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\java.exe' sign.java
```

Script sẽ **tự động gọi API** và in kết quả:

```
=== RESPONSE ===
Status: 200
Body  : {"success":true,"message":"Tiếp nhận giao dịch thành công...","data":[{"transactionCode":"TXN-2026-1-2","currentStatus":"VALIDATED"}]}
```

> ✅ Ghi lại `transactionCode` — dùng cho test ACK (UC12) và tra trạng thái (UC13).

---

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Mã văn bản đã tồn tại` | `documentCode` đã dùng trước đó | Đổi `documentCode` sang giá trị mới |
| `Chứng thư số không hợp lệ` | Certificate chưa đăng ký hoặc hết hạn | Chạy UC05 trong `run.md` |
| `Chữ ký không hợp lệ` | `private_key.pem` không khớp với public key đăng ký | Dùng đúng cặp key |
| `Không tìm thấy tổ chức` | `senderCode` hoặc `receiverCodes` chưa ACTIVE | Chạy UC01 + UC02 trong `run.md` |
| `Status: 400` sau khi chạy script | App chưa restart sau khi sửa code | Restart app rồi chạy lại |
