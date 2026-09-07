package org.temporedata.modules.sys.role.repository;

import org.temporedata.modules.sys.role.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleEntity.UserRoleId> {

    List<UserRoleEntity> findByIdUserId(String userId);

    List<UserRoleEntity> findByIdRoleId(String roleId);

    void deleteByIdUserId(String userId);
}