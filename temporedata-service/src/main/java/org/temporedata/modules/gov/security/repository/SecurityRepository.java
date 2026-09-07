package org.temporedata.modules.gov.security.repository;

import org.temporedata.modules.gov.security.entity.SecurityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityRepository extends JpaRepository<SecurityEntity, String> {}
