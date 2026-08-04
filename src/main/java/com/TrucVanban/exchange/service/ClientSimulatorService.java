package com.TrucVanban.exchange.service;

import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.SignAndBuildRequest;
import com.TrucVanban.exchange.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ClientSimulatorService {

    Object processAndSend(MultipartFile file,
                          String senderCode,
                          List<String> receiverCodes,
                          String documentCode,
                          String certificateSerialNumber,
                          Integer priority) throws Exception;

    List<FileUploadResponse> uploadFiles(MultipartFile[] files) throws Exception;

    /**
     * FE - Bước 2: Ký số bằng Private Key lưu tại Server và tạo Payload
     */
    ExchangeDocumentRequest signAndBuildPayload(SignAndBuildRequest request) throws Exception;
}