package org.temporedata.modules.svc.apilog.repository;

import org.temporedata.modules.svc.apilog.entity.ApilogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApilogRepository extends JpaRepository<ApilogEntity, String> {}
