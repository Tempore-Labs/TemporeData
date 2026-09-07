package org.temporedata.modules.gov.security.controller;

import lombok.RequiredArgsConstructor;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.gov.security.GovExecResult;
import org.temporedata.modules.gov.security.GovernedSqlExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Governed export (P2, 数据访问控制与动态脱敏执行链 v1.0 §7). A SELECT is executed through
 * the governance chain (列裁剪 + 动态脱敏) and the governed result is streamed out as CSV
 * with {@link StreamingResponseBody}, so a large-but-allowed result does not buffer a full
 * response in memory (row cap still enforced by {@link GovernedSqlExecutor}).
 */
@RestController
@RequestMapping("/api/query/export")
@RequiredArgsConstructor
public class GovernedExportController {

    private final GovernedSqlExecutor governedSqlExecutor;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> export(@RequestParam String sql,
                                                        @RequestParam String datasourceId) {
        GovExecResult res = governedSqlExecutor.execute(sql, datasourceId, "EXPORT");
        if (res.isDenied()) {
            throw new BusinessException("数据访问被拒绝");
        }
        List<String> cols = res.getColumns();
        StreamingResponseBody body = out -> writeCsv(out, cols, res.getRows());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"governed-export.csv\"")
                .body(body);
    }

    private void writeCsv(OutputStream out, List<String> cols, List<Map<String, Object>> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escaped(cols.get(i)));
        }
        sb.append('\n');
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));

        for (Map<String, Object> row : rows) {
            sb = new StringBuilder();
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) sb.append(',');
                Object v = row.get(cols.get(i));
                sb.append(escaped(v == null ? "" : String.valueOf(v)));
            }
            sb.append('\n');
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    private String escaped(String s) {
        boolean needQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0;
        if (!needQuote) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}