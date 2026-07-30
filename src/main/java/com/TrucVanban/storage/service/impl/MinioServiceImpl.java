package com.TrucVanban.storage.service.impl;

import com.TrucVanban.storage.service.MinioService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public String upload(MultipartFile file) {
        try {
            ensureBucketExists();
            String objectName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Upload file thành công: {}", objectName);
            return objectName;
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Upload file thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .build());
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể lấy URL file: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String objectName) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build())) {
            return stream.readAllBytes();
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Tải file từ MinIO thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByUrl(String url) {
        try {
            String path = new java.net.URI(url).getPath();
            String objectName = path.substring(path.indexOf(bucketName) + bucketName.length() + 1);
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("Xóa file thành công: {}", objectName);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Xóa file thất bại: " + e.getMessage(), e);
        } catch (java.net.URISyntaxException e) {
            throw new RuntimeException("URL không hợp lệ: " + e.getMessage(), e);
        }
    }

    private void ensureBucketExists() throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Tạo bucket mới: {}", bucketName);
        }
    }
}
