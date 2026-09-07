package org.temporedata.modules.asset.tag.service;

import org.temporedata.modules.asset.tag.entity.TagEntity;
import org.temporedata.modules.asset.tag.repository.TagRepository;
import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.base.pojos.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import static org.temporedata.common.cache.CacheConfig.CACHE_TAG;

import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Cacheable(value = CACHE_TAG)
    public Page<TagEntity> page(Pageable pageable) { return tagRepository.findAll(pageable); }

    @Cacheable(value = CACHE_TAG)
    public List<TagEntity> list() { return tagRepository.findAll(); }

    @Cacheable(value = CACHE_TAG)
    public TagEntity get(String id) { return tagRepository.findById(id).orElseThrow(() -> new BusinessException("Tag not found: " + id)); }

    @CacheEvict(value = CACHE_TAG, allEntries = true)
    @Transactional
    public TagEntity create(TagEntity entity) { return tagRepository.save(entity); }

    @CacheEvict(value = CACHE_TAG, allEntries = true)
    @Transactional
    public TagEntity update(TagEntity entity) { return tagRepository.save(entity); }

    @CacheEvict(value = CACHE_TAG, allEntries = true)
    @Transactional
    public void delete(String id) { tagRepository.deleteById(id); }
}
