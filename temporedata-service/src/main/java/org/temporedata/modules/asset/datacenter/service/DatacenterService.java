package org.temporedata.modules.asset.datacenter.service;

import org.temporedata.modules.asset.datacenter.entity.DatacenterEntity;
import org.temporedata.modules.asset.datacenter.repository.DatacenterRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class DatacenterService {

    private final DatacenterRepository datacenterRepository;

    public Page<DatacenterEntity> page(Pageable pageable) { return datacenterRepository.findAll(pageable); }

    public List<DatacenterEntity> list() { return datacenterRepository.findAll(); }

    public DatacenterEntity get(String id) { return datacenterRepository.findById(id).orElseThrow(() -> new BusinessException("Datacenter not found: " + id)); }

    @Transactional
    public DatacenterEntity create(DatacenterEntity entity) { return datacenterRepository.save(entity); }

    @Transactional
    public DatacenterEntity update(DatacenterEntity entity) { return datacenterRepository.save(entity); }

    @Transactional
    public void delete(String id) { datacenterRepository.deleteById(id); }
}
