package org.temporedata.modules.dev.work.repository;

import org.temporedata.modules.dev.work.entity.WorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkRepository extends JpaRepository<WorkEntity, String> {}
