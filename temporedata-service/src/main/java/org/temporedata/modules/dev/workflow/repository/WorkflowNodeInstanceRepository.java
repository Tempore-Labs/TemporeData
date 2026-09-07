package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowNodeInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowNodeInstanceRepository extends JpaRepository<WorkflowNodeInstanceEntity, String> {

    List<WorkflowNodeInstanceEntity> findByInstanceIdOrderByCreatedAtAsc(String instanceId);
}