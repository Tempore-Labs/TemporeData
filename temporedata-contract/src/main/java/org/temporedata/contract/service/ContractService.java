package org.temporedata.contract.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.contract.CompatibilityMode;
import org.temporedata.contract.SchemaCompatChecker;
import org.temporedata.contract.entity.ContractEntity;
import org.temporedata.contract.repository.ContractRepository;

import java.util.List;
import java.util.Optional;

/**
 * [ENT-P1] Data contract lifecycle &amp; gate orchestration over temporedata_data_contract.
 *
 * <p>Registers/provides the declared contract (schema / SLA / quality YAML), manages the
 * producer gate status, and validates a proposed schema against the promised one under the
 * contract's compatibility mode. A breaking change flips the contract to BROKEN which blocks
 * publishing and notifies owner/consumers (notification wiring is caller-side).
 */
@Service("enterpriseContractService")
public class ContractService {

    private final ContractRepository repository;

    public ContractService(ContractRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContractEntity register(Long datasetId, String contractYaml, CompatibilityMode mode, String owner) {
        if (datasetId == null || contractYaml == null || contractYaml.isBlank()) {
            throw new BusinessException("contract.datasetId and contractYaml are required");
        }
        if (repository.existsByDatasetId(datasetId)) {
            throw new BusinessException("contract already exists for dataset: " + datasetId);
        }
        ContractEntity contract = ContractEntity.builder()
                .datasetId(datasetId)
                .contractYaml(contractYaml)
                .compatibilityMode((mode == null ? CompatibilityMode.BACKWARD : mode).name())
                .status("DRAFT")
                .owner(owner)
                .build();
        return repository.save(contract);
    }

    /** Activate a DRAFT so downstream checks enforce it. */
    @Transactional
    public ContractEntity activate(Long id) {
        ContractEntity c = require(id);
        c.setStatus("ACTIVE");
        return repository.save(c);
    }

    /** Explicitly break (e.g. after a detected drift that violates the contract). */
    @Transactional
    public ContractEntity markBroken(Long id) {
        ContractEntity c = require(id);
        c.setStatus("BROKEN");
        return repository.save(c);
    }

    /**
     * Validate a proposed schema signature against the contract's promised schema.
     * On incompatibility, if the contract is ACTIVE it is flipped to BROKEN (gate closed).
     *
     * @return the contract after evaluation
     */
    @Transactional
    public ContractEntity check(Long datasetId, String promisedSchema, String providedSchema) {
        ContractEntity c = repository.findByDatasetId(datasetId)
                .orElseThrow(() -> new BusinessException("no contract for dataset: " + datasetId));
        CompatibilityMode mode = CompatibilityMode.from(c.getCompatibilityMode());
        boolean ok = SchemaCompatChecker.isCompatible(promisedSchema, providedSchema, mode);
        if (!ok && "ACTIVE".equals(c.getStatus())) {
            c.setStatus("BROKEN");
            repository.save(c);
        }
        return c;
    }

    @Transactional(readOnly = true)
    public Optional<ContractEntity> getByDataset(Long datasetId) {
        return repository.findByDatasetId(datasetId);
    }

    @Transactional(readOnly = true)
    public List<ContractEntity> list() {
        return repository.findAll();
    }

    private ContractEntity require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("contract not found: " + id));
    }
}