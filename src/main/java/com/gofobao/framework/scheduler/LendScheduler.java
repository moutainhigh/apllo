package com.gofobao.framework.scheduler;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.common.data.DataObject;
import com.gofobao.framework.common.data.LtSpecification;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.ExceptionEmailHelper;
import com.gofobao.framework.lend.entity.Lend;
import com.gofobao.framework.lend.service.LendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by Zeke on 2017/7/10.
 */
@Component
@Slf4j
public class LendScheduler {

    @Autowired
    private LendService lendService;

    @Autowired
    private ExceptionEmailHelper exceptionEmailHelper;

    @Scheduled(cron = "0 0 0,21 * * ? ")
    @Transactional(rollbackOn = Exception.class)
    public void process() {
        try {
            log.info("取消摘草任务调度启动");
            Specification<Lend> ls = Specifications
                    .<Lend>and()
                    .eq("status", 0)
                    .predicate(new LtSpecification("createdAt", new DataObject(DateHelper.beginOfDate(new Date()))))
                    .build();

            List<Lend> lendList = lendService.findList(ls);
            Optional<List<Lend>> lendOptional = Optional.of(lendList);
            lendOptional.ifPresent(lends -> lendList.forEach(lend -> {
                lend.setStatus(1);
            }));

            lendService.save(lendList);
        } catch (Exception e) {
            exceptionEmailHelper.sendException("取消摘草任务", e);
        }

    }
}
