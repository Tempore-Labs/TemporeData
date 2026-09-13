package org.temporedata.integration.core.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File storage module configuration (app.file.*),
 * bound via Spring's relaxed mapping (allow-ext -> allowExt).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    private String storageType = "local";

    private String uploadDir = "./uploads";

    private long maxSizePerFile = 268435456L;

    /** Extension whitelist keyed by bizType; '*' falls back to OTHER. */
    private Map<String, List<String>> allowExt = new HashMap<>();

    /** S3-compatible target (MinIO/Ceph/OSS/COS/OBS), used when storageType=s3. */
    private S3 s3 = new S3();

    /** Transparent compression config (v1.0 compress doc). */
    private Compress compress = new Compress();

    @Data
    public static class S3 {
        private String endpoint = "";
        private String bucket = "td-files";
        private String region = "us-east-1";
        private String accessKey = "";
        private String secretKey = "";
    }

    @Data
    public static class Compress {
        private boolean enabled = true;
        private String algorithm = "gzip"; // gzip | zstd
        private int level = -1;            // gzip 0-9 / zstd 1-22; -1 = codec default
        private long minSize = 1024;
        private List<String> extendExt = List.of("csv", "json", "xml", "txt", "log", "sql", "py", "md", "yml", "yaml", "properties", "sh");
        private List<String> excludeExt = List.of("zip", "gz", "tar", "jar", "7z", "rar", "png", "jpg", "jpeg", "gif", "webp", "mp4", "mov", "mp3", "exe", "so", "dll", "xlsx", "xls");
        private List<String> disabledBizTypes = List.of("RESOURCE");

        // Image optimization (optional, lossy)
        private boolean optimizeImages = false;
        private String imageExt = "jpg,jpeg,png";
        private int maxDim = 1920;   // max width/height px
        private float quality = 0.8f; // jpeg quality 0-1

        // Small-file packaging (optional)
        private boolean packageEnabled = false;
        private int packageMinCount = 1;
    }
}