package com.gofobao.framework.system.biz.impl;

import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.system.biz.HomeBiz;
import com.gofobao.framework.system.biz.StatisticBiz;
import com.gofobao.framework.system.service.BannerService;
import com.gofobao.framework.system.vo.response.IndexBorrow;
import com.gofobao.framework.system.vo.response.NewIndexStatisics;
import com.gofobao.framework.system.vo.response.VoIndexResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@Slf4j
public class HomeBizImpl implements HomeBiz {

    @Autowired
    BannerService bannerService;

    @Autowired
    StatisticBiz statisticBiz;

    @Autowired
    BorrowService borrowService;

    @Override
    public ResponseEntity<VoIndexResp> home() {
        VoIndexResp result = VoBaseResp.ok("查询成功", VoIndexResp.class);
        result.setBannerList(bannerService.index("mobile")); //获取bannar图
        try {
            NewIndexStatisics newIndexStatisics = statisticBiz.queryMobileIndexData();  // 获取首页统计
            result.setNewIndexStatisics(newIndexStatisics);
            Borrow borrow = borrowService.findNoviceBorrow();
            IndexBorrow indexBorrow = new IndexBorrow();
            if (borrow.getId() != 0L) {
                indexBorrow.setApr(StringHelper.formatMon(borrow.getApr() / 100d));   // 年化收益
                indexBorrow.setBorrowId(borrow.getId());
                indexBorrow.setLimit(String.valueOf(borrow.getTimeLimit()));
                indexBorrow.setTitle(borrow.getName());
                indexBorrow.setStartLimit(String.valueOf(new Double(borrow.getLowest() / 100D).longValue()));
            } else {  // 默认
                indexBorrow.setApr("*");
                indexBorrow.setBorrowId(0L);
                indexBorrow.setLimit("*");
                indexBorrow.setTitle("*****");
                indexBorrow.setStartLimit("*");
            }
            result.setIndexBorrow(indexBorrow);
        } catch (Exception e) {
            log.error("首页异常", e);
        }
        return ResponseEntity.ok(result);
    }
}
