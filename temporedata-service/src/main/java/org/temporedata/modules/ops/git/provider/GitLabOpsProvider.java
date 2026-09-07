package org.temporedata.modules.ops.git.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.temporedata.modules.ops.git.OpsException;
import org.temporedata.modules.ops.git.OpsHttpClient;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GitLab Ops provider adapter (P2-10). Uses a personal access token
 * (PRIVATE-TOKEN via the private_token query parameter).
 */
@Component
public class GitLabOpsProvider implements OpsProvider {

    private static final String API = "https://gitlab.com/api/v4";

    private final OpsHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitLabOpsProvider(OpsHttpClient client) {
        this.client = client;
    }

    @Override
    public String providerId() {
        return "gitlab";
    }

    @Override
    public String providerName() {
        return "GitLab";
    }

    @Override
    public String authorizeUrl(String callbackUrl) {
        return "https://gitlab.com/oauth/authorize?scope=api&response_type=code".concat(
                callbackUrl == null ? "" : "&redirect_uri=" + callbackUrl);
    }

    @Override
    public boolean testConnection(String repoRef, String branch, String token) {
        if (token == null || token.isBlank()) {
            throw new OpsException("未配置访问令牌，跳过连通性校验");
        }
        OpsHttpClient.Result r = client.get(API + "/projects/" + encode(repoRef) + tok(token), token);
        return r.status >= 200 && r.status < 300;
    }

    @Override
    public String repoUrl(String repoRef) {
        return "https://gitlab.com/" + repoRef;
    }

    @Override
    public String getFile(String repoRef, String branch, String path, String token) {
        if (token == null || token.isBlank() || path == null || path.isBlank()) {
            throw new OpsException("未配置令牌或无部署脚本路径");
        }
        String url = API + "/projects/" + encode(repoRef) + "/repository/files/"
                + encode(path) + "/raw?ref=" + branch + tok(token);
        OpsHttpClient.Result r = client.get(url, token);
        if (r.status >= 200 && r.status < 300) return r.body;
        throw new OpsException("读取文件失败(HTTP " + r.status + "): " + path);
    }

    @Override
    public String createWebhook(String repoRef, String token, String webhookUrl, String secret) {
        if (token == null || token.isBlank()) {
            throw new OpsException("未配置访问令牌，无法注册 Webhook");
        }
        String body = "{\"url\":\"" + webhookUrl + "\",\"push_events\":true,\"enable_ssl_verification\":false,"
                + "\"token\":\"" + secret + "\"}";
        OpsHttpClient.Result r = client.post(API + "/projects/" + encode(repoRef) + "/hooks" + tok(token), token, body);
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
            String url = API + "/projects/" + encode(repoRef) + "/hooks/" + webhookId + tok(token);
            client.request("DELETE", url, token, null);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    @Override
    public String latestCommitHint(String repoRef, String branch, String token) {
        if (token == null || token.isBlank()) return "";
        String url = API + "/projects/" + encode(repoRef) + "/repository/commits?ref_name="
                + branch + "&per_page=1" + tok(token);
        OpsHttpClient.Result r = client.get(url, token);
        if (r.status >= 200 && r.status < 300) {
            try {
                JsonNode n = mapper.readTree(r.body);
                if (n.isArray() && n.size() > 0) {
                    return n.get(0).path("short_id").asText()
                            + " " + n.get(0).path("title").asText("");
                }
            } catch (Exception e) {
                // fallthrough
            }
        }
        return "";
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("%2F", "%2F");
    }

    private String tok(String token) {
        return (token == null || token.isBlank()) ? "" : "&private_token=" + token;
    }
}