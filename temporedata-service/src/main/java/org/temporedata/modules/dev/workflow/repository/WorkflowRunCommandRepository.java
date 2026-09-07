package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowRunCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRunCommandRepository extends JpaRepository<WorkflowRunCommandEntity, String> {

    List<WorkflowRunCommandEntity> findByInstanceIdOrderByCreateTimeAsc(String instanceId);
}