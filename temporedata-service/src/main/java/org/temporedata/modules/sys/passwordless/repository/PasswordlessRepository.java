package org.temporedata.modules.sys.passwordless.repository;

import org.temporedata.modules.sys.passwordless.entity.PasswordlessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordlessRepository extends JpaRepository<PasswordlessEntity, String> {}
