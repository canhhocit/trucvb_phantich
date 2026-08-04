-- ============================================================
-- V2: Bổ sung index và cải thiện bảng audit_logs
-- Phục vụ tính năng Giả Lập Ký Số & Xác Minh Ký Số
-- ============================================================

-- Index tăng tốc tra cứu audit_logs theo transaction_id (Gateway ghi nhiều)
CREATE INDEX IF NOT EXISTS idx_audit_logs_transaction_id ON audit_logs(transaction_id);

-- Index tăng tốc tra cứu audit_logs theo actor_id (mã tổ chức)
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON audit_logs(actor_id);

-- Index tăng tốc tra cứu audit_logs theo action (SIGNATURE_VERIFIED, REPLAY_ATTACK_DETECTED...)
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);

-- Index tăng tốc tra cứu certificates theo serial_number
-- Được dùng bởi SignatureVerificationFilter (Chốt chặn 2 - Certificate Lookup)
CREATE UNIQUE INDEX IF NOT EXISTS idx_certificates_serial_number ON certificates(serial_number)
    WHERE serial_number IS NOT NULL;

-- Index tăng tốc tra cứu certificates ACTIVE theo serial_number (hot path)
CREATE INDEX IF NOT EXISTS idx_certificates_serial_status ON certificates(serial_number, status);

COMMENT ON INDEX idx_certificates_serial_number IS 'Hỗ trợ Gateway tra cứu Public Key theo serial_number (Chốt chặn 2 - Xác minh chữ ký số)';
COMMENT ON INDEX idx_audit_logs_transaction_id IS 'Hỗ trợ tra cứu lịch sử kiểm toán theo giao dịch';
