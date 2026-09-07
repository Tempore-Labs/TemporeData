package org.temporedata.modules.sys.notify.repository;

import org.temporedata.modules.sys.notify.entity.NotifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifyRepository extends JpaRepository<NotifyEntity, String> {}
