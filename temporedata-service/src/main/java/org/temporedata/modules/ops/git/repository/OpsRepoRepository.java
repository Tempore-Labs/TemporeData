package org.temporedata.modules.ops.git.repository;

import org.temporedata.modules.ops.git.entity.OpsRepoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpsRepoRepository extends JpaRepository<OpsRepoEntity, String> {

    Optional<OpsRepoEntity> findByProviderAndRepoRef(String provider, String repoRef);

    List<OpsRepoEntity> findByEnabledTrue();
}