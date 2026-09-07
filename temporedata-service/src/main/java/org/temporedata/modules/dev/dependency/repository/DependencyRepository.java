package org.temporedata.modules.dev.dependency.repository;

import org.temporedata.modules.dev.dependency.entity.DependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DependencyRepository extends JpaRepository<DependencyEntity, String> {}
