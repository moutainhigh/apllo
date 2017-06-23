package com.gofobao.framework.lend.service.impl;

import com.gofobao.framework.lend.entity.LendBlacklist;
import com.gofobao.framework.lend.repository.LendBlacklistRepository;
import com.gofobao.framework.lend.service.LendBlackListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Zeke on 2017/6/9.
 */
@Service
public class LendBlackListServiceImpl implements LendBlackListService {
    @Autowired
    private LendBlacklistRepository lendBlackListRepository;


    public LendBlacklist save(LendBlacklist lendBlackList) {
        return lendBlackListRepository.save(lendBlackList);
    }


    public LendBlacklist findById(Long id) {
        return lendBlackListRepository.findOne(id);
    }


    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<LendBlacklist> findList(Specification<LendBlacklist> specification) {
        return lendBlackListRepository.findAll(specification);
    }

    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<LendBlacklist> findList(Specification<LendBlacklist> specification, Sort sort) {
        return lendBlackListRepository.findAll(specification, sort);
    }

    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<LendBlacklist> findList(Specification<LendBlacklist> specification, Pageable pageable) {
        return lendBlackListRepository.findAll(specification, pageable).getContent();
    }

    public long count(Specification<LendBlacklist> specification) {
        return lendBlackListRepository.count(specification);
    }

    public void delete(LendBlacklist lendBlacklist){
        lendBlackListRepository.delete(lendBlacklist);
    }
}
