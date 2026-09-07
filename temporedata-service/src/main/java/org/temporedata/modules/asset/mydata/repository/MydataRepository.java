package org.temporedata.modules.asset.mydata.repository;

import org.temporedata.modules.asset.mydata.entity.MydataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MydataRepository extends JpaRepository<MydataEntity, String> {

    boolean existsByUserIdAndResourceId(String userId, String resourceId);
}
