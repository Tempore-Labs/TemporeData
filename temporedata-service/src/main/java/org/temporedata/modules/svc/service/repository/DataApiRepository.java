package org.temporedata.modules.svc.service.repository;

import org.temporedata.modules.svc.service.entity.DataApiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataApiRepository extends JpaRepository<DataApiEntity, String> {
    Optional<DataApiEntity> findByPath(String path);
}