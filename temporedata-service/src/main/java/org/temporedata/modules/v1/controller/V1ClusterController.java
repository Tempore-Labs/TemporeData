package org.temporedata.modules.v1.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.temporedata.api.contract.ApiResponse;
import org.temporedata.api.contract.ErrorCode;

import java.util.List;

/**
 * v1 cluster contract (template domain). Defines the endpoint + response shape for
 * {@code /api/v1/clusters}. Domain services land later; writes return NOT_IMPLEMENTED
 * so consumers see a stable envelope instead of a 404.
 */
@RestController
@RequestMapping("/api/v1/clusters")
public class V1ClusterController {

    @GetMapping
    public ApiResponse<List<Object>> list() {
        return ApiResponse.ok(List.of()); // scaffold: empty list; wire domain service later
    }

    @GetMapping("/{id}")
    public ApiResponse<Object> get(@PathVariable String id) {
        return ApiResponse.ok(java.util.Map.of("id", id,
                "name", "cluster-" + id, "type", "UNKNOWN", "status", "UNKNOWN"));
    }

    @GetMapping("/{id}/nodes")
    public ApiResponse<List<Object>> nodes(@PathVariable String id) {
        return ApiResponse.ok(List.of());
    }

    @PostMapping
    public ApiResponse<Void> create() {
        return ApiResponse.error(ErrorCode.SYS_NOT_IMPLEMENTED, null, null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id) {
        return ApiResponse.error(ErrorCode.SYS_NOT_IMPLEMENTED, null, null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        return ApiResponse.error(ErrorCode.SYS_NOT_IMPLEMENTED, null, null);
    }
}