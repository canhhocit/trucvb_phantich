# Triển khai cơ chế xác thực giữa các hệ thống bằng API Key + HMAC + Redis
Mục tiêu trong tuần này là sẽ xây dựng cơ chế xác thực cho các hệ thống tích hợp, đảm bảo chỉ các Agency đã đăng ký và được kích hoạt mới có thể truy cập các API liên quan. Cơ chế sẽ sử dụng API Key truyền qua HTTP Header, kết hợp HMAC signature để xác thực request, và dùng Redis để tăng hiệu năng, lưu cache API key và chống replay attack.

## Mục tiêu chính
- Thiết kế và triển khai quy trình xác thực request giữa các hệ thống bằng API Key + HMAC.
- Đồng bộ API Key từ Database lên Redis để phục vụ tra cứu nhanh khi request đến.
- Xây dựng logic kiểm tra API Key và trạng thái Agency trên Redis trước khi cho phép request tiếp tục.
- Chuẩn hóa phản hồi khi xác thực thất bại để các hệ thống tích hợp có thể xử lý lỗi thống nhất.

## Subtasks
### 1. Thiết kế flow xác thực
- Xác định vị trí truyền API Key và các header cần thiết: X-Api-Key, X-Timestamp, X-Nonce, X-Signature.
- Thiết kế quy trình kiểm tra request trước khi vào business logic.
- Xác định các trường hợp xác thực thất bại và mã lỗi tương ứng.

### 2. Triển khai HMAC signature verification
- Xây dựng logic chuẩn hóa canonical string theo method, path, query, headers, body hash.
- Tính và kiểm tra HMAC-SHA256 trên server.
- Đảm bảo việc so sánh chữ ký là constant-time và an toàn.

### 3. Đồng bộ API Key từ DB lên Redis
- Tạo/điều chỉnh logic load API Key hợp lệ từ Database.
- Đồng bộ dữ liệu lên Redis khi hệ thống khởi động hoặc khi có thay đổi trạng thái key/agency.
- Xây dựng cache-aside và negative cache để giảm truy vấn DB.

### 4. Xây dựng kiểm tra API Key trên Redis
- Tra cứu API Key trong Redis trước khi xử lý request.
- Xác định Agency tương ứng và kiểm tra trạng thái hoạt động của key và agency.
- Implement cơ chế chống replay bằng nonce store trên Redis.

### 5. Chuẩn hóa phản hồi và logging
- Thiết kế response chuẩn cho trường hợp thiếu header, sai signature, key không hợp lệ, agency bị khóa, replay detected.
- Ghi log và audit event phù hợp cho các trường hợp xác thực thất bại.


