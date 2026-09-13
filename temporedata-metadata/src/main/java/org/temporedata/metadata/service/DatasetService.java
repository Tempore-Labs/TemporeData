package org.temporedata.metadata.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.metadata.entity.DatasetEntity;
import org.temporedata.metadata.repository.DatasetRepository;

import java.util.List;
import java.util.Optional;

/**
 * [ENT-P0] Dataset CRUD orchestration over temporedata_dataset.
 *
 * <p>Serves as the registry for the unified data object. Downstream capability modules
 * (metadata versioning, modeling, quality, asset, contract) link to a dataset by id/code.
 */
@Service
public class DatasetService {

    private final DatasetRepository repository;

    public DatasetService(DatasetRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DatasetEntity create(DatasetEntity dataset) {
        if (dataset.getCode() == null || dataset.getCode().isBlank()) {
            throw new BusinessException("dataset.code is required");
        }
        if (repository.existsByCode(dataset.getCode())) {
            throw new BusinessException("dataset.code already exists: " + dataset.getCode());
        }
        dataset.setId(null); // let DB auto-generate
        return repository.save(dataset);
    }

    @Transactional
    public DatasetEntity update(Long id, DatasetEntity patch) {
        DatasetEntity existing = require(id);
        if (patch.getName() != null) {
            existing.setName(patch.getName());
        }
        if (patch.getLayer() != null) {
            existing.setLayer(patch.getLayer());
        }
        if (patch.getDomainId() != null) {
            existing.setDomainId(patch.getDomainId());
        }
        if (patch.getSecurityLevel() != null) {
            existing.setSecurityLevel(patch.getSecurityLevel());
        }
        if (patch.getOwner() != null) {
            existing.setOwner(patch.getOwner());
        }
        return repository.save(existing);
    }

    @Transactional(readOnly = true)
    public DatasetEntity getById(Long id) {
        return require(id);
    }

    @Transactional(readOnly = true)
    public Optional<DatasetEntity> getByCode(String code) {
        return repository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public Page<DatasetEntity> page(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<DatasetEntity> listByDomain(Long domainId) {
        return repository.findByDomainId(domainId);
    }

    @Transactional
    public void delete(Long id) {
        DatasetEntity existing = require(id);
        repository.delete(existing);
    }

    private DatasetEntity require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("dataset not found: " + id));
    }
}