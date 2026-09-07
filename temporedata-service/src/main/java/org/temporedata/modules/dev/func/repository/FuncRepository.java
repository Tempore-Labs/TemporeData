package org.temporedata.modules.dev.func.repository;

import org.temporedata.modules.dev.func.entity.FuncEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FuncRepository extends JpaRepository<FuncEntity, String> {}
