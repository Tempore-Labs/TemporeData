package org.temporedata.modules.ops.git;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP(S) client for the Ops providers (P2-10). Returns HTTP code + body
 * without depending on extra HTTP libraries.
 */
@Component
public class OpsHttpClient {

    public Result get(String url, String token) {
        return request("GET", url, token, null);
    }

    public Result post(String url, String token, String jsonBody) {
        return request("POST", url, token, jsonBody);
    }

    public Result request(String method, String url, String token, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "token " + token);
            }
            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] body = jsonBody == null ? new byte[0] : jsonBody.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }
            }
            int code = conn.getResponseCode();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String body = is == null ? "" : read(is);
            return new Result(code, body);
        } catch (Exception e) {
            throw new OpsException("HTTP " + method + " " + url + " failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) { }
            }
        }
    }

    private String read(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    public static final class Result {
        public final int status;
        public final String body;
        public Result(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }
}