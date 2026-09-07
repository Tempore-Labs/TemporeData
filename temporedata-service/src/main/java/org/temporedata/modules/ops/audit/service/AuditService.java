package org.temporedata.modules.ops.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.audit.entity.AuditArchiveEntity;
import org.temporedata.modules.ops.audit.entity.AuditEventEntity;
import org.temporedata.modules.ops.audit.entity.AuditPolicyEntity;
import org.temporedata.modules.ops.audit.repository.AuditArchiveRepository;
import org.temporedata.modules.ops.audit.repository.AuditEventRepository;
import org.temporedata.modules.ops.audit.repository.AuditPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * P3-12 unified financial-grade audit: tamper-evident hash chain, policy gating,
 * integrity verification, and compliance export.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String GENESIS = "GENESIS";

    private final AuditEventRepository eventRepository;
    private final AuditPolicyRepository policyRepository;
    private final AuditArchiveRepository archiveRepository;

    // ---- Write ----

    /**
     * Convenience write that maps a high-level eventType (LOGIN/BEHAVIOR/CHANGE)
     * onto a unified module tag so the merged log pages can group them.
     */
    @Transactional
    public AuditEventEntity submit(String eventType, String action, String resourceType, String resourceKey,
                                   String operator, String ip, String status, String detailJson) {
        String module = mapModule(eventType);
        return record(module, action, resourceType, resourceKey, operator, ip, null, status, detailJson);
    }

    private String mapModule(String eventType) {
        if (eventType == null) return "AUDIT";
        switch (eventType.toUpperCase()) {
            case "LOGIN": return "LOGIN";
            case "BEHAVIOR": return "BEHAVIOR";
            case "CHANGE": return "CHANGE";
            default: return "AUDIT";
        }
    }

    @Transactional
    public AuditEventEntity record(String module, String action, String resourceType, String resourceKey,
                                   String operator, String ip, String reqId, String status, String detailJson) {
        String level = policyLevel(module, action);
        if ("NONE".equalsIgnoreCase(level)) return null;
        if ("SUMMARY".equalsIgnoreCase(level) && detailJson != null && detailJson.length() > 512) {
            detailJson = detailJson.substring(0, 512) + "...";
        }

        AuditEventEntity last = eventRepository.findTopByOrderBySeqDesc().orElse(null);
        long seq = last == null ? 1 : last.getSeq() + 1;
        String prevHash = last == null ? GENESIS : last.getEventHash();

        AuditEventEntity e = AuditEventEntity.builder()
                .module(module).action(action)
                .resourceType(resourceType).resourceKey(resourceKey)
                .operator(operator).ip(ip).reqId(reqId).status(status)
                .tenantId(org.temporedata.security.context.TenantContext.getTenantId())
                .eventTime(now()).detailJson(detailJson).seq(seq).prevHash(prevHash)
                .build();
        e.setEventHash(hash(e));
        return eventRepository.save(e);
    }

    // ---- Query ----

    /**
     * Unified paged audit query used by the merged frontend log pages.
     * <code>eventType</code> classifies events: AUDIT (any non-login/behavior/change),
     * LOGIN, BEHAVIOR, CHANGE. Filters module, action and operator in memory, then
     * maps to a page ordered by seq ascending.
     */
    @Transactional(readOnly = true)
    public Page<AuditEventEntity> page(String eventType, String operator, String action, String resourceType, Pageable pageable) {
        List<AuditEventEntity> filtered = orderedAsc().stream()
                .filter(e -> matchesEventType(e, eventType))
                .filter(e -> operator == null || operator.isBlank() || operator.equals(e.getOperator()))
                .filter(e -> action == null || action.isBlank() || action.equals(e.getAction()))
                .filter(e -> resourceType == null || resourceType.isBlank() || resourceType.equals(e.getResourceType()))
                .collect(Collectors.toList());
        int total = filtered.size();
        int from = (int) Math.min(pageable.getOffset(), total);
        int to = (int) Math.min(from + pageable.getPageSize(), total);
        return new PageImpl<>(from >= to ? Collections.emptyList() : filtered.subList(from, to), pageable, total);
    }

    private boolean matchesEventType(AuditEventEntity e, String eventType) {
        if (eventType == null || eventType.isBlank()) return true;
        String m = e.getModule() == null ? "" : e.getModule().toUpperCase();
        switch (eventType.toUpperCase()) {
            case "LOGIN":
                return "LOGIN".equals(m);
            case "BEHAVIOR":
                return "BEHAVIOR".equals(m);
            case "CHANGE":
                return "CHANGE".equals(m);
            case "AUDIT":
                return !"LOGIN".equals(m) && !"BEHAVIOR".equals(m) && !"CHANGE".equals(m);
            default:
                return true;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventEntity> list(String module, String operator) {
        List<AuditEventEntity> all = eventRepository.findAllByOrderBySeqDesc();
        if (module != null && !module.isBlank()) {
            all = new ArrayList<>(all);
            all.removeIf(e -> !module.equals(e.getModule()));
        }
        if (operator != null && !operator.isBlank()) {
            all = new ArrayList<>(all);
            all.removeIf(e -> !operator.equals(e.getOperator()));
        }
        return all;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String id) {
        AuditEventEntity e = eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("审计记录不存在: " + id));
        @SuppressWarnings("unchecked")
        boolean broken = ((List<String>) verifyRange(null, null).get("brokenIds")).contains(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("event", e);
        out.put("hashStatus", broken ? "TAMPERED" : "OK");
        return out;
    }

    // ---- Verify ----

    @Transactional(readOnly = true)
    public Map<String, Object> verifyRange(String from, String to) {
        List<AuditEventEntity> all = orderedAsc();
        Map<String, Object> res = new LinkedHashMap<>();
        List<String> broken = new ArrayList<>();
        String cursor = GENESIS;
        int checked = 0;
        for (AuditEventEntity e : all) {
            checked++;
            boolean bad = false;
            if (!e.getPrevHash().equals(cursor)) bad = true;
            if (!hash(e).equals(e.getEventHash())) bad = true;
            if (bad) broken.add(e.getId());
            cursor = e.getEventHash();
        }
        res.put("ok", broken.isEmpty());
        res.put("checked", checked);
        res.put("brokenIds", broken);
        return res;
    }

    // ---- Export ----

    @Transactional(readOnly = true)
    public Map<String, Object> export(String from, String to) {
        List<AuditEventEntity> events = orderedAsc();
        StringBuilder csv = new StringBuilder();
        csv.append("seq,event_time,module,action,operator,resource_type,resource_key,status,event_hash\n");
        List<String> hashes = new ArrayList<>();
        for (AuditEventEntity e : events) {
            csv.append(e.getSeq()).append(',').append(e.getEventTime()).append(',')
                    .append(nvl(e.getModule())).append(',').append(nvl(e.getAction())).append(',')
                    .append(nvl(e.getOperator())).append(',').append(nvl(e.getResourceType())).append(',')
                    .append(nvl(e.getResourceKey())).append(',').append(nvl(e.getStatus())).append(',')
                    .append(e.getEventHash()).append('\n');
            hashes.add(e.getEventHash());
        }
        String rootHash = sha256(String.join("|", hashes));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", events.size());
        out.put("rootHash", rootHash);
        out.put("csv", csv.toString());
        return out;
    }

    @Transactional
    public AuditArchiveEntity archive() {
        List<AuditEventEntity> events = orderedAsc();
        List<String> hashes = new ArrayList<>();
        for (AuditEventEntity e : events) hashes.add(e.getEventHash());
        String root = sha256(String.join("|", hashes));
        AuditArchiveEntity ar = AuditArchiveEntity.builder()
                .periodStart(events.isEmpty() ? now() : events.get(0).getEventTime())
                .periodEnd(events.isEmpty() ? now() : events.get(events.size() - 1).getEventTime())
                .recordCount(events.size()).rootHash(root).signedBy("td-signing-key")
                .archivePath("audit/archive-" + System.currentTimeMillis()).createTime(now())
                .build();
        return archiveRepository.save(ar);
    }

    @Transactional(readOnly = true)
    public List<AuditArchiveEntity> archives() {
        return archiveRepository.findAllByOrderByCreateTimeDesc();
    }

    // ---- Policy ----

    @Transactional(readOnly = true)
    public List<AuditPolicyEntity> policies() {
        return policyRepository.findAll();
    }

    @Transactional
    public AuditPolicyEntity savePolicy(AuditPolicyEntity p) {
        if (p.getId() != null) {
            AuditPolicyEntity db = policyRepository.findById(p.getId())
                    .orElseThrow(() -> new BusinessException("策略不存在: " + p.getId()));
            db.setAuditLevel(p.getAuditLevel());
            db.setRetentionDays(p.getRetentionDays());
            db.setNotifyAlert(p.getNotifyAlert());
            return policyRepository.save(db);
        }
        p.setId(null);
        return policyRepository.save(p);
    }

    private String policyLevel(String module, String action) {
        return policyRepository.findByModuleAndAction(module, action)
                .map(AuditPolicyEntity::getAuditLevel)
                .orElse(policyRepository.findByModuleAndActionIsNull(module)
                        .map(AuditPolicyEntity::getAuditLevel)
                        .orElse("FULL"));
    }

    // ---- Hash helpers ----

    private List<AuditEventEntity> orderedAsc() {
        List<AuditEventEntity> all = new ArrayList<>(eventRepository.findAll());
        all.sort(Comparator.comparingLong(e -> e.getSeq() == null ? 0 : e.getSeq()));
        return all;
    }

    private String hash(AuditEventEntity e) {
        String canon = nvl(e.getModule()) + "|" + nvl(e.getAction()) + "|" + nvl(e.getResourceType()) + "|"
                + nvl(e.getResourceKey()) + "|" + nvl(e.getOperator()) + "|" + nvl(e.getEventTime()) + "|"
                + nvl(e.getReqId()) + "|" + nvl(e.getStatus()) + "|" + nvl(e.getDetailJson()) + "|"
                + (e.getSeq() == null ? 0 : e.getSeq()) + "|" + nvl(e.getPrevHash());
        return sha256(canon);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }
}