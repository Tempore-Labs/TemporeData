package org.temporedata.modules.ops.git.repository;

import org.temporedata.modules.ops.git.entity.OpsBuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpsBuildRepository extends JpaRepository<OpsBuildEntity, String> {

    List<OpsBuildEntity> findByRepoIdOrderByCreateTimeDesc(String repoId);

    List<OpsBuildEntity> findAllByOrderByCreateTimeDesc();
}