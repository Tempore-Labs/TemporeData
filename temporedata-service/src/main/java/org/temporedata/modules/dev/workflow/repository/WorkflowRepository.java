package org.temporedata.modules.dev.workflow.repository;

import org.temporedata.modules.dev.workflow.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {}
