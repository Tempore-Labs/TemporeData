package org.temporedata.modules.asset.indicator.repository;

import org.temporedata.modules.asset.indicator.entity.IndicatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndicatorRepository extends JpaRepository<IndicatorEntity, String> {}
