package org.temporedata.modules.ops.ha.repository;

import org.temporedata.modules.ops.ha.entity.HaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HaRepository extends JpaRepository<HaEntity, String> {}
