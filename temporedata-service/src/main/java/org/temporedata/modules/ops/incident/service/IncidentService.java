package org.temporedata.modules.ops.incident.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.incident.entity.IncidentEntity;
import org.temporedata.modules.ops.incident.repository.IncidentRepository;

import java.util.List;
import java.util.Optional;

/**
 * [ENT-P2] Incident &amp; RCA orchestration over temporedata_incident.
 *
 * <p>Opens incidents from quality/alarm/workflow/API sources, tracks the lifecycle
 * (OPEN -&gt; INVESTIGATING -&gt; RESOLVED -&gt; CLOSED) and attaches root-cause analysis.
 * Is the persistence hub for the DataOps / RCA agent loop.
 */
@Service
public class IncidentService {

    private final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IncidentEntity open(String title, String sourceType, Long datasetId, String severity, String rca) {
        IncidentEntity incident = IncidentEntity.builder()
                .title(title)
                .sourceType(sourceType)
                .datasetId(datasetId)
                .severity(severity == null ? "MEDIUM" : severity)
                .status("OPEN")
                .rcaAnalysis(rca)
                .build();
        return repository.save(incident);
    }

    @Transactional
    public IncidentEntity updateStatus(Long id, String status) {
        IncidentEntity incident = require(id);
        incident.setStatus(status);
        return repository.save(incident);
    }

    @Transactional
    public IncidentEntity setRca(Long id, String rcaAnalysis) {
        IncidentEntity incident = require(id);
        incident.setRcaAnalysis(rcaAnalysis);
        if ("OPEN".equals(incident.getStatus())) {
            incident.setStatus("INVESTIGATING");
        }
        return repository.save(incident);
    }

    @Transactional(readOnly = true)
    public Optional<IncidentEntity> get(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> listByDataset(Long datasetId) {
        return repository.findByDatasetIdOrderByCreatedAtDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> listByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<IncidentEntity> listBySeverity(String severity) {
        return repository.findBySeverityOrderByCreatedAtDesc(severity);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return repository.countByStatus("OPEN");
    }

    private IncidentEntity require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("incident not found: " + id));
    }
}