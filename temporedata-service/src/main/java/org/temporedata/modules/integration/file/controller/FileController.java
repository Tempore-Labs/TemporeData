package org.temporedata.modules.integration.file.controller;

import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.api.base.pojos.PageRes;
import org.temporedata.api.integration.file.FileRes;
import org.temporedata.modules.integration.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public BaseResponse<FileRes> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "bizType", required = false) String bizType,
                                        @RequestParam(value = "bizId", required = false) String bizId) throws IOException {
        return BaseResponse.success(fileService.upload(file, bizType, bizId));
    }

    @PostMapping("/bundle")
    public BaseResponse<FileRes> bundle(@RequestParam("files") MultipartFile[] files,
                                        @RequestParam(value = "bizType", required = false) String bizType,
                                        @RequestParam(value = "bizId", required = false) String bizId) throws IOException {
        return BaseResponse.success(fileService.bundle(files, bizType, bizId));
    }

    @GetMapping
    public BaseResponse<PageRes<FileRes>> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "size", defaultValue = "10") int size,
                                               @RequestParam(value = "bizType", required = false) String bizType,
                                               @RequestParam(value = "bizId", required = false) String bizId,
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResponse.success(fileService.page(page, size, bizType, bizId, keyword));
    }

    @GetMapping("/{id}/meta")
    public BaseResponse<FileRes> meta(@PathVariable String id) {
        return BaseResponse.success(fileService.meta(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        FileRes meta = fileService.meta(id);
        InputStream in = fileService.openStream(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + rfc5987(meta.getOriginalName()) + "\"")
                .body(new InputStreamResource(in));
    }

    @GetMapping("/{id}/original")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable String id) {
        FileRes meta = fileService.meta(id);
        InputStream in = fileService.openOriginalStream(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + rfc5987(meta.getOriginalName()) + "\"")
                .body(new InputStreamResource(in));
    }

    @GetMapping("/{id}/member")
    public ResponseEntity<Resource> downloadMember(@PathVariable String id,
                                                   @RequestParam("name") String name) {
        InputStream in = fileService.openMember(id, name);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + rfc5987(name) + "\"")
                .body(new InputStreamResource(in));
    }

    @GetMapping("/{id}/members")
    public BaseResponse<java.util.List<String>> members(@PathVariable String id) {
        return BaseResponse.success(fileService.members(id));
    }

    @GetMapping("/stats")
    public BaseResponse<java.util.Map<String, Object>> stats() {
        return BaseResponse.success(fileService.stats());
    }

    @PostMapping("/cleanup")
    public BaseResponse<java.util.Map<String, Object>> cleanup() {
        int removed = fileService.cleanup();
        return BaseResponse.success(java.util.Map.of("reconciled", removed));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        fileService.delete(id);
        return BaseResponse.success();
    }

    private String rfc5987(String name) {
        if (name == null) return "file";
        // ASCII-safe fallback plus RFC 5987 UTF-8 filename* for non-ASCII.
        String fileNameUtf8 = java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
        return "filename*=UTF-8''" + fileNameUtf8;
    }
}