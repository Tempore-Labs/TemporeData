package org.temporedata.modules.dev.globalvar.repository;

import org.temporedata.modules.dev.globalvar.entity.GlobalvarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalvarRepository extends JpaRepository<GlobalvarEntity, String> {}
