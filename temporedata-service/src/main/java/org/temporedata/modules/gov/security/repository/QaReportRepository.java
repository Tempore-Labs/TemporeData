package org.temporedata.modules.gov.security.repository;

import org.temporedata.modules.gov.security.entity.QaReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QaReportRepository extends JpaRepository<QaReportEntity, String> {

    List<QaReportEntity> findByVersionOrderByRunTimeDesc(String version);

    List<QaReportEntity> findAllByOrderByRunTimeDesc();
}