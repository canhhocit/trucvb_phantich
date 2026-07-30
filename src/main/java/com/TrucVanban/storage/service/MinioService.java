package com.TrucVanban.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    String upload(MultipartFile file);

    String getPresignedUrl(String objectName);

    byte[] download(String objectName);

    void deleteByUrl(String url);
}
