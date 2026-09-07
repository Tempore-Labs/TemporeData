package org.temporedata.modules.integration.file.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.PageRes;
import org.temporedata.api.integration.file.FileRes;
import org.temporedata.modules.integration.file.config.FileProperties;
import org.temporedata.modules.integration.file.config.FileProperties.Compress;
import org.temporedata.modules.integration.file.entity.FileEntity;
import org.temporedata.modules.integration.file.repository.FileRepository;
import org.temporedata.modules.integration.file.storage.CompressCodec;
import org.temporedata.modules.integration.file.storage.FileStorageProvider;
import org.temporedata.modules.ops.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileProperties fileProperties;
    private final FileStorageProvider storageProvider;
    private final AuditService auditService;

    // ---- Upload ----

    @Transactional
    public FileRes upload(MultipartFile file, String bizType, String bizId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > fileProperties.getMaxSizePerFile()) {
            throw new BusinessException("文件大小超过限制: " + (fileProperties.getMaxSizePerFile() / 1024 / 1024) + "MB");
        }
        String ext = extOf(file.getOriginalFilename());
        String biz = normalizeBizType(bizType);
        if (!isAllowed(biz, ext)) {
            throw new BusinessException("不允许上传该类型文件: ." + ext);
        }

        String originalName = sanitize(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        // Buffer to a temp file so providers can stream large objects without loading them in memory.
        Path tmp = Files.createTempFile("td-upload-", ".tmp");
        Path compTmp = null;
        Path imgTmp = null;
        String originalKeyRef = null;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);

            long originalSize = Files.size(tmp);
            boolean compressed = false;
            boolean imageOptimized = false;
            String algo = "NONE";
            Long storedSize = null;
            if (imageOptimizable(biz, ext, tmp)) {
                imgTmp = Files.createTempFile("td-img-", ".opt");
                boolean ok = optimizeImage(tmp, imgTmp, ext);
                long osize = ok ? Files.size(imgTmp) : 0;
                if (osize > 0 && osize < originalSize) {
                    imageOptimized = true;
                    storedSize = osize;
                    storageProvider.put(imgTmp, storedName);
                    // keep the original bytes for "保持原图" downloads
                    String originalKey = storedName + ".orig";
                    storageProvider.put(tmp, originalKey);
                    originalKeyRef = originalKey;
                } else {
                    storageProvider.put(tmp, storedName);
                }
            } else if (shouldCompress(biz, ext, originalSize)) {
                String codec = defaultCodec(fileProperties.getCompress().getAlgorithm());
                compTmp = Files.createTempFile("td-" + codec + "-", ".comp");
                CompressCodec.compress(codec, tmp, compTmp, fileProperties.getCompress().getLevel());
                long csize = Files.size(compTmp);
                if (csize < originalSize * 0.95) {
                    compressed = true;
                    algo = codec;
                    storedSize = csize;
                    storageProvider.put(compTmp, storedName);
                } else {
                    storageProvider.put(tmp, storedName);
                }
            } else {
                storageProvider.put(tmp, storedName);
            }

            String md5 = md5(tmp);
            String tenantId = tenant();
            String operator = operator();

            FileEntity entity = new FileEntity();
            entity.setName(storedName);
            entity.setOriginalName(originalName);
            entity.setFileSize(originalSize);
            entity.setFileType(file.getContentType());
            entity.setExt(ext);
            entity.setMd5(md5);
            entity.setBizType(biz);
            entity.setBizId(bizId);
            entity.setStorageKey(storedName);
            entity.setStorageType(storageProvider.type());
            entity.setStatus("READY");
            entity.setScanStatus("NONE");
            entity.setDelFlag(false);
            entity.setCompressed(compressed);
            entity.setCompressAlgo(algo);
            entity.setOriginalSize(originalSize);
            entity.setStoredSize(storedSize);
            entity.setImageOptimized(imageOptimized);
            entity.setPackaged(false);
            entity.setMemberCount(0);
            entity.setOriginalKey(originalKeyRef);
            entity.setTenantId(tenantId);
            entity.setCreateBy(operator);
            entity.setCreateTime(now());
            FileEntity saved = fileRepository.save(entity);

            audit("UPLOAD", saved.getId(),
                    "{\"name\":\"" + safeJson(originalName) + "\",\"size\":" + saved.getFileSize()
                            + ",\"compressed\":" + compressed + ",\"algo\":\"" + algo + "\",\"stored\":" + storedSize
                            + ",\"imgOpt\":" + imageOptimized + "}",
                    operator);
            return toRes(saved);
        } finally {
            Files.deleteIfExists(tmp);
            if (compTmp != null) Files.deleteIfExists(compTmp);
            if (imgTmp != null) Files.deleteIfExists(imgTmp);
        }
    }

    // ---- Small-file packaging (optional) ----

    /**
     * Package multiple files into a single zip archive stored as one object.
     * Type-biased to text/data (packageEnabled); RESOURCE disabled by default.
     */
    @Transactional
    public FileRes bundle(MultipartFile[] files, String bizType, String bizId) throws IOException {
        if (files == null || files.length == 0) {
            throw new BusinessException("打包上传至少需要一个文件");
        }
        String biz = normalizeBizType(bizType);
        boolean enabled = fileProperties.getCompress().isPackageEnabled();
        int minCount = Math.max(1, fileProperties.getCompress().getPackageMinCount());
        if (!enabled) {
            throw new BusinessException("未启用打包上传");
        }
        if (files.length < minCount) {
            throw new BusinessException("打包至少需要 " + minCount + " 个文件");
        }

        long totalSize = 0;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                totalSize += f.getSize();
                String entryName = sanitize(f.getOriginalFilename());
                zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                try (InputStream is = f.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) zos.write(buf, 0, n);
                }
                zos.closeEntry();
            }
            zos.finish();

            byte[] zipBytes = baos.toByteArray();
            Path zipTmp = Files.createTempFile("td-pack-", ".zip");
            try {
                Files.write(zipTmp, zipBytes);
                String storedName = UUID.randomUUID().toString().replace("-", "") + ".zip";
                storageProvider.put(zipTmp, storedName);

                FileEntity entity = new FileEntity();
                entity.setName(storedName);
                entity.setOriginalName("bundle_" + nowStamp() + ".zip");
                entity.setFileSize(totalSize);
                entity.setFileType("application/zip");
                entity.setExt("zip");
                entity.setMd5(md5(zipTmp));
                entity.setBizType(biz);
                entity.setBizId(bizId);
                entity.setStorageKey(storedName);
                entity.setStorageType(storageProvider.type());
                entity.setStatus("READY");
                entity.setScanStatus("NONE");
                entity.setDelFlag(false);
                entity.setCompressed(false);
                entity.setCompressAlgo("NONE");
                entity.setOriginalSize(totalSize);
                entity.setStoredSize((long) zipBytes.length);
                entity.setImageOptimized(false);
                entity.setPackaged(true);
                entity.setMemberCount(files.length);
                entity.setTenantId(tenant());
                entity.setCreateBy(operator());
                entity.setCreateTime(now());
                FileEntity saved = fileRepository.save(entity);
                audit("UPLOAD", saved.getId(),
                        "{\"name\":\"" + safeJson(saved.getOriginalName()) + "\",\"bundle\":true,\"members\":" + files.length + ",\"zip\":" + zipBytes.length + "}",
                        operator());
                return toRes(saved);
            } finally {
                Files.deleteIfExists(zipTmp);
            }
        }
    }

    // ---- Query ----

    @Transactional(readOnly = true)
    public PageRes<FileRes> page(int page, int size, String bizType, String bizId, String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createDateTime"));

        String tenantId = tenant();
        Specification<FileEntity> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("delFlag"), false));
            if (StringUtils.hasText(tenantId)) {
                ps.add(cb.or(cb.isNull(root.get("tenantId")), cb.equal(root.get("tenantId"), tenantId)));
            }
            if (StringUtils.hasText(bizType)) ps.add(cb.equal(root.get("bizType"), bizType));
            if (StringUtils.hasText(bizId)) ps.add(cb.equal(root.get("bizId"), bizId));
            if (StringUtils.hasText(keyword)) ps.add(cb.like(root.get("originalName"), "%" + keyword + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<FileEntity> result = fileRepository.findAll(spec, pageable);
        List<FileRes> items = result.getContent().stream().map(this::toRes).collect(java.util.stream.Collectors.toList());
        return PageRes.of(items, result.getTotalElements(), safePage, safeSize);
    }

    public FileEntity get(String id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在: " + id));
        if (entity.isDelFlag() || !visibleInTenant(entity)) {
            throw new BusinessException("文件不存在: " + id);
        }
        return entity;
    }

    public FileRes meta(String id) {
        return toRes(get(id));
    }

    public InputStream openStream(String id) {
        FileEntity entity = get(id);
        InputStream raw = storageProvider.stream(entity.getName());
        if (entity.isCompressed() && StringUtils.hasText(entity.getCompressAlgo())) {
            try {
                return CompressCodec.wrapRead(raw, entity.getCompressAlgo());
            } catch (IOException e) {
                throw new BusinessException("文件解压失败: " + id);
            }
        }
        return raw;
    }

    /** Stream the kept-original bytes of an image-optimized file, if available. */
    public InputStream openOriginalStream(String id) {
        FileEntity entity = get(id);
        if (!StringUtils.hasText(entity.getOriginalKey())) {
            throw new BusinessException("该文件未保留原图");
        }
        return storageProvider.stream(entity.getOriginalKey());
    }

    /**
     * Download a single member from a packaged (zip) file. Member names are
     * validated to prevent zip-slip / path traversal.
     */
    public InputStream openMember(String id, String memberName) {
        FileEntity entity = get(id);
        if (!entity.isPackaged()) {
            throw new BusinessException("该文件不是打包文件");
        }
        if (!StringUtils.hasText(memberName) || memberName.contains("/") || memberName.contains("\\")
                || memberName.equals("..") || memberName.startsWith(".")) {
            throw new BusinessException("非法的打包成员名");
        }
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(storageProvider.stream(entity.getName()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (memberName.equals(entry.getName())) {
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = zis.read(buf)) != -1) bos.write(buf, 0, n);
                    return new java.io.ByteArrayInputStream(bos.toByteArray());
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取打包成员失败: " + e.getMessage());
        }
        throw new BusinessException("打包成员不存在: " + memberName);
    }

    // ---- Delete (owner-only for option A) ----

    @Transactional
    public void delete(String id) {
        FileEntity entity = get(id);
        String operator = operator();
        if (entity.getCreateBy() == null || entity.getCreateBy().isBlank()
                || !entity.getCreateBy().equals(operator)) {
            throw new BusinessException("无权删除该文件：仅文件属主可删除");
        }
        entity.setDelFlag(true);
        entity.setStatus("DELETED");
        fileRepository.save(entity);
        try {
            storageProvider.delete(entity.getName());
        } catch (Exception e) {
            log.warn("Failed to delete physical file {}: {}", id, e.getMessage());
        }
        // also remove the kept-original object if any
        if (StringUtils.hasText(entity.getOriginalKey())) {
            try {
                storageProvider.delete(entity.getOriginalKey());
            } catch (Exception e) {
                log.warn("Failed to delete original {}: {}", entity.getOriginalKey(), e.getMessage());
            }
        }
        audit("DELETE", id, "{\"name\":\"" + safeJson(entity.getOriginalName()) + "\"}", operator);
    }

    // ---- helpers ----

    private FileRes toRes(FileEntity e) {
        FileRes r = new FileRes();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setOriginalName(e.getOriginalName());
        r.setFileSize(e.getFileSize());
        r.setFileType(e.getFileType());
        r.setExt(e.getExt());
        r.setMd5(e.getMd5());
        r.setBizType(e.getBizType());
        r.setBizId(e.getBizId());
        r.setCreateBy(e.getCreateBy());
        r.setCreateTime(e.getCreateTime());
        r.setImageOptimized(e.isImageOptimized());
        r.setPackaged(e.isPackaged());
        r.setMemberCount(e.getMemberCount());
        return r;
    }

    /** List member file names of a packaged (zip) file. */
    public List<String> members(String id) {
        FileEntity entity = get(id);
        if (!entity.isPackaged()) {
            throw new BusinessException("该文件不是打包文件");
        }
        List<String> names = new ArrayList<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(storageProvider.stream(entity.getName()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        } catch (IOException e) {
            throw new BusinessException("读取打包成员失败: " + e.getMessage());
        }
        return names;
    }

    // ---- Storage governance (disk cleanup + stats) ----

    /** Aggregate storage accounting for disk governance (based on stored_size). */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<FileEntity> active = fileRepository.findAll().stream()
                .filter(e -> !e.isDelFlag()).collect(java.util.stream.Collectors.toList());
        long files = active.size();
        long originalBytes = active.stream()
                .filter(e -> e.getOriginalSize() != null).mapToLong(FileEntity::getOriginalSize).sum();
        long storedBytes = active.stream()
                .filter(e -> e.getStoredSize() != null).mapToLong(FileEntity::getStoredSize).sum();
        Map<String, Long> byBizType = new HashMap<>();
        for (FileEntity e : active) {
            if (e.getBizType() != null) byBizType.merge(e.getBizType(), 1L, Long::sum);
        }
        long savedPct = originalBytes > 0 ? Math.round((1 - storedBytes / (double) originalBytes) * 100) : 0;
        Map<String, Object> s = new java.util.LinkedHashMap<>();
        s.put("fileCount", files);
        s.put("originalBytes", originalBytes);
        s.put("storedBytes", storedBytes);
        s.put("savedPercent", savedPct);
        s.put("byBizType", byBizType);
        return s;
    }

    /**
     * Reconcile storage: delete any physical object still present for rows marked
     * DELETED (soft-delete leftovers), keeping the rows for audit. Returns the
     * number of reconciled rows.
     */
    @Transactional
    public int cleanup() {
        int removed = 0;
        for (FileEntity e : fileRepository.findAll()) {
            if (!e.isDelFlag()) continue;
            try {
                storageProvider.delete(e.getName());
            } catch (Exception ex) {
                log.warn("cleanup delete failed {}: {}", e.getName(), ex.getMessage());
            }
            if (StringUtils.hasText(e.getOriginalKey())) {
                try {
                    storageProvider.delete(e.getOriginalKey());
                } catch (Exception ex) {
                    log.warn("cleanup delete original {}: {}", e.getOriginalKey(), ex.getMessage());
                }
            }
            removed++;
        }
        return removed;
    }

    private boolean isAllowed(String bizType, String ext) {
        if (ext.isEmpty()) return false;
        Map<String, List<String>> allow = fileProperties.getAllowExt();
        List<String> allowed = allow.getOrDefault(bizType, allow.getOrDefault("OTHER", Collections.emptyList()));
        return allowed.stream().anyMatch(a -> a.equalsIgnoreCase(ext));
    }

    private String extOf(String name) {
        if (name == null) return "";
        String n = name.trim();
        int dot = n.lastIndexOf('.');
        if (dot < 0 || dot == n.length() - 1) return "";
        String ext = n.substring(dot + 1);
        // handle composite like .tar.gz -> keep "tar.gz"
        if ("gz".equalsIgnoreCase(ext) && dot > 0) {
            int prev = n.lastIndexOf('.', dot - 1);
            if (prev >= 0) {
                String prevExt = n.substring(prev + 1, dot);
                if ("tar".equalsIgnoreCase(prevExt)) return (prevExt + "." + ext).toLowerCase();
            }
        }
        return ext.toLowerCase();
    }

    private String normalizeBizType(String bizType) {
        return StringUtils.hasText(bizType) ? bizType.toUpperCase() : "OTHER";
    }

    private String sanitize(String name) {
        if (name == null) return "unnamed";
        String cleaned = name.replace('\\', '/');
        int slash = cleaned.lastIndexOf('/');
        cleaned = (slash >= 0) ? cleaned.substring(slash + 1) : cleaned;
        cleaned = cleaned.replaceAll("[\\r\\n\\t\\x00-\\x1F]", "").trim();
        if (cleaned.isEmpty()) return "unnamed";
        if (cleaned.length() > 200) cleaned = cleaned.substring(cleaned.length() - 200);
        return cleaned;
    }

    private String md5(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Transparent compression (v1.0 compress doc) ----

    private boolean shouldCompress(String bizType, String ext, long size) {
        if (!fileProperties.getCompress().isEnabled()) return false;
        if (size < fileProperties.getCompress().getMinSize()) return false;
        if (fileProperties.getCompress().getDisabledBizTypes().contains(bizType)) return false;
        if (fileProperties.getCompress().getExcludeExt().contains(ext)) return false;
        return fileProperties.getCompress().getExtendExt().contains(ext);
    }

    private String defaultCodec(String algo) {
        if (algo != null && "zstd".equalsIgnoreCase(algo)) return "zstd";
        return "gzip";
    }

    // ---- Image optimization (optional, lossy) ----

    private boolean imageOptimizable(String bizType, String ext, Path file) {
        Compress cfg = fileProperties.getCompress();
        boolean enabled = cfg.isOptimizeImages();
        if (!enabled) { log.info("[imgOpt] disabled: optimizeImages={}", enabled); return false; }
        if (cfg.getDisabledBizTypes().contains(bizType)) { log.info("[imgOpt] bizType disabled: {}", bizType); return false; }
        if (ext == null) return false;
        boolean image = List.of(cfg.getImageExt().split(",")).contains(ext.toLowerCase());
        log.info("[imgOpt] enabled={} ext={} isImage={} bizType={}", enabled, ext, image, bizType);
        return image;
    }

    /** Lossy optimize: resize > maxDim and/or re-encode at quality. Returns false to fall back to original. */
    private boolean optimizeImage(Path src, Path tgt, String ext) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(src.toFile());
            if (img == null) return false;
            int maxDim = Math.max(1, fileProperties.getCompress().getMaxDim());
            int w = img.getWidth(), h = img.getHeight();
            if (Math.max(w, h) > maxDim) {
                double scale = maxDim / (double) Math.max(w, h);
                int nw = (int) (w * scale), nh = (int) (h * scale);
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, nw, nh, null);
                g.dispose();
                img = scaled;
            }
            if ("jpg".equals(ext) || "jpeg".equals(ext)) {
                java.util.Iterator<javax.imageio.ImageWriter> ws = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
                if (!ws.hasNext()) return false;
                javax.imageio.ImageWriter writer = ws.next();
                javax.imageio.ImageWriteParam p = writer.getDefaultWriteParam();
                p.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(Math.max(0f, Math.min(1f, fileProperties.getCompress().getQuality())));
                try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(tgt.toFile())) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(img, null, null), p);
                } finally {
                    writer.dispose();
                }
            } else {
                if (!javax.imageio.ImageIO.write(img, ext, tgt.toFile())) return false;
            }
            return Files.size(tgt) > 0;
        } catch (Exception e) {
            log.warn("Image optimize failed, using original: {}", e.getMessage());
            return false;
        }
    }

    private String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private boolean visibleInTenant(FileEntity e) {
        String tenant = tenant();
        return !StringUtils.hasText(tenant) || e.getTenantId() == null || tenant.equals(e.getTenantId());
    }

    private String tenant() {
        return org.temporedata.security.context.TenantContext.getTenantId();
    }

    private String operator() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? "anonymous"
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String safeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private void audit(String action, String id, String detail, String operator) {
        try {
            auditService.record("FILE", action, "FILE", id, operator, null, null, "SUCCESS", detail);
        } catch (Exception e) {
            log.warn("Audit record failed for file {} {}: {}", action, id, e.getMessage());
        }
    }
}