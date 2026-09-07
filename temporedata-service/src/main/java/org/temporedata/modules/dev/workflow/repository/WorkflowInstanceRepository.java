package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, String> {

    List<WorkflowInstanceEntity> findByWorkflowIdOrderByStartTimeDesc(String workflowId);

    Optional<WorkflowInstanceEntity> findTopByWorkflowIdAndBizDateOrderByStartTimeDesc(String workflowId, String bizDate);

    Optional<WorkflowInstanceEntity> findFirstByWorkflowIdOrderByStartTimeDesc(String workflowId);
}