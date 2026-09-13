package org.temporedata.modules.ops.cost.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.modules.ops.cost.entity.CostEntity;
import org.temporedata.modules.ops.cost.repository.CostRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [ENT-P2] FinOps cost aggregation over temporedata_data_cost.
 *
 * <p>Records per-dataset-per-day cost rows (idempotent upsert on (dataset, date)), lists them
 * and produces a period aggregate. Later feeds the cost-reduction adviser.
 */
@Service
public class CostService {

    private final CostRepository repository;

    public CostService(CostRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CostEntity record(Long datasetId, BigDecimal compute, BigDecimal storage, BigDecimal query, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        CostEntity cost = repository.findByDatasetIdAndRecordDate(datasetId, day)
                .orElseGet(() -> CostEntity.builder()
                        .datasetId(datasetId)
                        .recordDate(day)
                        .build());
        if (compute != null) cost.setComputeCost(compute);
        if (storage != null) cost.setStorageCost(storage);
        if (query != null) cost.setQueryCost(query);
        return repository.save(cost);
    }

    @Transactional(readOnly = true)
    public CostEntity latest(Long datasetId) {
        return repository.findByDatasetIdOrderByRecordDateDesc(datasetId).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("no cost record for dataset: " + datasetId));
    }

    @Transactional(readOnly = true)
    public List<CostEntity> list(Long datasetId) {
        return repository.findByDatasetIdOrderByRecordDateDesc(datasetId);
    }

    /** Sum over a date range for the dataset. Returns all-zero when empty. */
    @Transactional(readOnly = true)
    public CostSummary summarize(Long datasetId, LocalDate from, LocalDate to) {
        List<CostEntity> rows = repository.findByDatasetIdAndRecordDateBetween(datasetId, from, to);
        BigDecimal compute = BigDecimal.ZERO;
        BigDecimal storage = BigDecimal.ZERO;
        BigDecimal query = BigDecimal.ZERO;
        for (CostEntity r : rows) {
            if (r.getComputeCost() != null) compute = compute.add(r.getComputeCost());
            if (r.getStorageCost() != null) storage = storage.add(r.getStorageCost());
            if (r.getQueryCost() != null) query = query.add(r.getQueryCost());
        }
        return new CostSummary(datasetId, from, to, compute, storage, query, rows.size());
    }

    /** Immutable period summary. */
    @lombok.Getter
    public static final class CostSummary {
        private final Long datasetId;
        private final LocalDate from;
        private final LocalDate to;
        private final BigDecimal computeCost;
        private final BigDecimal storageCost;
        private final BigDecimal queryCost;
        private final int dayCount;

        public CostSummary(Long datasetId, LocalDate from, LocalDate to,
                           BigDecimal computeCost, BigDecimal storageCost,
                           BigDecimal queryCost, int dayCount) {
            this.datasetId = datasetId;
            this.from = from;
            this.to = to;
            this.computeCost = computeCost;
            this.storageCost = storageCost;
            this.queryCost = queryCost;
            this.dayCount = dayCount;
        }

        public Long datasetId() { return datasetId; }
        public LocalDate from() { return from; }
        public LocalDate to() { return to; }
        public BigDecimal computeCost() { return computeCost; }
        public BigDecimal storageCost() { return storageCost; }
        public BigDecimal queryCost() { return queryCost; }
        public int dayCount() { return dayCount; }

        public BigDecimal total() {
            return computeCost.add(storageCost).add(queryCost);
        }
    }
}