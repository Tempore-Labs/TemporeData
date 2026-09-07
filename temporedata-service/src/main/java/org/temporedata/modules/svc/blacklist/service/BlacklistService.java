package org.temporedata.modules.svc.blacklist.service;

import org.temporedata.modules.svc.blacklist.entity.BlacklistEntity;
import org.temporedata.modules.svc.blacklist.repository.BlacklistRepository;
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
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;

    public Page<BlacklistEntity> page(Pageable pageable) { return blacklistRepository.findAll(pageable); }

    public List<BlacklistEntity> list() { return blacklistRepository.findAll(); }

    public BlacklistEntity get(String id) { return blacklistRepository.findById(id).orElseThrow(() -> new BusinessException("Blacklist not found: " + id)); }

    @Transactional
    public BlacklistEntity create(BlacklistEntity entity) { return blacklistRepository.save(entity); }

    @Transactional
    public BlacklistEntity update(BlacklistEntity entity) { return blacklistRepository.save(entity); }

    @Transactional
    public void delete(String id) { blacklistRepository.deleteById(id); }
}
