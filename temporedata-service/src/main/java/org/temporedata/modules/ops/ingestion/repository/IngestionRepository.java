package org.temporedata.modules.ops.ingestion.repository;

import org.temporedata.modules.ops.ingestion.entity.IngestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionRepository extends JpaRepository<IngestionEntity, String> {}
