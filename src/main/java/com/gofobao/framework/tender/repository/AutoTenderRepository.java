package com.gofobao.framework.tender.repository;

import com.gofobao.framework.tender.entity.AutoTender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Zeke on 2017/5/27.
 */
@Repository
public interface AutoTenderRepository extends JpaRepository<AutoTender,Long>,JpaSpecificationExecutor<AutoTender>{
    /**
     * 根据用户id查询用户自动投标规则
     * @param userId
     * @return
     */
    List<AutoTender> findByUserId(Long userId);

}
