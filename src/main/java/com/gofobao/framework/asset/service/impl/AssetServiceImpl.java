package com.gofobao.framework.asset.service.impl;

import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.repository.AssetRepository;
import com.gofobao.framework.asset.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * Created by Zeke on 2017/5/19.
 */
@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Override
    public List<Asset> findList(Specification<Asset> specification) {
        return assetRepository.findAll(specification);
    }

    @Override
    public List<Asset> findList(Specification<Asset> specification, Pageable pageable) {
        return assetRepository.findAll(specification, pageable).getContent();
    }

    @Override
    public List<Asset> findList(Specification<Asset> specification, Sort sort) {
        return assetRepository.findAll(specification, sort);
    }

    @Override
    public long count(Specification<Asset> specification) {
        return assetRepository.count(specification);
    }

    /**
     * 根据id产寻资产
     *
     * @param id
     * @return
     */
    @Override
    public Asset findByUserId(Long id) {
        return assetRepository.findOne(id);
    }

    @Override
    public Asset findByUserIdLock(Long id) {
        return assetRepository.findByUserId(id);
    }

    @Override
    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    @Override
    public Asset updateById(Asset asset) {
        if (ObjectUtils.isEmpty(asset) || ObjectUtils.isEmpty(asset.getUserId())) {
            return null;
        }
        return assetRepository.save(asset);
    }

    @Override
    public List<Asset> findByUserIds(List<Long> userIds) {
        return assetRepository.findByUserIdIn(userIds);
    }
}
