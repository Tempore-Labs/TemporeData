package org.temporedata.modules.dev.workflow.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * HTTP callback node executor (real outbound call).
 */
@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    public HttpNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> type() {
        return Set.of("HTTP", "WEBHOOK");
    }

    @Override
    public NodeRunResult execute(WorkflowNodeEntity node, NodeExecutionContext ctx) {
        String url = node.getHttpUrl();
        if (url == null || url.isBlank()) {
            return NodeRunResult.failed("HTTP node has empty httpUrl");
        }

        String method = node.getHttpMethod() == null ? "GET" : node.getHttpMethod().toUpperCase();
        int timeout = node.getTimeoutSeconds() != null && node.getTimeoutSeconds() > 0
                ? node.getTimeoutSeconds() : 30;

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeout));

            applyHeaders(builder, node.getHttpHeaders());

            HttpRequest request;
            switch (method) {
                case "POST": request = builder.POST(HttpRequest.BodyPublishers.noBody()).build(); break;
                case "PUT": request = builder.PUT(HttpRequest.BodyPublishers.noBody()).build(); break;
                case "DELETE": request = builder.DELETE().build(); break;
                default: request = builder.GET().build(); break;
            }

            HttpResponse<String> resp = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                String body = resp.body() == null ? "" : resp.body();
                String snippet = body.length() > 200 ? body.substring(0, 200) : body;
                return NodeRunResult.ok("HTTP " + method + " " + resp.statusCode() + " : " + snippet,
                        resp.statusCode());
            }
            return NodeRunResult.failed("HTTP " + method + " failed with status " + resp.statusCode() + " : " + resp.body());
        } catch (Exception e) {
            log.warn("HTTP node failed: {}", e.getMessage());
            return NodeRunResult.failed("HTTP call error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void applyHeaders(HttpRequest.Builder builder, String headersJson) throws Exception {
        if (headersJson == null || headersJson.isBlank()) return;
        Map<String, String> headers = objectMapper.readValue(headersJson, Map.class);
        headers.forEach(builder::header);
    }
}