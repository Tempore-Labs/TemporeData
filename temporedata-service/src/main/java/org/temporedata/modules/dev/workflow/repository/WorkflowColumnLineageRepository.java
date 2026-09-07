package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowColumnLineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowColumnLineageRepository extends JpaRepository<WorkflowColumnLineageEntity, String> {

    void deleteByWorkflowId(String workflowId);

    List<WorkflowColumnLineageEntity> findByWorkflowId(String workflowId);
}