package org.temporedata.modules.integration.file.storage;

import org.temporedata.modules.integration.file.config.FileProperties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * S3-compatible object storage provider (app.file.storage-type=s3).
 * Works with MinIO / Ceph RGW / Aliyun OSS / Tencent COS / Huawei OBS by
 * pointing `app.file.s3.endpoint` at the target (S3 is the shared protocol).
 */
@Slf4j
public class S3FileStorageProvider implements FileStorageProvider {

    /** Files above this size use multipart upload to avoid single-PUT limits/timeouts. */
    private static final long MULTIPART_THRESHOLD = 100L * 1024 * 1024; // 100MB
    private static final long PART_SIZE = 50L * 1024 * 1024; // 50MB

    private final FileProperties fileProperties;
    private S3Client client;

    public S3FileStorageProvider(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @PostConstruct
    public void init() {
        FileProperties.S3 cfg = fileProperties.getS3();
        client = S3Client.builder()
                .endpointOverride(URI.create(cfg.getEndpoint()))
                .region(Region.of(cfg.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey())))
                // path-style addressing is required by MinIO / Ceph and works for S3 too
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        log.info("S3 storage provider initialized, bucket={}, endpoint={}", cfg.getBucket(), cfg.getEndpoint());
    }

    @Override
    public String put(Path source, String key) {
        try {
            long size = Files.size(source);
            if (size <= MULTIPART_THRESHOLD) {
                client.putObject(PutObjectRequest.builder()
                        .bucket(bucket()).key(key).build(),
                        RequestBody.fromFile(source.toFile()));
            } else {
                multipartUpload(source, key, size);
            }
            return key;
        } catch (Exception e) {
            throw new RuntimeException("S3 put failed: " + key, e);
        }
    }

    /**
     * Multipart upload streaming each 50MB part from the file (bounded memory,
     * resumable-friendly), aborting on partial failure.
     */
    private void multipartUpload(Path source, String key, long size) {
        String uploadId = client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket()).key(key).build()).uploadId();
        List<CompletedPart> parts = new ArrayList<>();
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(source.toFile(), "r")) {
            long position = 0;
            int partNumber = 1;
            while (position < size) {
                long length = Math.min(PART_SIZE, size - position);
                raf.seek(position);
                byte[] chunk = new byte[(int) length];
                raf.readFully(chunk);
                UploadPartRequest req = UploadPartRequest.builder()
                        .bucket(bucket()).key(key).uploadId(uploadId)
                        .partNumber(partNumber).contentLength(length).build();
                String etag = client.uploadPart(req, RequestBody.fromBytes(chunk)).eTag();
                parts.add(CompletedPart.builder().partNumber(partNumber).eTag(etag).build());
                position += length;
                partNumber++;
            }
            client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket()).key(key).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());
        } catch (Exception e) {
            try {
                client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                        .bucket(bucket()).key(key).uploadId(uploadId).build());
            } catch (Exception abortEx) {
                log.warn("Failed to abort multipart upload {}: {}", uploadId, abortEx.getMessage());
            }
            throw new RuntimeException("S3 multipart upload failed: " + key, e);
        }
    }

    @Override
    public InputStream stream(String key) {
        ResponseBytes<?> bytes = client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket()).key(key).build());
        return new ByteArrayInputStream(bytes.asByteArray());
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(key).build());
    }

    @Override
    public String type() {
        return "s3";
    }

    private String bucket() {
        return fileProperties.getS3().getBucket();
    }

    @PreDestroy
    public void destroy() {
        if (client != null) client.close();
    }
}