package org.temporedata.modules.integration.file.storage;

import org.temporedata.modules.integration.file.config.FileProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the {@link FileStorageProvider} implementation from
 * {@code app.file.storage-type} (default local). Business code is unaffected.
 */
@Configuration
public class FileStorageProviderConfig {

    @Bean
    public FileStorageProvider fileStorageProvider(FileProperties fileProperties,
                                                   @Value("${app.file.storage-type:local}") String storageType) {
        switch (storageType == null ? "local" : storageType.toLowerCase()) {
            case "s3":
                return new S3FileStorageProvider(fileProperties);
            case "local":
            default:
                return new LocalFileStorageProvider(fileProperties);
        }
    }
}