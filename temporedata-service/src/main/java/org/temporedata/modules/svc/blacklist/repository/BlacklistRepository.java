package org.temporedata.modules.svc.blacklist.repository;

import org.temporedata.modules.svc.blacklist.entity.BlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntity, String> {}
