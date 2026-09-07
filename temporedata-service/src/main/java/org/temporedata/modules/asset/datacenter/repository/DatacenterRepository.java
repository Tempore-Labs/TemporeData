package org.temporedata.modules.asset.datacenter.repository;

import org.temporedata.modules.asset.datacenter.entity.DatacenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatacenterRepository extends JpaRepository<DatacenterEntity, String> {}
