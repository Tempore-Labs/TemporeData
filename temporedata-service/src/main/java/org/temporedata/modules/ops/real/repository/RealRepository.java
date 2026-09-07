package org.temporedata.modules.ops.real.repository;

import org.temporedata.modules.ops.real.entity.RealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealRepository extends JpaRepository<RealEntity, String> {}
