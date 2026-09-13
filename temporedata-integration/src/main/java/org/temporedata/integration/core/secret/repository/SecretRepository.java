package org.temporedata.integration.core.secret.repository;

import org.temporedata.integration.core.secret.entity.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretRepository extends JpaRepository<SecretEntity, String> {}
