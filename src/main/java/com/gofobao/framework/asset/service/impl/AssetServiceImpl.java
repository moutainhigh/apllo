package com.gofobao.framework.asset.service.impl;

import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.repository.AssetRepository;
import com.gofobao.framework.asset.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.persistence.LockModeType;

/**
 * Created by Zeke on 2017/5/19.
 */
@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetRepository assetRepository;

    /**
     * 根据id产寻资产
     *
     * @param id
     * @return
     */
    public Asset findByUserId(Long id) {
        return assetRepository.findOne(id);
    }

    public Asset findByUserIdLock(Long id) {
        return assetRepository.findByUserId(id);
    }

    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset updateById(Asset asset) {
        if (ObjectUtils.isEmpty(asset) || ObjectUtils.isEmpty(asset.getUserId())) {
            return null;
        }
        return assetRepository.save(asset);
    }
}
