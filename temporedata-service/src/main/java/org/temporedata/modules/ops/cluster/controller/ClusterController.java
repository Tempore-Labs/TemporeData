package org.temporedata.modules.ops.cluster.controller;

import org.temporedata.modules.ops.cluster.entity.ClusterEntity;
import org.temporedata.modules.ops.cluster.service.ClusterService;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    @GetMapping
    public BaseResponse<List<ClusterEntity>> list() {
        return BaseResponse.success(clusterService.list());
    }

    @GetMapping("/{id}")
    public BaseResponse<ClusterEntity> get(@PathVariable String id) {
        return BaseResponse.success(clusterService.get(id));
    }

    @PostMapping
    public BaseResponse<ClusterEntity> create(@RequestBody ClusterEntity entity) {
        return BaseResponse.success(clusterService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<ClusterEntity> update(@PathVariable String id, @RequestBody ClusterEntity entity) {
        return BaseResponse.success(clusterService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        clusterService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/{clusterId}/nodes")
    public BaseResponse<List<Map<String, Object>>> getNodes(@PathVariable String clusterId) {
        return BaseResponse.success(clusterService.getNodes(clusterId));
    }

    @PostMapping("/{clusterId}/nodes")
    public BaseResponse<Map<String, Object>> addNode(@PathVariable String clusterId,
                                                      @RequestBody Map<String, Object> nodeReq) {
        return BaseResponse.success(clusterService.addNode(clusterId, nodeReq));
    }

    @PostMapping("/nodes/{nodeId}/install")
    public BaseResponse<Map<String, Object>> installAgent(@PathVariable String nodeId) {
        return BaseResponse.success(clusterService.installAgent(nodeId));
    }

    @GetMapping("/nodes/{nodeId}/status")
    public BaseResponse<Map<String, Object>> checkNodeStatus(@PathVariable String nodeId) {
        return BaseResponse.success(clusterService.checkNodeStatus(nodeId));
    }
}