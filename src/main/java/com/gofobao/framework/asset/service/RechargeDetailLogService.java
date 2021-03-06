package com.gofobao.framework.asset.service;

import com.gofobao.framework.asset.entity.RechargeDetailLog;
import com.gofobao.framework.asset.vo.request.VoPcRechargeReq;
import com.gofobao.framework.asset.vo.response.pc.RechargeLogs;
import com.gofobao.framework.asset.vo.response.pc.VoViewRechargeWarpRes;
import com.google.common.collect.ImmutableList;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

/**
 * Created by Max on 17/6/7.
 */
public interface RechargeDetailLogService {

    RechargeDetailLog save(RechargeDetailLog rechargeDetailLog);

    RechargeDetailLog findTopBySeqNo(String seqNo);

    RechargeDetailLog findById(Long rechargeId);

    List<RechargeDetailLog> log(Long userId, int pageIndex, int pageSize);

    List<RechargeDetailLog> findByUserIdAndDelAndStateInAndCreateTimeBetween(long userId, int del, ImmutableList<Integer> stateList, Date startTime, Date startTime1);

    ResponseEntity<VoViewRechargeWarpRes> pcLogs(VoPcRechargeReq rechargeReq);

    /**
     * 充值记录导出excel
     * @param rechargeReq
     * @return
     */
    List<RechargeLogs>toExcel(VoPcRechargeReq rechargeReq);


    /**
     * 查询投资记录
     * @param rechargeDetailLogSpecification
     * @return
     */
    List<RechargeDetailLog> findAll(Specification<RechargeDetailLog> rechargeDetailLogSpecification);

}
