package org.temporedata.modules.gov.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.gov.security.entity.QaReportEntity;
import org.temporedata.modules.gov.security.entity.VulnEntity;
import org.temporedata.modules.gov.security.repository.QaReportRepository;
import org.temporedata.modules.gov.security.repository.VulnRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityServiceTest {

    @Mock VulnRepository vulnRepository;
    @Mock QaReportRepository qaReportRepository;

    private SecurityService service() {
        return new SecurityService(vulnRepository, qaReportRepository);
    }

    private VulnEntity vuln(String id, String severity, String status) {
        return VulnEntity.builder().id(id).packageName("log4j").severity(severity)
                .status(status).cve("CVE-TEST").build();
    }

    private void stubOpenVulns(List<VulnEntity> high, List<VulnEntity> critical) {
        when(vulnRepository.findBySeverityInAndStatusNot(eq(List.of("CRITICAL", "HIGH")), eq("FIXED"))).thenReturn(high);
        when(vulnRepository.findBySeverityInAndStatusNot(eq(List.of("CRITICAL")), eq("FIXED"))).thenReturn(critical);
        when(vulnRepository.findBySeverityInAndStatusNot(eq(List.of("HIGH")), eq("FIXED"))).thenReturn(high);
    }

    @Test
    void createVulnRejectsBlankPackage() {
        VulnEntity v = VulnEntity.builder().packageName(" ").build();
        assertThatThrownBy(() -> service().createVuln(v))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createVulnDefaultsStatusToOpen() {
        VulnEntity v = VulnEntity.builder().packageName("log4j").build();
        when(vulnRepository.save(org.mockito.ArgumentMatchers.any(VulnEntity.class))).thenReturn(v);
        VulnEntity saved = service().createVuln(v);
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.getReportTime()).isNotBlank();
    }

    @Test
    void acceptMarksRiskAcceptedAndRecordsMitigation() {
        VulnEntity v = vuln("v1", "HIGH", "OPEN");
        when(vulnRepository.findById("v1")).thenReturn(Optional.of(v));
        when(vulnRepository.save(org.mockito.ArgumentMatchers.any(VulnEntity.class))).thenReturn(v);
        VulnEntity out = service().accept("v1", "waf mitigates");
        assertThat(out.getStatus()).isEqualTo("RISK_ACCEPTED");
        assertThat(out.getMitigation()).isEqualTo("waf mitigates");
    }

    @Test
    void acceptUnknownVulnThrows() {
        when(vulnRepository.findById(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().accept("ghost", "x"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sbomCountsOnlyOpenHigh() {
        stubOpenVulns(List.of(vuln("v1", "HIGH", "OPEN"), vuln("v2", "CRITICAL", "FIXING")),
                List.of(vuln("v2", "CRITICAL", "FIXING")));
        Map<String, Object> bom = service().sbom("1.2.0");
        assertThat(bom.get("bomFormat")).isEqualTo("CycloneDX");
        assertThat(((Number) bom.get("openHighVulnerabilities")).longValue()).isEqualTo(2);
        assertThat(bom.get("version")).isEqualTo("1.2.0");
    }

    @Test
    void sbomUsesDefaultVersionWhenNull() {
        stubOpenVulns(List.of(), List.of());
        assertThat(service().sbom(null).get("version")).isEqualTo("1.0.0");
    }

    @Test
    void gatePassesWhenNoOpenCriticalOrHigh() {
        stubOpenVulns(List.of(), List.of());
        when(qaReportRepository.findAllByOrderByRunTimeDesc()).thenReturn(List.of());
        Map<String, Object> gate = service().gateCheck();
        assertThat(gate.get("gate")).isEqualTo("PASS");
    }

    @Test
    void gateBlocksWhenOpenHighExists() {
        stubOpenVulns(List.of(vuln("v1", "HIGH", "OPEN")), List.of());
        Map<String, Object> gate = service().gateCheck();
        assertThat(gate.get("gate")).isEqualTo("BLOCK");
        assertThat(((Number) gate.get("highOpen")).longValue()).isEqualTo(1);
    }

    @Test
    void gateBlocksWhenOpenCriticalExists() {
        stubOpenVulns(List.of(vuln("v1", "CRITICAL", "OPEN")),
                List.of(vuln("v1", "CRITICAL", "OPEN")));
        Map<String, Object> gate = service().gateCheck();
        assertThat(gate.get("gate")).isEqualTo("BLOCK");
        assertThat(((Number) gate.get("criticalOpen")).longValue()).isEqualTo(1);
    }

    @Test
    void qaReportsFiltersByVersionWhenProvided() {
        QaReportEntity r = QaReportEntity.builder().version("1.2.0").build();
        when(qaReportRepository.findByVersionOrderByRunTimeDesc("1.2.0")).thenReturn(List.of(r));
        assertThat(service().qaReports("1.2.0")).hasSize(1);
        verify(qaReportRepository).findByVersionOrderByRunTimeDesc("1.2.0");
    }

    @Test
    void updateStatusTracksState() {
        VulnEntity v = vuln("v1", "HIGH", "OPEN");
        when(vulnRepository.findById("v1")).thenReturn(Optional.of(v));
        when(vulnRepository.save(org.mockito.ArgumentMatchers.any(VulnEntity.class))).thenReturn(v);
        assertThat(service().updateStatus("v1", "FIXED").getStatus()).isEqualTo("FIXED");
    }
}