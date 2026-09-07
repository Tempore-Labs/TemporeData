package org.temporedata.modules.svc.report.repository;

import org.temporedata.modules.svc.report.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, String> {}
