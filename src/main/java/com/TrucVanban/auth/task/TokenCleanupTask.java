package com.TrucVanban.auth.task;

import com.TrucVanban.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Chạy vào lúc 00:00:00 mỗi ngày
    @Transactional
    public void cleanupTokens() {
        log.info("Bắt đầu dọn dẹp refresh token hết hạn hoặc bị khóa...");
        int deletedCount = refreshTokenRepository.deleteExpiredOrRevokedTokens(Instant.now());
        log.info("Hoàn tất dọn dẹp. Đã xóa {} refresh token.", deletedCount);
    }
}
