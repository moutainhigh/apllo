package com.gofobao.framework.system.biz.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.system.biz.ThirdBatchDealLogBiz;
import com.gofobao.framework.system.contants.ThirdBatchLogContants;
import com.gofobao.framework.system.entity.ThirdBatchLog;
import com.gofobao.framework.system.service.ThirdBatchLogService;
import com.gofobao.framework.system.vo.request.VoFindLendRepayStatusListReq;
import com.gofobao.framework.system.vo.response.VoFindLendRepayStatus;
import com.gofobao.framework.system.vo.response.VoViewFindLendRepayStatusListRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Zeke on 2017/9/12.
 */
@Service
public class ThirdBatchDealLogBizImpl implements ThirdBatchDealLogBiz {

    @Autowired
    private BorrowService borrowService;
    @Autowired
    private ThirdBatchLogService thirdBatchLogService;

    /**
     * 查询放款状态集合
     *
     * @param voFindLendRepayStatusListReq
     * @return
     */
    public ResponseEntity<VoViewFindLendRepayStatusListRes> findLendRepayStatusList(VoFindLendRepayStatusListReq voFindLendRepayStatusListReq) {
        VoViewFindLendRepayStatusListRes repayStatusListRes = VoViewFindLendRepayStatusListRes.ok("查询成功!", VoViewFindLendRepayStatusListRes.class);
        /*借款id*/
        long borrowId = voFindLendRepayStatusListReq.getBorrowId();
        /*借款对象*/
        Borrow borrow = borrowService.findById(borrowId);
        /*查询批次*/
        Specification<ThirdBatchLog> tbls = Specifications
                .<ThirdBatchLog>and()
                .eq("sourceId", borrow.getId())
                .eq("type", ThirdBatchLogContants.BATCH_LEND_REPAY)
                .build();
        List<ThirdBatchLog> thirdBatchLogList = thirdBatchLogService.findList(tbls, new Sort(Sort.Direction.DESC, "createdAt"));
        /* 批次处理记录 */
        ThirdBatchLog thirdBatchLog = null;
        if (!CollectionUtils.isEmpty(thirdBatchLogList)) {
            //已完成批次
            List<ThirdBatchLog> successThirdBatchLogList = thirdBatchLogList.stream().filter(t -> t.getType().intValue() == 3).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(successThirdBatchLogList)) {
                thirdBatchLog = successThirdBatchLogList.get(1);
            } else {
                thirdBatchLog = thirdBatchLogList.get(0);
            }
        }
        //不存在已完成批次，继续获取批次处理记录 0待处理 1.未通过 2.已通过
        boolean flag = false;
        List<VoFindLendRepayStatus> voFindLendRepayStatusList = new ArrayList<>();
        //填充放款状态数据
        fillFindLendRepayStatusData(borrow, thirdBatchLog, flag, voFindLendRepayStatusList);

        //修改参数
        repayStatusListRes.setVoFindLendRepayStatusList(voFindLendRepayStatusList);
        return ResponseEntity.ok(repayStatusListRes);
    }

    /**
     * 填充放款状态数据
     *
     * @param borrow
     * @param thirdBatchLog
     * @param flag
     * @param voFindLendRepayStatusList
     */
    public void fillFindLendRepayStatusData(Borrow borrow, ThirdBatchLog thirdBatchLog, boolean flag, List<VoFindLendRepayStatus> voFindLendRepayStatusList) {
        //第一步
        VoFindLendRepayStatus voFindLendRepayStatus = new VoFindLendRepayStatus();
        voFindLendRepayStatus.setName(ThirdBatchLogContants.BORROW_FIRST_STEP);
        if (ObjectUtils.isEmpty(borrow.getVerifyAt())) { // 0待处理
            voFindLendRepayStatus.setDateStr("- -");
            voFindLendRepayStatus.setState(0);
        } else if (borrow.getStatus() == 2) { //1.未通过
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
            voFindLendRepayStatus.setState(1);
        } else if (!ObjectUtils.isEmpty(borrow.getVerifyAt())) { // 2.已通过
            flag = true;
            voFindLendRepayStatus.setState(2);
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
        }
        voFindLendRepayStatusList.add(voFindLendRepayStatus);
        //第二步
        Set<Integer> borrowType = new HashSet<Integer>() {
        };
        borrowType.add(2);
        borrowType.add(4);
        borrowType.add(5);
        borrowType.add(6);
        voFindLendRepayStatus = new VoFindLendRepayStatus();
        voFindLendRepayStatus.setName(ThirdBatchLogContants.BORROW_SECOND_STEP);
        if (flag) { // 0待处理
            voFindLendRepayStatus.setDateStr("- -");
            voFindLendRepayStatus.setState(0);
        } else if (borrowType.contains(borrow.getStatus().intValue())) { //1.未通过
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
            voFindLendRepayStatus.setState(1);
        } else if (borrow.getMoneyYes().intValue() >= borrow.getMoney().intValue() && !ObjectUtils.isEmpty(borrow.getSuccessAt())) { // 2.已通过
            flag = true;
            voFindLendRepayStatus.setState(2);
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
        }
        voFindLendRepayStatusList.add(voFindLendRepayStatus);
        //第三步
        voFindLendRepayStatus = new VoFindLendRepayStatus();
        voFindLendRepayStatus.setName(ThirdBatchLogContants.BORROW_THIRD_STEP);
        if (flag && ObjectUtils.isEmpty(thirdBatchLog)) { // 0待处理
            voFindLendRepayStatus.setDateStr("- -");
            voFindLendRepayStatus.setState(0);
        } else if (thirdBatchLog.getState() == 2 || thirdBatchLog.getState() == 4) { //1.未通过
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
            voFindLendRepayStatus.setState(1);
        } else { // 2.已通过
            flag = true;
            voFindLendRepayStatus.setState(2);
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(thirdBatchLog.getUpdateAt()));
        }
        voFindLendRepayStatusList.add(voFindLendRepayStatus);
        //第四步
        voFindLendRepayStatus = new VoFindLendRepayStatus();
        voFindLendRepayStatus.setName(ThirdBatchLogContants.BORROW_FOUR_STEP);
        if (flag && thirdBatchLog.getState() != 3) { // 0待处理
            voFindLendRepayStatus.setDateStr("- -");
            voFindLendRepayStatus.setState(0);
        } else if (thirdBatchLog.getState() == 4 || thirdBatchLog.getState() == 2) { //1.未通过
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
            voFindLendRepayStatus.setState(1);
        } else { // 2.已通过
            flag = true;
            voFindLendRepayStatus.setState(2);
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(thirdBatchLog.getUpdateAt()));
        }
        voFindLendRepayStatusList.add(voFindLendRepayStatus);
        //第五步
        voFindLendRepayStatus = new VoFindLendRepayStatus();
        voFindLendRepayStatus.setName(ThirdBatchLogContants.BORROW_FIVE_STEP);
        if (flag && thirdBatchLog.getState() != 3) { // 0待处理
            voFindLendRepayStatus.setDateStr("- -");
            voFindLendRepayStatus.setState(0);
        } else if (thirdBatchLog.getState() == 2 || thirdBatchLog.getState() == 4) { //1.未通过
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(borrow.getVerifyAt()));
            voFindLendRepayStatus.setState(1);
        } else { // 2.已通过
            voFindLendRepayStatus.setState(2);
            voFindLendRepayStatus.setDateStr(DateHelper.dateToString(thirdBatchLog.getUpdateAt()));
        }
        voFindLendRepayStatusList.add(voFindLendRepayStatus);
    }
}

