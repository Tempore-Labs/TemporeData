package org.temporedata.modules.dev.query.repository;

import org.temporedata.modules.dev.query.entity.QueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<QueryEntity, String> {}
