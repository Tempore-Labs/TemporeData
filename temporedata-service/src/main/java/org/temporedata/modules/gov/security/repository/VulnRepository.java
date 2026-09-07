package org.temporedata.modules.gov.security.repository;

import org.temporedata.modules.gov.security.entity.VulnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VulnRepository extends JpaRepository<VulnEntity, String> {

    List<VulnEntity> findByOrderByReportTimeDesc();

    List<VulnEntity> findBySeverityInAndStatusNot(List<String> severities, String status);
}