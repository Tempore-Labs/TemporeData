package org.temporedata.modules.sys.preference.repository;

import org.temporedata.modules.sys.preference.entity.PreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreferenceRepository extends JpaRepository<PreferenceEntity, String> {}
