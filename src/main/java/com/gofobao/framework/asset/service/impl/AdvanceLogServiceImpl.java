package com.gofobao.framework.asset.service.impl;

import com.gofobao.framework.asset.entity.AdvanceLog;
import com.gofobao.framework.asset.repository.AdvanceLogRepository;
import com.gofobao.framework.asset.service.AdvanceLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * Created by Zeke on 2017/6/7.
 */
@Service
public class AdvanceLogServiceImpl implements AdvanceLogService{

    @Autowired
    private AdvanceLogRepository advanceLogRepository;



    public AdvanceLog save(AdvanceLog advanceLog){
        return advanceLogRepository.save(advanceLog);
    }

    public AdvanceLog findById(Long id){
        return advanceLogRepository.findOne(id);
    }

    public AdvanceLog findByRepaymentId(Long repaymentId){
        return advanceLogRepository.findByRepaymentId(repaymentId);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public AdvanceLog findByRepaymentIdLock(Long repaymentId){
        return advanceLogRepository.findByRepaymentId(repaymentId);
    }


    public AdvanceLog findByIdLock(Long id){
        return advanceLogRepository.findById(id);
    }

    public List<AdvanceLog> findList(Specification<AdvanceLog> specification){
        return advanceLogRepository.findAll(specification);
    }

    public List<AdvanceLog> findList(Specification<AdvanceLog> specification, Sort sort){
        return advanceLogRepository.findAll(specification,sort);
    }

    public List<AdvanceLog> findList(Specification<AdvanceLog> specification, Pageable pageable){
        return advanceLogRepository.findAll(specification,pageable).getContent();
    }

    public long count(Specification<AdvanceLog> specification){
        return advanceLogRepository.count(specification);
    }
}
