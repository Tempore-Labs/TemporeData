package org.temporedata.modules.ops.engine.repository;

import org.temporedata.modules.ops.engine.entity.EngineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EngineRepository extends JpaRepository<EngineEntity, String> {
    List<EngineEntity> findByType(String type);
}
