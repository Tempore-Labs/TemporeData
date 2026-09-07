package org.temporedata.modules.ops.container.repository;

import org.temporedata.modules.ops.container.entity.ContainerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerEntity, String> {}
