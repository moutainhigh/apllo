package com.gofobao.framework.asset.repository;

import com.gofobao.framework.asset.entity.AssetsChangeLogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by Administrator on 2017/7/8 0008.
 */
@Repository
public interface AssetsChangeLogItemRepository extends JpaRepository<AssetsChangeLogItem, Long> {
}