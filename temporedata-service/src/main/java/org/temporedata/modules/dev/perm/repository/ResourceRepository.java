package org.temporedata.modules.dev.perm.repository;

import org.temporedata.modules.dev.perm.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<ResourceEntity, String> {

    Optional<ResourceEntity> findByResourceTypeAndResourceKey(String resourceType, String resourceKey);

    List<ResourceEntity> findByResourceType(String resourceType);

    List<ResourceEntity> findByOwner(String owner);
}