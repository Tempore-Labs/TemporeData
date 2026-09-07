package org.temporedata.modules.ops.cluster.repository;

import org.temporedata.modules.ops.cluster.entity.ClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterRepository extends JpaRepository<ClusterEntity, String> {}
