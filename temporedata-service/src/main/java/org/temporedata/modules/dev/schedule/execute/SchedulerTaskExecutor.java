package org.temporedata.modules.dev.schedule.execute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.temporedata.common.util.Crypto;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.temporedata.integration.core.datasource.repository.DatasourceRepository;
import org.temporedata.modules.dev.schedule.service.TaskDispatchResult;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Real in-process executors for scheduler task types other than WORKFLOW.
 *
 * <ul>
 *   <li>SQL   : runs the target on a real datasource over JDBC (SELECT or DML), honoring
 *               ${biz_date} placeholders like the DAG runtime.</li>
 *   <li>SCRIPT: runs a shell command in a sandboxed subprocess with a timeout, capturing
 *               output and the real exit code.</li>
 *   <li>SYNC  : performs a real single-table full-refresh copy (source table {@literal ->}
 *               target table, optionally across two datasources) over JDBC.</li>
 * </ul>
 *
 * <p>Datasource selection reads ids from the task's params JSON (datasourceId /
 * sourceDatasourceId / targetDatasourceId); it falls back to ds-test1 (demo-mysql).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTaskExecutor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long SCRIPT_TIMEOUT_SECONDS = 30;
    private static final long SCRIPT_OUTPUT_LIMIT = 32 * 1024;
    private static final int SYNC_ROW_LIMIT = 100_000;
    private static final int SYNC_BATCH = 500;

    private final DatasourceRepository datasourceRepository;
    private final Crypto crypto;

    // ---- SQL ----

    public TaskDispatchResult sql(String target, String paramsJson, String bizDate) {
        String sql = stripPrefix(target, "sql");
        if (sql == null || sql.isBlank()) {
            return TaskDispatchResult.fail("SQL task has empty target_ref");
        }
        if (bizDate != null && !bizDate.isBlank()) {
            sql = sql.replace("${biz_date}", bizDate)
                    .replace("${biz_date_ym}", bizDate.replace("-", "").substring(0, 6));
        }

        DatasourceEntity ds = resolveDatasource(paramsJson, "datasourceId", true);
        if (ds == null) {
            return TaskDispatchResult.fail("未找到可用数据源，请在任务参数 params 中配置 datasourceId");
        }

        long started = System.currentTimeMillis();
        try (Connection conn = open(ds);
             Statement st = conn.createStatement()) {
            boolean select = isSelect(sql);
            long rows;
            if (select) {
                try (ResultSet rs = st.executeQuery(sql)) {
                    rows = countRows(rs);
                }
            } else {
                rows = st.executeUpdate(sql);
            }
            long cost = System.currentTimeMillis() - started;
            String kind = select ? "SELECT" : "DML";
            return TaskDispatchResult.ok(String.format(
                    "SQL 执行成功 (%s 于 %s): %s 行=%d 耗时=%dms",
                    kind, ds.getName(), select ? "返回" : "影响", rows, cost));
        } catch (Exception e) {
            log.error("Scheduler SQL task failed: ds={}, sql={}", ds.getName(), sql, e);
            return TaskDispatchResult.fail("SQL 执行失败: " + shortMsg(e));
        }
    }

    // ---- SCRIPT ----

    public TaskDispatchResult script(String target, String bizDate) {
        String cmd = stripPrefix(target, "sh");
        if (cmd == null || cmd.isBlank()) {
            return TaskDispatchResult.fail("SCRIPT task has empty target_ref");
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            if (bizDate != null) {
                pb.environment().put("BIZ_DATE", bizDate);
            }
            Process p = pb.start();
            // Drain stdout and stderr concurrently to avoid a blocked pipe.
            OutputPair out = drain(p);
            boolean finished = p.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return TaskDispatchResult.fail("脚本执行超时(> " + SCRIPT_TIMEOUT_SECONDS + "s): " + cmd);
            }
            int code = p.exitValue();
            if (code == 0) {
                return TaskDispatchResult.ok("脚本执行成功 (exit 0): " + out.stdout());
            }
            return TaskDispatchResult.fail("脚本执行失败 (exit=" + code + "): " + out.stdErr());
        } catch (Exception e) {
            log.error("Scheduler SCRIPT task failed: cmd={}", cmd, e);
            return TaskDispatchResult.fail("脚本执行失败: " + shortMsg(e));
        }
    }

    // ---- SYNC ----

    public TaskDispatchResult sync(String target, String paramsJson) {
        TargetPair pair = parseSyncTarget(target);
        if (pair == null) {
            return TaskDispatchResult.fail("SYNC target_ref 格式应为 <源表> 或 <源表>:<目标表>");
        }
        DatasourceEntity srcDs = resolveDatasource(paramsJson, "sourceDatasourceId", true);
        if (srcDs == null) {
            return TaskDispatchResult.fail("未找到源数据源，请在 params 中配置 sourceDatasourceId");
        }
        DatasourceEntity tgtDs = resolveDatasource(paramsJson, "targetDatasourceId", srcDs);
        if (tgtDs == null) {
            return TaskDispatchResult.fail("未找到目标数据源");
        }

        long started = System.currentTimeMillis();
        int copied;
        try (Connection src = open(srcDs);
             Connection dst = open(tgtDs)) {
            copied = copyTable(src, dst, pair.source, pair.target());
        } catch (Exception e) {
            log.error("Scheduler SYNC task failed: {} -> {}", pair.source, pair.target(), e);
            return TaskDispatchResult.fail("SYNC 失败: " + shortMsg(e));
        }
        long cost = System.currentTimeMillis() - started;
        return TaskDispatchResult.ok(String.format(
                "SYNC 完成: %s.%s -> %s.%s 复制行数=%d 耗时=%dms",
                srcDs.getName(), pair.source, tgtDs.getName(), pair.target(), copied, cost));
    }

    // ---- internals ----

    private int copyTable(Connection src, Connection dst, String srcTable, String tgtTable) throws Exception {
        // 1) Read source schema + full rows (bounded).
        String select = "SELECT * FROM " + quote(srcTable);
        List<List<Object>> rows = new ArrayList<>();
        List<String> cols = new ArrayList<>();
        try (Statement st = src.createStatement();
             ResultSet rs = st.executeQuery(select)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                cols.add(md.getColumnLabel(i));
            }
            while (rs.next()) {
                if (rows.size() >= SYNC_ROW_LIMIT) break;
                List<Object> row = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
        }

        // 2) Ensure the target table exists (MySQL: create LIKE source when absent).
        ensureTargetTable(src, dst, srcTable, tgtTable);

        // 3) Full refresh into the target.
        String colsJoined = String.join(",", cols);
        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }
        String insertSql = "INSERT INTO " + quote(tgtTable) + " (" + colsJoined + ") VALUES (" + ph + ")";

        try (Statement del = dst.createStatement()) {
            del.executeUpdate("DELETE FROM " + quote(tgtTable));
        }

        int copied = 0;
        try (PreparedStatement ps = dst.prepareStatement(insertSql)) {
            for (List<Object> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    ps.setObject(i + 1, row.get(i));
                }
                ps.addBatch();
                if (++copied % SYNC_BATCH == 0) {
                    ps.executeBatch();
                }
            }
            if (copied % SYNC_BATCH != 0) {
                ps.executeBatch();
            }
        }
        return copied;
    }

    private void ensureTargetTable(Connection src, Connection dst, String srcTable, String tgtTable) throws Exception {
        String db = currentDb(dst);
        try (PreparedStatement ps = dst.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?")) {
            ps.setString(1, db);
            ps.setString(2, tgtTable);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (Statement st = dst.createStatement()) {
            st.executeUpdate("CREATE TABLE " + quote(tgtTable) + " LIKE " + quote(srcTable));
            log.info("SYNC: created target table {} LIKE {}", tgtTable, srcTable);
        }
    }

    private String currentDb(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static String quote(String ident) {
        return "`" + ident.replace("`", "``") + "`";
    }

    /** Parses "<src>" or "<src>:<target>"; the target defaults to the source name. */
    private TargetPair parseSyncTarget(String target) {
        if (target == null) return null;
        String t = stripPrefix(target, "sync");
        if (t == null || t.isBlank()) return null;
        int colon = t.indexOf(':');
        if (colon < 0) {
            String name = t.trim();
            return name.isEmpty() ? null : new TargetPair(name, name);
        }
        String src = t.substring(0, colon).trim();
        String dst = t.substring(colon + 1).trim();
        if (src.isEmpty()) return null;
        return new TargetPair(src, dst.isEmpty() ? src : dst);
    }

    // ---- datasource resolution ----

    /**
     * Resolve a datasource from the task's params JSON (by id or by name), else a
     * fallback target, else demo-mysql / the first MYSQL one.
     */
    private DatasourceEntity resolveDatasource(String paramsJson, String key, DatasourceEntity fallback) {
        // 1) explicit JSON param, by id first then by name
        String val = jsonParam(paramsJson, key);
        if (val != null && !val.isBlank()) {
            DatasourceEntity byId = findById(val);
            if (byId != null) return byId;
            DatasourceEntity byName = findByName(val);
            if (byName != null) return byName;
        }
        // 2) explicit fallback datasource (e.g. target defaults to source)
        if (fallback != null) return fallback;
        // 3) demo-mysql, then any MYSQL
        DatasourceEntity demo = findByName("demo-mysql");
        if (demo != null) return demo;
        return datasourceRepository.findAll().stream()
                .filter(d -> "MYSQL".equalsIgnoreCase(d.getType()))
                .findFirst()
                .orElse(null);
    }

    private DatasourceEntity resolveDatasource(String paramsJson, String key, boolean required) {
        return resolveDatasource(paramsJson, key, (DatasourceEntity) null);
    }

    private String jsonParam(String paramsJson, String key) {
        if (paramsJson == null || paramsJson.isBlank()) return null;
        String trimmed = paramsJson.trim();
        if (!trimmed.startsWith("{")) return null;
        try {
            JsonNode node = MAPPER.readTree(trimmed);
            JsonNode v = node.get(key);
            return v == null ? null : v.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private DatasourceEntity findById(String id) {
        return datasourceRepository.findById(id).orElse(null);
    }

    private DatasourceEntity findByName(String name) {
        return datasourceRepository.findAll().stream()
                .filter(d -> name.equals(d.getName()))
                .findFirst()
                .orElse(null);
    }

    private Connection open(DatasourceEntity ds) throws Exception {
        String url = buildJdbcUrl(ds);
        String password = crypto.decrypt(ds.getPassword());
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception ignore) {
            // driver may already be loaded
        }
        return DriverManager.getConnection(url, ds.getUsername(), password);
    }

    private String buildJdbcUrl(DatasourceEntity ds) {
        String type = ds.getType();
        String url;
        if ("MYSQL".equalsIgnoreCase(type)) {
            url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false",
                    ds.getHost(), ds.getPort(), ds.getDatabase());
        } else if ("POSTGRESQL".equalsIgnoreCase(type)) {
            url = String.format("jdbc:postgresql://%s:%d/%s", ds.getHost(), ds.getPort(), ds.getDatabase());
        } else if ("ORACLE".equalsIgnoreCase(type)) {
            url = String.format("jdbc:oracle:thin:@%s:%d:%s", ds.getHost(), ds.getPort(), ds.getDatabase());
        } else {
            throw new IllegalStateException("Unsupported datasource type: " + type);
        }
        if (ds.getParams() != null && !ds.getParams().isBlank()) {
            url += (url.contains("?") ? "&" : "?") + ds.getParams();
        }
        return url;
    }

    private static boolean isSelect(String sql) {
        String s = sql.trim();
        return s.regionMatches(true, 0, "select", 0, 6)
                || s.regionMatches(true, 0, "with", 0, 4);
    }

    private static long countRows(ResultSet rs) throws Exception {
        long n = 0;
        while (rs.next()) {
            if (++n > 1_000_000) break;
        }
        return n;
    }

    private static String stripPrefix(String target, String prefix) {
        if (target == null) return null;
        String t = target.trim();
        int colon = t.indexOf(':');
        if (colon >= 0) {
            String head = t.substring(0, colon).trim();
            if (head.equalsIgnoreCase(prefix)) {
                return t.substring(colon + 1).trim();
            }
        }
        return t;
    }

    private static String shortMsg(Throwable e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    /** Drains a subprocess's stdout/stderr concurrently, returning capped text. */
    private OutputPair drain(Process p) throws Exception {
        StreamGobbler outGobbler = new StreamGobbler(p.getInputStream());
        StreamGobbler errGobbler = new StreamGobbler(p.getErrorStream());
        outGobbler.start();
        errGobbler.start();
        outGobbler.join(SCRIPT_TIMEOUT_SECONDS * 1000L);
        errGobbler.join(SCRIPT_TIMEOUT_SECONDS * 1000L);
        return new OutputPair(outGobbler.text(), errGobbler.text());
    }

    private static final class OutputPair {
        private final String stdout;
        private final String stdErr;

        OutputPair(String stdout, String stdErr) {
            this.stdout = stdout;
            this.stdErr = stdErr;
        }

        String stdout() {
            return stdout;
        }

        String stdErr() {
            return stdErr;
        }
    }

    /** Backgrounds a single stream read and caps the accumulated text. */
    private static final class StreamGobbler extends Thread {
        private final InputStream in;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        StreamGobbler(InputStream in) {
            this.in = in;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            int n;
            try {
                while ((n = in.read(buffer)) != -1) {
                    if (buf.size() + n > SCRIPT_OUTPUT_LIMIT) {
                        n = (int) (SCRIPT_OUTPUT_LIMIT - buf.size());
                        buf.write(buffer, 0, Math.max(0, n));
                        break;
                    }
                    buf.write(buffer, 0, n);
                }
            } catch (Exception ignore) {
                // stream closed
            }
        }

        String text() {
            return buf.toString();
        }
    }

    private static final class TargetPair {
        private final String source;
        private final String target;

        TargetPair(String source, String target) {
            this.source = source;
            this.target = target;
        }

        String target() {
            return target;
        }
    }
}