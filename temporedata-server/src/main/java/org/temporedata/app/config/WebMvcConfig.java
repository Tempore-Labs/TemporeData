package org.temporedata.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * SPA routing and CORS configuration.
 *
 * <p>Static sources（运行模式重设计 §4.2）：优先读外部目录（{@code temporedata.ui.static-dir}，
 * 默认指向前端 {@code temporedata-ui/dist}），使前端 build 后无需重打包/重启即可被
 * 按请求读取；回退 {@code classpath:/static/} 兼容发行 jar / 未配外部目录时。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 外部前端静态目录（file: 相对工作目录解析），留空则只用 classpath。 */
    @Value("${temporedata.ui.static-dir:}")
    private String uiStaticDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5174", "http://localhost:5175", "http://127.0.0.1:5500", "http://localhost:5500", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources from classpath + optional external ui dir, with SPA fallback.
        // Exclude /api/** paths so they reach controllers instead of falling back to index.html.
        List<String> locations = new ArrayList<>();
        if (uiStaticDir != null && !uiStaticDir.isBlank()) {
            String resolved = resolveStaticDir(uiStaticDir);
            locations.add(resolved.endsWith("/") ? resolved : resolved + "/");
        }
        locations.add("classpath:/static/");

        registry.addResourceHandler("/**")
                .addResourceLocations(locations.toArray(new String[0]))
                // SPA fallback 解析需 resourceChain；前端产物为内容哈希文件名，build 是新增路径即时可读
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Let API requests pass through to controllers
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        // Fallback to index.html for SPA routing (non-API paths only)
                        if (!resourcePath.contains(".")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        return null;
                    }
                });
    }

    /**
     * Resolve {@link #uiStaticDir} into an absolute file URI that Spring's resource
     * handler can use, robust to the process working directory.
     *
     * <p>Rationale: the default config value is a CWD-relative {@code file:temporedata-ui/dist/}.
     * This works when the app is launched from the repo root (e.g. {@code java -jar}), but
     * under {@code mvn spring-boot:run} the working directory is the module build dir, so the
     * relative base can't be found and the handler silently falls back to the stale
     * {@code classpath:/static/}. To fix that, a relative base is resolved from the current
     * working directory and, when the target isn't under the CWD, we walk up the ancestor
     * chain looking for a base containing the directory (i.e. locate the repo root that holds
     * {@code temporedata-ui/dist}). Absolute and non-file locations (http/classpath) are kept
     * unchanged.</p>
     */
    private String resolveStaticDir(String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return value;
        }
        // Keep non-file locations (and absolute file: URIs) as-is.
        if (value.startsWith("http://") || value.startsWith("https://")
                || value.startsWith("classpath:") || value.startsWith("s3:")) {
            return value;
        }
        // Strip an optional "file:" prefix so we work with a plain path.
        String path = value;
        if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        // file:/abs and file:///abs -> absolute unix path.
        while (path.startsWith("/") || path.startsWith("\\\\")) {
            if (path.startsWith("//")) {
                path = path.substring(1);
            } else {
                break;
            }
        }
        // Absolute (unix / or windows drive) -> already usable as a file URI.
        if (path.startsWith("/") || path.matches("^[a-zA-Z]:[\\\\/].*")) {
            return asDirUri(path);
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        // 1) Resolve relative to the CWD.
        Path fromCwd = cwd.resolve(path).normalize();
        if (Files.isDirectory(fromCwd)) {
            return fromCwd.toUri().toString();
        }
        // 2) Walk up from the CWD to find an ancestor that hosts the directory
        //    (locates the repo root even when launched from a module build dir).
        Path base = cwd;
        while (base != null) {
            Path candidate = base.resolve(path).normalize();
            if (Files.isDirectory(candidate)) {
                return candidate.toUri().toString();
            }
            base = base.getParent();
        }
        // 3) Fall back to CWD + path (keeps original behaviour; logging caller is on config test).
        return fromCwd.toUri().toString();
    }

    private String asDirUri(String absolutePath) {
        String p = absolutePath;
        if (!p.endsWith("/")) {
            p = p + "/";
        }
        return "file:" + p;
    }
}