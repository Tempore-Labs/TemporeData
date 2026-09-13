package org.temporedata.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temporedata.contract.entity.ContractEntity;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for temporedata_data_contract.
 */
@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, Long> {

    Optional<ContractEntity> findByDatasetId(Long datasetId);

    boolean existsByDatasetId(Long datasetId);

    List<ContractEntity> findByStatus(String status);
}