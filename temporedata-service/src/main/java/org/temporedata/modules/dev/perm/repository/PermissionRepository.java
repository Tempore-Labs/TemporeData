package org.temporedata.modules.dev.perm.repository;

import org.temporedata.modules.dev.perm.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

    List<PermissionEntity> findByRoleIdIn(List<String> roleIds);

    List<PermissionEntity> findByResourceTypeAndRoleIdIn(String resourceType, List<String> roleIds);

    void deleteByRoleId(String roleId);
}