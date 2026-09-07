package org.temporedata.modules.sys.settings.repository;

import org.temporedata.modules.sys.settings.entity.SettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<SettingsEntity, String> {}
