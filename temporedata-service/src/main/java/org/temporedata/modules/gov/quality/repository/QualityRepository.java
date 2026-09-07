package org.temporedata.modules.gov.quality.repository;

import org.temporedata.modules.gov.quality.entity.QualityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityRepository extends JpaRepository<QualityEntity, String> {}
