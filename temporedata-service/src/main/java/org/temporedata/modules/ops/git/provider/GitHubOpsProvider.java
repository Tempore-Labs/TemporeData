package org.temporedata.modules.ops.git.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.temporedata.modules.ops.git.OpsException;
import org.temporedata.modules.ops.git.OpsHttpClient;
import org.springframework.stereotype.Component;

/**
 * GitHub Ops provider adapter (P2-10). Uses a classic personal access token.
 */
@Component
public class GitHubOpsProvider implements OpsProvider {

    private static final String API = "https://api.github.com";

    private final OpsHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubOpsProvider(OpsHttpClient client) {
        this.client = client;
    }

    @Override
    public String providerId() {
        return "github";
    }

    @Override
    public String providerName() {
        return "GitHub";
    }

    @Override
    public String authorizeUrl(String callbackUrl) {
        return "https://github.com/login/oauth/authorize?scope=repo,admin:repo_hook".concat(
                callbackUrl == null ? "" : "&redirect_uri=" + callbackUrl);
    }

    @Override
    public boolean testConnection(String repoRef, String branch, String token) {
        if (token == null || token.isBlank()) {
            throw new OpsException("未配置访问令牌，跳过连通性校验");
        }
        OpsHttpClient.Result r = client.get(API + "/repos/" + repoRef, token);
        return r.status >= 200 && r.status < 300;
    }

    @Override
    public String repoUrl(String repoRef) {
        return "https://github.com/" + repoRef;
    }

    @Override
    public String getFile(String repoRef, String branch, String path, String token) {
        if (token == null || token.isBlank() || path == null || path.isBlank()) {
            throw new OpsException("未配置令牌或无部署脚本路径");
        }
        String url = "https://raw.githubusercontent.com/" + repoRef + "/" + branch + "/" + path;
        OpsHttpClient.Result r = client.get(url, null);
        if (r.status >= 200 && r.status < 300) return r.body;
        throw new OpsException("读取文件失败(HTTP " + r.status + "): " + path);
    }

    @Override
    public String createWebhook(String repoRef, String token, String webhookUrl, String secret) {
        if (token == null || token.isBlank()) {
            throw new OpsException("未配置访问令牌，无法注册 Webhook");
        }
        String body = "{\"name\":\"web\",\"active\":true,\"events\":[\"push\"],\"config\":{"
                + "\"url\":\"" + webhookUrl + "\",\"content_type\":\"json\",\"secret\":\"" + secret + "\"}}";
        OpsHttpClient.Result r = client.post(API + "/repos/" + repoRef + "/hooks", token, body);
        if (r.status >= 200 && r.status < 300) {
            try {
                JsonNode n = mapper.readTree(r.body);
                return n.path("id").asText();
            } catch (Exception e) {
                return "unknown";
            }
        }
        throw new OpsException("创建 Webhook 失败(HTTP " + r.status + ")");
    }

    @Override
    public void deleteWebhook(String repoRef, String token, String webhookId) {
        if (token == null || token.isBlank() || webhookId == null || webhookId.isBlank()) return;
        try {
            client.request("DELETE", API + "/repos/" + repoRef + "/hooks/" + webhookId, token, null);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    @Override
    public String latestCommitHint(String repoRef, String branch, String token) {
        if (token == null || token.isBlank()) return "";
        OpsHttpClient.Result r = client.get(API + "/repos/" + repoRef + "/commits?sha=" + branch + "&per_page=1", token);
        if (r.status >= 200 && r.status < 300) {
            try {
                JsonNode n = mapper.readTree(r.body);
                if (n.isArray() && n.size() > 0) {
                    return n.get(0).path("sha").asText().substring(0, 7)
                            + " " + n.get(0).path("commit").path("message").asText("").split("\n")[0];
                }
            } catch (Exception e) {
                // fallthrough
            }
        }
        return "";
    }
}