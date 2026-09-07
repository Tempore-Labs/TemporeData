package org.temporedata.modules.svc.form.repository;

import org.temporedata.modules.svc.form.entity.FormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormRepository extends JpaRepository<FormEntity, String> {}
