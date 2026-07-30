package com.TrucVanban.exchange.controller;


import com.TrucVanban.exchange.service.ClientSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/simulator")
@RequiredArgsConstructor
public class ClientSimulatorController {

    private final ClientSimulatorService clientSimulatorService;

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Giả lập Client: Upload file, tự động ký số thật và gửi văn bản")
    public ResponseEntity<?> simulateClientSend(
            @RequestPart("file") MultipartFile file,
            @RequestParam String senderCode,
            @RequestParam List<String> receiverCodes,
            @RequestParam String documentCode,
            @RequestParam String certificateSerialNumber,
            @RequestParam(required = false, defaultValue = "1") Integer priority) throws Exception {

        log.info("[Simulator Controller] Tiếp nhận yêu cầu giả lập gửi văn bản từ Client...");

        // Đẩy toàn bộ tác vụ nặng (Upload, Băm, Ký, Gửi) xuống tầng Service
        Object response = clientSimulatorService.processAndSend(
                file, senderCode, receiverCodes, documentCode, certificateSerialNumber, priority);

        return ResponseEntity.ok(response);
    }
}
