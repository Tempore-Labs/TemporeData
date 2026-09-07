package org.temporedata.modules.gov.sensitive.repository;

import org.temporedata.modules.gov.sensitive.entity.SensitiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensitiveRepository extends JpaRepository<SensitiveEntity, String> {}
