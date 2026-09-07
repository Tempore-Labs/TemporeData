package org.temporedata.modules.ops.sync.repository;

import org.temporedata.modules.ops.sync.entity.SyncEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncRepository extends JpaRepository<SyncEntity, String> {}
