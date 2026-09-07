package org.temporedata.modules.dev.resource.repository;

import org.temporedata.modules.dev.resource.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("devResourceRepository")
public interface ResourceRepository extends JpaRepository<ResourceEntity, String> {}
