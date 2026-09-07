package org.temporedata.modules.sys.auth.repository;

import org.temporedata.modules.sys.auth.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRepository extends JpaRepository<AuthEntity, String> {}
