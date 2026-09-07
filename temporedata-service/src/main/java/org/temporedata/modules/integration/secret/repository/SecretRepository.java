package org.temporedata.modules.integration.secret.repository;

import org.temporedata.modules.integration.secret.entity.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretRepository extends JpaRepository<SecretEntity, String> {}
