package org.temporedata.modules.gov.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.gov.security.entity.QaReportEntity;
import org.temporedata.modules.gov.security.entity.VulnEntity;
import org.temporedata.modules.gov.security.repository.QaReportRepository;
import org.temporedata.modules.gov.security.repository.VulnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P3-13 supply-chain & QA: vulnerability tracker, SBOM summary and quality gates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VulnRepository vulnRepository;
    private final QaReportRepository qaReportRepository;

    // ---- Vulnerabilities ----

    @Transactional(readOnly = true)
    public List<VulnEntity> vulns() {
        return vulnRepository.findByOrderByReportTimeDesc();
    }

    @Transactional
    public VulnEntity createVuln(VulnEntity v) {
        if (v.getPackageName() == null || v.getPackageName().isBlank()) {
            throw new BusinessException("依赖包名必填");
        }
        v.setId(null);
        v.setStatus(v.getStatus() == null ? "OPEN" : v.getStatus());
        v.setReportTime(now());
        return vulnRepository.save(v);
    }

    @Transactional
    public VulnEntity accept(String id, String mitigation) {
        VulnEntity v = findVuln(id);
        v.setStatus("RISK_ACCEPTED");
        v.setMitigation(mitigation);
        return vulnRepository.save(v);
    }

    @Transactional
    public VulnEntity updateStatus(String id, String status) {
        VulnEntity v = findVuln(id);
        v.setStatus(status);
        return vulnRepository.save(v);
    }

    // ---- SBOM ----

    @Transactional(readOnly = true)
    public Map<String, Object> sbom(String version) {
        List<VulnEntity> high = vulnRepository.findBySeverityInAndStatusNot(
                List.of("CRITICAL", "HIGH"), "FIXED");
        long openHigh = high.size();
        Map<String, Object> bom = new LinkedHashMap<>();
        bom.put("bomFormat", "CycloneDX");
        bom.put("specVersion", "1.4");
        bom.put("version", version == null ? "1.0.0" : version);
        bom.put("serialNumber", "urn:uuid:" + java.util.UUID.randomUUID());
        bom.put("components", List.of());
        bom.put("openHighVulnerabilities", openHigh);
        bom.put("generatedAt", now());
        return bom;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gateCheck() {
        long critical = vulnRepository.findBySeverityInAndStatusNot(List.of("CRITICAL"), "FIXED").size();
        long high = vulnRepository.findBySeverityInAndStatusNot(List.of("HIGH"), "FIXED").size();
        List<QaReportEntity> reports = qaReportRepository.findAllByOrderByRunTimeDesc();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("criticalOpen", critical);
        out.put("highOpen", high);
        out.put("gate", (critical + high == 0) ? "PASS" : "BLOCK");
        out.put("reports", reports);
        return out;
    }

    // ---- QA ----

    @Transactional(readOnly = true)
    public List<QaReportEntity> qaReports(String version) {
        return version == null || version.isBlank()
                ? qaReportRepository.findAllByOrderByRunTimeDesc()
                : qaReportRepository.findByVersionOrderByRunTimeDesc(version);
    }

    @Transactional
    public QaReportEntity addQa(QaReportEntity q) {
        q.setId(null);
        q.setRunTime(now());
        return qaReportRepository.save(q);
    }

    private VulnEntity findVuln(String id) {
        return vulnRepository.findById(id)
                .orElseThrow(() -> new BusinessException("漏洞记录不存在: " + id));
    }

    private String now() {
        return LocalDateTime.now().format(DTF);
    }
}