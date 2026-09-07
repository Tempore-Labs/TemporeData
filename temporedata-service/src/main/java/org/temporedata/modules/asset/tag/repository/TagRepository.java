package org.temporedata.modules.asset.tag.repository;

import org.temporedata.modules.asset.tag.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, String> {}
