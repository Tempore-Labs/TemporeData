package org.temporedata.modules.svc.form.repository;

import org.temporedata.modules.svc.form.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, String> {
    List<FormSubmissionEntity> findByFormId(String formId);
}