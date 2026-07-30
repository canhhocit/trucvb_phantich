package com.TrucVanban.exchange.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ClientSimulatorService {

    Object processAndSend(MultipartFile file,
                          String senderCode,
                          List<String> receiverCodes,
                          String documentCode,
                          String certificateSerialNumber,
                          Integer priority) throws Exception;
}