package org.temporedata.modules.integration.file.storage;

import org.temporedata.modules.integration.file.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Default local-disk provider (app.file.storage-type=local).
 * Objects live under {@code app.file.upload-dir/key}, compatible with the
 * pre-existing {@code ./uploads} layout (key == stored filename).
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFileStorageProvider implements FileStorageProvider {

    private final FileProperties fileProperties;

    private String baseDir() {
        return fileProperties.getUploadDir();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(baseDir()));
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", baseDir(), e);
        }
    }

    @Override
    public String put(Path source, String key) {
        try {
            Path target = Paths.get(baseDir(), key);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Local storage write failed: " + key, e);
        }
    }

    @Override
    public InputStream stream(String key) {
        try {
            return Files.newInputStream(Paths.get(baseDir(), key));
        } catch (IOException e) {
            throw new RuntimeException("Local storage read failed: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(Paths.get(baseDir(), key));
        } catch (IOException e) {
            log.warn("Local storage delete failed: {}", key);
        }
    }

    @Override
    public String type() {
        return "local";
    }
}