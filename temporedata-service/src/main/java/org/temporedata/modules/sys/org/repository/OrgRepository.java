package org.temporedata.modules.sys.org.repository;

import org.temporedata.modules.sys.org.entity.OrgEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRepository extends JpaRepository<OrgEntity, String> {}
