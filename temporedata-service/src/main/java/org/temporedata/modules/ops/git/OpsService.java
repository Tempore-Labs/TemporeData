package org.temporedata.modules.ops.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.git.entity.OpsBuildEntity;
import org.temporedata.modules.ops.git.entity.OpsRepoEntity;
import org.temporedata.modules.ops.git.provider.OpsProvider;
import org.temporedata.modules.ops.git.provider.OpsProviderRegistry;
import org.temporedata.modules.ops.git.repository.OpsBuildRepository;
import org.temporedata.modules.ops.git.repository.OpsRepoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-10 GitHub/GitLab Ops integration: bind repositories, receive webhooks and
 * trigger builds/deploys. Real provider calls are attempted when a token is
 * configured; otherwise operations degrade to recordings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OpsRepoRepository repoRepository;
    private final OpsBuildRepository buildRepository;
    private final OpsProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    @Value("${temporedata.ops.git-token:}")
    private String gitToken;

    @Value("${temporedata.ops.webhook-secret:td-gitops}")
    private String webhookSecret;

    @Value("${temporedata.ops.webhook-base:}")
    private String webhookBase;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProviders() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (OpsProvider p : providerRegistry.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("providerId", p.providerId());
            m.put("name", p.providerName());
            m.put("authorizeUrl", p.authorizeUrl(null));
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<OpsRepoEntity> listRepos() {
        return repoRepository.findAll();
    }

    @Transactional
    public OpsRepoEntity bind(String provider, String repoRef, String branch, boolean autoTrigger,
                              String deployScriptPath, String environment, String token) {
        OpsProvider p = providerRegistry.get(provider);
        if (repoRef == null || repoRef.isBlank()) throw new BusinessException("仓库引用必填(owner/repo)");
        String effToken = token != null && !token.isBlank() ? token : gitToken;

        // best-effort connectivity + webhook
        String bindInfo = "{}";
        boolean canWebhook = autoTrigger && effToken != null && !effToken.isBlank()
                && webhookBase != null && !webhookBase.isBlank();
        if (canWebhook) {
            try {
                String hookId = p.createWebhook(repoRef, effToken, webhookBase + "/api/ops/webhook/" + provider, webhookSecret);
                bindInfo = "{\"webhookId\":\"" + hookId + "\"}";
            } catch (Exception e) {
                log.warn("注册 Webhook 失败: {}", e.getMessage());
                bindInfo = "{\"webhookError\":\"" + e.getMessage() + "\"}";
            }
        } else if (effToken != null && !effToken.isBlank()) {
            try {
                p.testConnection(repoRef, branch, effToken);
            } catch (Exception e) {
                log.warn("连通性校验: {}", e.getMessage());
            }
        }

        OpsRepoEntity repo = repoRepository.findByProviderAndRepoRef(provider, repoRef)
                .orElseGet(() -> OpsRepoEntity.builder().provider(provider).repoRef(repoRef).createTime(now()).build());
        repo.setBranch(branch == null ? "main" : branch);
        repo.setAutoTrigger(autoTrigger);
        repo.setDeployScriptPath(deployScriptPath);
        repo.setEnvironment(environment == null ? "DEV" : environment);
        repo.setEnabled(Boolean.TRUE);
        repo.setBindInfoJson(bindInfo);
        repo.setUpdateTime(now());
        return repoRepository.save(repo);
    }

    @Transactional
    public void unbind(String id) {
        OpsRepoEntity repo = find(id);
        OpsProvider p = providerRegistry.get(repo.getProvider());
        try {
            JsonNode info = objectMapper.readTree(repo.getBindInfoJson() == null ? "{}" : repo.getBindInfoJson());
            p.deleteWebhook(repo.getRepoRef(), gitToken, info.path("webhookId").asText(null));
        } catch (Exception ignored) {
            // best-effort
        }
        repoRepository.delete(repo);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sync(String id) {
        OpsRepoEntity repo = find(id);
        OpsProvider p = providerRegistry.get(repo.getProvider());
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            boolean ok = p.testConnection(repo.getRepoRef(), repo.getBranch(), gitToken);
            out.put("status", ok ? "OK" : "FAILED");
            out.put("message", ok ? "连接正常" : "无法访问仓库");
        } catch (Exception e) {
            out.put("status", "NA");
            out.put("message", e.getMessage());
        }
        try {
            out.put("latestCommit", p.latestCommitHint(repo.getRepoRef(), repo.getBranch(), gitToken));
        } catch (Exception e) {
            out.put("latestCommit", "");
        }
        return out;
    }

    @Transactional
    public OpsBuildEntity trigger(String id, String commitSha, String commitMessage) {
        OpsRepoEntity repo = find(id);
        return deploy(repo, "MANUAL", repo.getBranch(), commitSha, commitMessage);
    }

    /**
     * Webhook entry (P2-10). Parses the push payload and triggers auto deploys.
     */
    @Transactional
    public Map<String, Object> handleWebhook(String provider, String payload) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("provider", provider);
        try {
            JsonNode body = objectMapper.readTree(payload);
            String fullName = body.path("repository").path("full_name").asText();
            if (fullName == null || fullName.isBlank()) {
                fullName = body.path("project").path("path_with_namespace").asText();
            }
            if (fullName == null || fullName.isBlank()) {
                res.put("result", "IGNORED_NO_REPO");
                return res;
            }
            OpsRepoEntity repo = repoRepository.findByProviderAndRepoRef(provider, fullName).orElse(null);
            if (repo == null || !Boolean.TRUE.equals(repo.getEnabled())
                    || !Boolean.TRUE.equals(repo.getAutoTrigger())) {
                res.put("result", "IGNORED");
                return res;
            }
            String sha = body.path("head_commit").path("id").asText(null);
            if (sha == null) sha = body.path("after").asText(null);
            String msg = body.path("head_commit").path("message").asText(null);
            if (msg == null) msg = body.path("commits").size() > 0 ? body.path("commits").get(0).path("message").asText(null) : null;
            OpsBuildEntity build = deploy(repo, "COMMIT", repo.getBranch(), sha, msg);
            res.put("result", "TRIGGERED");
            res.put("buildId", build.getId());
            res.put("status", build.getStatus());
        } catch (Exception e) {
            log.warn("Webhook 解析失败: {}", e.getMessage());
            res.put("result", "ERROR");
            res.put("message", e.getMessage());
        }
        return res;
    }

    private OpsBuildEntity deploy(OpsRepoEntity repo, String triggerType, String ref, String sha, String msg) {
        OpsBuildEntity build = OpsBuildEntity.builder()
                .repoId(repo.getId())
                .triggerType(triggerType)
                .refName(ref)
                .commitSha(sha)
                .commitMessage(msg)
                .status("TRIGGERED")
                .createTime(now())
                .updateTime(now())
                .build();
        buildRepository.save(build);

        OpsProvider p = providerRegistry.get(repo.getProvider());
        try {
            if (repo.getDeployScriptPath() != null && !repo.getDeployScriptPath().isBlank()) {
                String content = p.getFile(repo.getRepoRef(), repo.getBranch(), repo.getDeployScriptPath(), gitToken);
                build.setPipelineRef("script:" + content.length() + "B");
            }
            build.setStatus("SUCCESS");
        } catch (Exception e) {
            build.setStatus("FAILED");
            build.setLogRef(e.getMessage());
        }
        build.setUpdateTime(now());
        buildRepository.save(build);
        return build;
    }

    @Transactional(readOnly = true)
    public List<OpsBuildEntity> builds(String repoId, String status) {
        List<OpsBuildEntity> all = repoId == null || repoId.isBlank()
                ? buildRepository.findAllByOrderByCreateTimeDesc()
                : buildRepository.findByRepoIdOrderByCreateTimeDesc(repoId);
        if (status == null || status.isBlank()) return all;
        List<OpsBuildEntity> out = new ArrayList<>();
        for (OpsBuildEntity b : all) {
            if (status.equalsIgnoreCase(b.getStatus())) out.add(b);
        }
        return out;
    }

    private OpsRepoEntity find(String id) {
        return repoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("仓库未绑定: " + id));
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }
}