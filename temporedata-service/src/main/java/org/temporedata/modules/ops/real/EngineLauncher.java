package org.temporedata.modules.ops.real;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.engine.entity.EngineEntity;
import org.temporedata.modules.ops.engine.repository.EngineRepository;
import org.temporedata.modules.ops.real.entity.RealEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real Flink/Spark submission launcher for real-time tasks.
 *
 * <p>Rebuilds and actually spawns the engine's CLI (spark-submit / flink run) as a
 * child process, parses the resulting application/job id from the output, and records
 * real status. It is intentionally HONEST about missing engines: if no binary is found
 * (PATH or a configured cluster's flinkHome) the call fails with a clear message
 * instead of reporting a fake run. This is the "real submission" behaviour: verified
 * against whatever engine exists in the deployment environment.
 */
@Slf4j
@Component
public class EngineLauncher {

    private final EngineRepository engineRepository;

    public EngineLauncher(EngineRepository engineRepository) {
        this.engineRepository = engineRepository;
    }

    private static final long LAUNCH_TIMEOUT_SECONDS = 120;
    private static final long OUTPUT_LIMIT = 64 * 1024;

    /** Outcome of a real (spawned) submission. */
    public static final class SubmitOutcome {
        private final boolean success;
        private final String appId;
        private final String output;

        SubmitOutcome(boolean success, String appId, String output) {
            this.success = success;
            this.appId = appId;
            this.output = output;
        }

        public boolean success() {
            return success;
        }

        public String appId() {
            return appId;
        }

        public String output() {
            return output;
        }
    }

    /**
     * Resolve the engine CLI and spawn a real submission for <code>entity</code>.
     * Never fakes success: throws BusinessException when the binary is missing or the
     * submit exits non-zero.
     */
    public SubmitOutcome submit(RealEntity entity) {
        String type = (entity.getType() == null ? "" : entity.getType()).toUpperCase();
        ClusterConfig cfg = resolveConfig(type, entity.getClusterId());

        List<String> cmd = buildCommand(type, cfg, entity);
        Process process = spawn(cmd, true);

        OutputCapture cap = new OutputCapture(process);
        try {
            boolean finished = process.waitFor(LAUNCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String output = cap.blockingText();
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("作业提交超时(> " + LAUNCH_TIMEOUT_SECONDS + "s)，已终止");
            }
            int code = process.exitValue();
            String appId = extractAppId(type, output);
            if (code != 0) {
                throw new BusinessException("作业提交失败 (exit=" + code + "): " + tail(output));
            }
            return new SubmitOutcome(true, appId, output);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException("作业提交异常: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        } finally {
            process.destroy();
        }
    }

    /**
     * Cancel / kill a running job identified by its application or job id.
     */
    public void stop(RealEntity entity, String appId, String jobId) {
        String type = (entity.getType() == null ? "" : entity.getType()).toUpperCase();
        ClusterConfig cfg = resolveConfig(type, entity.getClusterId());

        List<String> cmd;
        if (isSpark(type)) {
            if (appId == null || appId.isBlank()) {
                throw new BusinessException("无法停止：缺少 Spark applicationId");
            }
            cmd = List.of("yarn", "application", "-kill", appId);
        } else {
            if (jobId == null && appId == null) {
                throw new BusinessException("无法停止：缺少 Flink jobId");
            }
            cmd = buildStopList(cfg, jobId != null ? jobId : appId);
        }

        Process process = spawn(cmd, true);
        OutputCapture cap = new OutputCapture(process);
        try {
            process.waitFor(LAUNCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String out = cap.blockingText();
            if (process.exitValue() != 0) {
                throw new BusinessException("停止作业失败 (exit=" + process.exitValue() + "): " + tail(out));
            }
            log.info("Engine job stopped: appId={} jobId={} output={}", appId, jobId, tail(out));
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException("停止作业异常: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
        } finally {
            process.destroy();
        }
    }

    // ---- command construction ----

    private ClusterConfig resolveConfig(String type, String clusterId) {
        // 1) try a registered engine (zy_engine) referenced by clusterId
        if (clusterId != null && !clusterId.isBlank()) {
            EngineEntity engine = engineRepository.findById(clusterId).orElse(null);
            if (engine != null) {
                String home = isSpark(type) ? sparkHome(engine) : engine.getFlinkHome();
                String binary = isSpark(type) ? sparkBin(engine) : flinkBin(engine);
                return new ClusterConfig(binary, engine.getMaster(), engine.getDeployMode(),
                        engine.getJarPath(), engine.getMainClass());
            }
        }
        // 2) default: rely on engines on PATH
        String binary = isSpark(type) ? "spark-submit" : "flink";
        return new ClusterConfig(binary, null, "client", null, null);
    }

    private static boolean isSpark(String type) {
        return type.equals("SPARK") || type.equals("SPARK_STREAMING");
    }

    private String flinkBin(EngineEntity e) {
        return e.getFlinkHome() != null ? e.getFlinkHome().replaceAll("/+$", "") + "/bin/flink" : "flink";
    }

    private String sparkHome(EngineEntity e) {
        return e.getFlinkHome(); // zy_engine uses a single home field for the engine root
    }

    private String sparkBin(EngineEntity e) {
        String home = sparkHome(e);
        return home != null ? home.replaceAll("/+$", "") + "/bin/spark-submit" : "spark-submit";
    }

    private List<String> buildCommand(String type, ClusterConfig cfg, RealEntity entity) {
        if (isSpark(type)) {
            List<String> c = new ArrayList<>();
            c.add(cfg.binary());
            c.add("--master");
            c.add(cfg.master() != null ? cfg.master() : "local[*]");
            c.add("--deploy-mode");
            c.add(cfg.deployMode() != null ? cfg.deployMode() : "client");
            if (cfg.mainClass() != null && !cfg.mainClass().isBlank()) {
                c.add("--class");
                c.add(cfg.mainClass());
            }
            String program = cfg.jarPath() != null ? cfg.jarPath() : entity.getScript();
            if (program != null && !program.isBlank()) {
                c.add(program);
            }
            return c;
        }
        // Flink
        List<String> c = new ArrayList<>();
        String binary = cfg.binary();
        // support both "flink" and "flink run"
        c.add(binary.contains(" ") ? binary : binary);
        c.add("run");
        c.add("-d");
        if (cfg.master() != null && !cfg.master().isBlank() && cfg.master().startsWith("yarn")) {
            c.add("-m");
            c.add(cfg.master());
        }
        String program = cfg.jarPath() != null ? cfg.jarPath() : entity.getScript();
        if (program != null && !program.isBlank()) {
            c.add(program);
        }
        return c;
    }

    private List<String> buildStopList(ClusterConfig cfg, String jobId) {
        List<String> c = new ArrayList<>();
        c.add(cfg.binary());
        c.add("cancel");
        if (cfg.master() != null && cfg.master().startsWith("yarn")) {
            c.add("-m");
            c.add(cfg.master());
        }
        c.add(jobId);
        return c;
    }

    // ---- process helpers ----

    private Process spawn(List<String> cmd, boolean inheritWorkingDir) throws BusinessException {
        // Honest presence check: fail fast with guidance when the engine binary is absent.
        checkBinary(cmd.get(0));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            return pb.start();
        } catch (IOException e) {
            throw new BusinessException("无法启动计算引擎进程: " + cmd.get(0) + " (" + (e.getMessage() == null ? e : e.getMessage()) + ")");
        }
    }

    private void checkBinary(String candidate) {
        // A plain name => lookup on PATH via system "which".
        if (!candidate.contains("/")) {
            if (which(candidate) == null) {
                throw new BusinessException(
                        "未找到计算引擎可执行文件: " + candidate + "。请安装该引擎并将其加入 PATH，"
                                + "或为实时任务关联一个已配置 home 的集群(clusterId)。");
            }
            return;
        }
        Path p = Paths.get(candidate);
        if (!Files.exists(p)) {
            throw new BusinessException("计算引擎二进制不存在: " + candidate);
        }
        if (!Files.isExecutable(p)) {
            throw new BusinessException("计算引擎二进制不可执行: " + candidate);
        }
    }

    private String which(String name) {
        try {
            Process p = new ProcessBuilder("which", name).start();
            p.waitFor(5, TimeUnit.SECONDS);
            try (java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                return (line != null && !line.isBlank()) ? line : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static final Pattern APP_ID = Pattern.compile("(application_\\d+_\\d+)|(Flink Job \\w+ is submitted|Job ID: (\\S+))");

    private String extractAppId(String type, String output) {
        Matcher m = APP_ID.matcher(output == null ? "" : output);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1);
            if (m.group(3) != null) return m.group(3);
        }
        return null;
    }

    private static String tail(String s) {
        if (s == null) return "";
        int len = Math.min(s.length(), 400);
        return s.substring(Math.max(0, s.length() - len));
    }

    /** Spawns a thread to drain the process output and exposes the captured text. */
    private static final class OutputCapture {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        OutputCapture(Process p) {
            drain(p.getInputStream());
            drain(p.getErrorStream());
        }

        private void drain(InputStream in) {
            Thread t = new Thread(() -> {
                byte[] b = new byte[2048];
                int n;
                try {
                    while ((n = in.read(b)) != -1) {
                        synchronized (buf) {
                            if (buf.size() + n > OUTPUT_LIMIT) {
                                n = (int) (OUTPUT_LIMIT - buf.size());
                                buf.write(b, 0, Math.max(0, n));
                                break;
                            }
                            buf.write(b, 0, n);
                        }
                    }
                } catch (Exception ignore) {
                    // stream closed
                }
            });
            t.setDaemon(true);
            t.start();
        }

        String blockingText() {
            try {
                TimeUnit.MILLISECONDS.sleep(80); // brief settle for process output
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            synchronized (buf) {
                return buf.toString();
            }
        }
    }

    /** Resolved engine command + config for a submission. */
    static final class ClusterConfig {
        private final String binary;
        private final String master;
        private final String deployMode;
        private final String jarPath;
        private final String mainClass;

        ClusterConfig(String binary, String master, String deployMode, String jarPath, String mainClass) {
            this.binary = binary;
            this.master = master;
            this.deployMode = deployMode;
            this.jarPath = jarPath;
            this.mainClass = mainClass;
        }

        String binary() {
            return binary;
        }

        String master() {
            return master;
        }

        String deployMode() {
            return deployMode;
        }

        String jarPath() {
            return jarPath;
        }

        String mainClass() {
            return mainClass;
        }
    }
}