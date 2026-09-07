package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowInstanceLogRepository extends JpaRepository<WorkflowInstanceLogEntity, String> {

    List<WorkflowInstanceLogEntity> findByInstanceIdOrderByCreatedAtAsc(String instanceId);
}