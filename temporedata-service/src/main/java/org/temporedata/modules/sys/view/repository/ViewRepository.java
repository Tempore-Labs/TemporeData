package org.temporedata.modules.sys.view.repository;

import org.temporedata.modules.sys.view.entity.ViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewRepository extends JpaRepository<ViewEntity, String> {}
