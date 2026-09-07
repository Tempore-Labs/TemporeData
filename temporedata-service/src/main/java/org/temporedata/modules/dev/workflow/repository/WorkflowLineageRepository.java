package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowLineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowLineageRepository extends JpaRepository<WorkflowLineageEntity, String> {

    void deleteByWorkflowId(String workflowId);

    List<WorkflowLineageEntity> findByWorkflowId(String workflowId);
}