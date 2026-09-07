package org.temporedata.modules.integration.file.repository;

import org.temporedata.modules.integration.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, String>, JpaSpecificationExecutor<FileEntity> {}