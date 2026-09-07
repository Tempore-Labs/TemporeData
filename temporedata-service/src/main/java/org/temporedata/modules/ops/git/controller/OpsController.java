package org.temporedata.modules.ops.git.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.pojos.BaseResponse;
import org.temporedata.modules.ops.git.OpsService;
import org.temporedata.modules.ops.git.entity.OpsBuildEntity;
import org.temporedata.modules.ops.git.entity.OpsRepoEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2-10 Ops integration REST API (GitHub/GitLab).
 */
@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {

    private final OpsService opsService;

    @GetMapping("/providers")
    public BaseResponse<List<Map<String, Object>>> providers() {
        return BaseResponse.success(opsService.listProviders());
    }

    @GetMapping("/repos")
    public BaseResponse<List<OpsRepoEntity>> repos() {
        return BaseResponse.success(opsService.listRepos());
    }

    @PostMapping("/repos")
    public BaseResponse<OpsRepoEntity> bind(@RequestBody Map<String, Object> body) {
        return BaseResponse.success(opsService.bind(
                str(body.get("provider")),
                str(body.get("repoRef")),
                str(body.get("branch")),
                bool(body.get("autoTrigger")),
                str(body.get("deployScriptPath")),
                str(body.get("environment")),
                str(body.get("token"))));
    }

    @DeleteMapping("/repos/{id}")
    public BaseResponse<Void> unbind(@PathVariable String id) {
        opsService.unbind(id);
        return BaseResponse.success();
    }

    @PostMapping("/repos/{id}/sync")
    public BaseResponse<Map<String, Object>> sync(@PathVariable String id) {
        return BaseResponse.success(opsService.sync(id));
    }

    @PostMapping("/repos/{id}/trigger")
    public BaseResponse<OpsBuildEntity> trigger(@PathVariable String id) {
        return BaseResponse.success(opsService.trigger(id, null, null));
    }

    @GetMapping("/builds")
    public BaseResponse<List<OpsBuildEntity>> builds(
            @RequestParam(required = false) String repoId,
            @RequestParam(required = false) String status) {
        return BaseResponse.success(opsService.builds(repoId, status));
    }

    @PostMapping("/webhook/{provider}")
    public BaseResponse<Map<String, Object>> webhook(@PathVariable String provider, @RequestBody String payload) {
        return BaseResponse.success(opsService.handleWebhook(provider, payload));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private boolean bool(Object o) {
        return o != null && Boolean.parseBoolean(String.valueOf(o));
    }
}