package com.gofobao.framework.integral.service.impl;

import com.gofobao.framework.integral.entity.IntegralLog;
import com.gofobao.framework.integral.repository.IntegralLogRepository;
import com.gofobao.framework.integral.service.IntegralLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Zeke on 2017/5/22.
 */
@Service
public class IntegralLogServiceImpl implements IntegralLogService {

    @Autowired
    private IntegralLogRepository integralLogRepository;

    public List<IntegralLog> findByUserId(Long userId, int pageIndex, int pageSize) {
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, " id"));
        Pageable pageable = new PageRequest(pageIndex, pageSize, sort);
        return integralLogRepository.findByUserId(userId, pageable);
    }
}
