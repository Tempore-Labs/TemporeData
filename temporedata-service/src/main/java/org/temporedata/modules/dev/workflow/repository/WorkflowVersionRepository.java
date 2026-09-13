package org.temporedata.modules.dev.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.temporedata.modules.dev.workflow.entity.WorkflowVersionEntity;

import java.util.List;
import java.util.Optional;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersionEntity, String> {

    List<WorkflowVersionEntity> findByWorkflowIdOrderByVersionNoDesc(String workflowId);

    Optional<WorkflowVersionEntity> findFirstByWorkflowIdOrderByVersionNoDesc(String workflowId);
}