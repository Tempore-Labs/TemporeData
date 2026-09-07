package org.temporedata.modules.asset.mydata.service;

import org.temporedata.modules.asset.mydata.entity.MydataEntity;
import org.temporedata.modules.asset.mydata.repository.MydataRepository;
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
public class MydataService {

    private final MydataRepository mydataRepository;

    public Page<MydataEntity> page(Pageable pageable) { return mydataRepository.findAll(pageable); }

    public List<MydataEntity> list() { return mydataRepository.findAll(); }

    public MydataEntity get(String id) { return mydataRepository.findById(id).orElseThrow(() -> new BusinessException("Mydata not found: " + id)); }

    @Transactional
    public MydataEntity create(MydataEntity entity) { return mydataRepository.save(entity); }

    @Transactional
    public MydataEntity update(MydataEntity entity) { return mydataRepository.save(entity); }

    @Transactional
    public void delete(String id) { mydataRepository.deleteById(id); }
}
