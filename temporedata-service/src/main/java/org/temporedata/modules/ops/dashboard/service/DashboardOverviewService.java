package org.temporedata.modules.ops.dashboard.service;

import org.temporedata.api.ops.dashboard.DashboardRes;
import org.temporedata.modules.dev.workflow.repository.WorkflowRepository;
import org.temporedata.modules.gov.meta.entity.MetaTableEntity;
import org.temporedata.modules.gov.meta.repository.MetaTableRepository;
import org.temporedata.modules.gov.quality.entity.QualityEntity;
import org.temporedata.modules.gov.quality.repository.QualityRepository;
import org.temporedata.integration.core.datasource.entity.DatasourceEntity;
import org.temporedata.integration.core.datasource.repository.DatasourceRepository;
import org.temporedata.modules.ops.sync.entity.SyncEntity;
import org.temporedata.modules.ops.sync.repository.SyncRepository;
import org.temporedata.modules.svc.service.entity.DataApiEntity;
import org.temporedata.modules.svc.service.repository.DataApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the aggregated overview consumed by GET /api/dashboard.
 *
 * Cross-module aggregation of counts, distributions and recent/failed records
 * for the monitor overview page. Kept read-only and tolerant of null fields so a
 * sparse database still renders a healthy dashboard instead of raising 500.
 */
@Service
@RequiredArgsConstructor
public class DashboardOverviewService {

    private static final DateTimeFormatter HFMT = DateTimeFormatter.ofPattern("HH:00");
    private static final List<DateTimeFormatter> TS_FMTS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    private final DatasourceRepository datasourceRepository;
    private final MetaTableRepository metaTableRepository;
    private final SyncRepository syncRepository;
    private final QualityRepository qualityRepository;
    private final WorkflowRepository workflowRepository;
    private final DataApiRepository dataApiRepository;

    public DashboardRes overview() {
        List<MetaTableEntity> metas = metaTableRepository.findAll();
        List<SyncEntity> syncs = syncRepository.findAll();
        List<QualityEntity> qualities = qualityRepository.findAll();
        List<DatasourceEntity> datasources = datasourceRepository.findAll();
        List<DataApiEntity> apis = dataApiRepository.findAll();

        DashboardRes res = new DashboardRes();

        // ---- Stat cards ----
        res.setDatasourceCount(datasources.size());
        res.setTableCount(distinctTableCount(metas));
        res.setTotalRows(sumRows(metas));
        res.setSyncTaskCount(syncs.size());
        res.setQualityRuleCount(qualities.size());
        res.setWorkflowCount((int) workflowRepository.count());
        res.setApiCount(apis.size());
        res.setApiCallTotal(0L);

        // ---- Sync/quality status counts ----
        long syncSuccess = statusCount(syncs, SyncEntity::getStatus, "SUCCESS");
        long syncFail = statusCount(syncs, SyncEntity::getStatus, "FAILED");
        res.setSyncSuccessCount((int) syncSuccess);
        res.setSyncFailCount((int) syncFail);
        res.setQualityPassCount((int) qualityCount(qualities, "PASS"));
        res.setQualityFailCount((int) qualityCount(qualities, "FAIL"));

        // ---- Distributions ----
        res.setSyncStatus(groupCount(syncs, SyncEntity::getStatus));
        res.setQualityStatus(qualityStatusDist(qualities));
        res.setDatasourceTypeDist(groupCount(datasources, DatasourceEntity::getType));
        res.setDatasources(datasources.stream()
                .map(d -> {
                    DashboardRes.DataSourceStat s = new DashboardRes.DataSourceStat();
                    s.setName(d.getName());
                    s.setType(d.getType());
                    return s;
                })
                .collect(Collectors.toList()));

        // ---- Charts ----
        res.setTopTables(topTables(metas));
        res.setHourlyTrend(hourlyTrend(syncs));

        // ---- Tables ----
        res.setRecentSyncs(recentSyncs(syncs));
        res.setFailedSyncs(failedSyncs(syncs));
        res.setFailedRules(failedRules(qualities));

        return res;
    }

    // ---- helpers ----

    private int distinctTableCount(List<MetaTableEntity> metas) {
        return (int) metas.stream()
                .map(MetaTableEntity::getTableName)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long sumRows(List<MetaTableEntity> metas) {
        // Real metadata collection only captures structure (no data sampling), so row
        // counts are null -> this sums 0. Kept for the card that reports total rows.
        return metas.stream()
                .map(MetaTableEntity::getRowCount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    private long statusCount(List<SyncEntity> list, Function<SyncEntity, String> getter, String wanted) {
        return list.stream().filter(s -> wanted.equalsIgnoreCase(getter.apply(s))).count();
    }

    private long qualityCount(List<QualityEntity> list, String wanted) {
        return list.stream()
                .filter(q -> wanted.equalsIgnoreCase(q.getStatus()) || wanted.equalsIgnoreCase(q.getLastStatus()))
                .count();
    }

    private <T> List<DashboardRes.StatItem> groupCount(List<T> list, Function<T, String> getter) {
        Map<String, Long> counts = new HashMap<>();
        for (T o : list) {
            String k = getter.apply(o);
            if (k != null) counts.merge(k, 1L, Long::sum);
        }
        List<DashboardRes.StatItem> items = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    DashboardRes.StatItem it = new DashboardRes.StatItem();
                    it.setName(e.getKey());
                    it.setValue(e.getValue().intValue());
                    items.add(it);
                });
        return items;
    }

    private List<DashboardRes.StatItem> qualityStatusDist(List<QualityEntity> list) {
        List<DashboardRes.StatItem> items = new ArrayList<>();
        items.add(stat("PASS", (int) qualityCount(list, "PASS")));
        items.add(stat("FAIL", (int) qualityCount(list, "FAIL")));
        return items;
    }

    private DashboardRes.StatItem stat(String name, int value) {
        DashboardRes.StatItem it = new DashboardRes.StatItem();
        it.setName(name);
        it.setValue(value);
        return it;
    }

    private List<DashboardRes.TableStat> topTables(List<MetaTableEntity> metas) {
        Map<String, Long> rows = new HashMap<>();
        metas.stream()
                .filter(m -> m.getTableName() != null && m.getRowCount() != null)
                .forEach(m -> rows.merge(m.getTableName(), m.getRowCount(), Long::sum));
        return rows.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    DashboardRes.TableStat t = new DashboardRes.TableStat();
                    t.setTableName(e.getKey());
                    t.setRowCount(e.getValue());
                    return t;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardRes.TrendItem> hourlyTrend(List<SyncEntity> syncs) {
        Map<String, Integer[]> buckets = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            buckets.put(formatHour(now.minusHours(i)), new Integer[]{0, 0});
        }
        for (SyncEntity s : syncs) {
            LocalDateTime t = parseTime(s.getLastRunTime());
            if (t == null) continue;
            String hour = formatHour(t);
            Integer[] cell = buckets.get(hour);
            if (cell == null) continue;
            if (isSuccess(s.getStatus())) cell[0]++;
            else if (isFailed(s.getStatus())) cell[1]++;
        }
        List<DashboardRes.TrendItem> items = new ArrayList<>();
        buckets.forEach((hour, cell) -> {
            DashboardRes.TrendItem it = new DashboardRes.TrendItem();
            it.setHour(hour);
            it.setSuccess(cell[0]);
            it.setFailed(cell[1]);
            items.add(it);
        });
        return items;
    }

    private List<DashboardRes.SyncStat> recentSyncs(List<SyncEntity> syncs) {
        return syncs.stream()
                .sorted(Comparator.comparing(SyncEntity::getLastRunTime,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .map(s -> {
                    DashboardRes.SyncStat st = new DashboardRes.SyncStat();
                    st.setTaskName(s.getName());
                    st.setStatus(s.getStatus());
                    st.setLastRunTime(s.getLastRunTime());
                    st.setRowCount(s.getRowCount());
                    return st;
                })
                .collect(Collectors.toList());
    }

    private List<DashboardRes.FailedTask> failedSyncs(List<SyncEntity> syncs) {
        List<DashboardRes.FailedTask> out = new ArrayList<>();
        syncs.stream().filter(s -> isFailed(s.getStatus())).limit(20).forEach(s -> {
            DashboardRes.FailedTask f = new DashboardRes.FailedTask();
            f.setTaskName(s.getName());
            f.setTargetTable(s.getTargetTable());
            f.setLastRunTime(s.getLastRunTime());
            f.setErrorMsg(s.getErrorMsg());
            out.add(f);
        });
        return out;
    }

    private List<DashboardRes.FailRule> failedRules(List<QualityEntity> qualities) {
        List<DashboardRes.FailRule> out = new ArrayList<>();
        qualities.stream()
                .filter(q -> "FAIL".equalsIgnoreCase(q.getStatus()) || "FAIL".equalsIgnoreCase(q.getLastStatus()))
                .limit(20)
                .forEach(q -> {
                    DashboardRes.FailRule r = new DashboardRes.FailRule();
                    r.setRuleName(q.getRuleName());
                    r.setTableName(q.getTableName());
                    r.setColumnName(q.getColumnName());
                    r.setRuleType(q.getRuleType());
                    r.setDescription(q.getDescription());
                    out.add(r);
                });
        return out;
    }

    private LocalDateTime parseTime(String s) {
        if (s == null) return null;
        for (DateTimeFormatter f : TS_FMTS) {
            try {
                return LocalDateTime.parse(s.trim(), f);
            } catch (Exception ignored) {
                // try next format
            }
        }
        return null;
    }

    private String formatHour(LocalDateTime t) {
        return t.withMinute(0).withSecond(0).withNano(0).format(HFMT);
    }

    private boolean isSuccess(String s) {
        return s != null && "SUCCESS".equalsIgnoreCase(s);
    }

    private boolean isFailed(String s) {
        return s != null && "FAILED".equalsIgnoreCase(s);
    }
}