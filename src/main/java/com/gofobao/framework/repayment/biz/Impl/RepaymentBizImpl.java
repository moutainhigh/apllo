package com.gofobao.framework.repayment.biz.Impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.api.contants.ChannelContant;
import com.gofobao.framework.api.contants.JixinResultContants;
import com.gofobao.framework.api.helper.JixinManager;
import com.gofobao.framework.api.helper.JixinTxCodeEnum;
import com.gofobao.framework.api.model.balance_freeze.BalanceFreezeReq;
import com.gofobao.framework.api.model.balance_freeze.BalanceFreezeResp;
import com.gofobao.framework.api.model.balance_query.BalanceQueryRequest;
import com.gofobao.framework.api.model.balance_query.BalanceQueryResponse;
import com.gofobao.framework.api.model.balance_un_freeze.BalanceUnfreezeReq;
import com.gofobao.framework.api.model.balance_un_freeze.BalanceUnfreezeResp;
import com.gofobao.framework.api.model.batch_credit_end.BatchCreditEndResp;
import com.gofobao.framework.api.model.batch_credit_invest.BatchCreditInvestReq;
import com.gofobao.framework.api.model.batch_credit_invest.CreditInvest;
import com.gofobao.framework.api.model.batch_repay.BatchRepayReq;
import com.gofobao.framework.api.model.batch_repay.BatchRepayResp;
import com.gofobao.framework.api.model.batch_repay.Repay;
import com.gofobao.framework.asset.contants.BatchAssetChangeContants;
import com.gofobao.framework.asset.entity.AdvanceLog;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.entity.BatchAssetChange;
import com.gofobao.framework.asset.entity.BatchAssetChangeItem;
import com.gofobao.framework.asset.service.AdvanceLogService;
import com.gofobao.framework.asset.service.AssetService;
import com.gofobao.framework.asset.service.BatchAssetChangeItemService;
import com.gofobao.framework.asset.service.BatchAssetChangeService;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.repository.BorrowRepository;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.borrow.vo.request.VoRepayAllReq;
import com.gofobao.framework.collection.entity.BorrowCollection;
import com.gofobao.framework.collection.service.BorrowCollectionService;
import com.gofobao.framework.collection.vo.request.VoCollectionListReq;
import com.gofobao.framework.collection.vo.request.VoCollectionOrderReq;
import com.gofobao.framework.collection.vo.response.VoViewCollectionDaysWarpRes;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderListWarpResp;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderRes;
import com.gofobao.framework.common.assets.AssetChange;
import com.gofobao.framework.common.assets.AssetChangeProvider;
import com.gofobao.framework.common.assets.AssetChangeTypeEnum;
import com.gofobao.framework.common.constans.TypeTokenContants;
import com.gofobao.framework.common.data.DataObject;
import com.gofobao.framework.common.data.LtSpecification;
import com.gofobao.framework.common.integral.IntegralChangeEntity;
import com.gofobao.framework.common.integral.IntegralChangeEnum;
import com.gofobao.framework.common.jxl.ExcelException;
import com.gofobao.framework.common.jxl.ExcelUtil;
import com.gofobao.framework.common.rabbitmq.MqConfig;
import com.gofobao.framework.common.rabbitmq.MqHelper;
import com.gofobao.framework.common.rabbitmq.MqQueueEnum;
import com.gofobao.framework.common.rabbitmq.MqTagEnum;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.*;
import com.gofobao.framework.helper.project.BatchAssetChangeHelper;
import com.gofobao.framework.helper.project.IntegralChangeHelper;
import com.gofobao.framework.helper.project.SecurityHelper;
import com.gofobao.framework.member.entity.UserCache;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.service.UserCacheService;
import com.gofobao.framework.member.service.UserService;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.repayment.biz.RepaymentBiz;
import com.gofobao.framework.repayment.contants.RepaymentContants;
import com.gofobao.framework.repayment.entity.AdvanceAssetChange;
import com.gofobao.framework.repayment.entity.BorrowRepayment;
import com.gofobao.framework.repayment.entity.RepayAssetChange;
import com.gofobao.framework.repayment.service.BorrowRepaymentService;
import com.gofobao.framework.repayment.vo.request.*;
import com.gofobao.framework.repayment.vo.response.RepayCollectionLog;
import com.gofobao.framework.repayment.vo.response.RepaymentOrderDetail;
import com.gofobao.framework.repayment.vo.response.VoViewRepayCollectionLogWarpRes;
import com.gofobao.framework.repayment.vo.response.VoViewRepaymentOrderDetailWarpRes;
import com.gofobao.framework.repayment.vo.response.pc.VoCollection;
import com.gofobao.framework.repayment.vo.response.pc.VoOrdersList;
import com.gofobao.framework.repayment.vo.response.pc.VoViewCollectionWarpRes;
import com.gofobao.framework.repayment.vo.response.pc.VoViewOrderListWarpRes;
import com.gofobao.framework.system.biz.StatisticBiz;
import com.gofobao.framework.system.biz.ThirdBatchLogBiz;
import com.gofobao.framework.system.contants.ThirdBatchLogContants;
import com.gofobao.framework.system.entity.Notices;
import com.gofobao.framework.system.entity.Statistic;
import com.gofobao.framework.system.entity.ThirdBatchLog;
import com.gofobao.framework.system.service.DictItemService;
import com.gofobao.framework.system.service.DictValueService;
import com.gofobao.framework.system.service.ThirdBatchLogService;
import com.gofobao.framework.tender.biz.TransferBiz;
import com.gofobao.framework.tender.contants.BorrowContants;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.entity.Transfer;
import com.gofobao.framework.tender.entity.TransferBuyLog;
import com.gofobao.framework.tender.service.TenderService;
import com.gofobao.framework.tender.service.TransferBuyLogService;
import com.gofobao.framework.tender.service.TransferService;
import com.gofobao.framework.windmill.borrow.biz.WindmillTenderBiz;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gofobao.framework.helper.DateHelper.isBetween;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * Created by admin on 2017/6/6.
 */
@Service
@Slf4j
public class RepaymentBizImpl implements RepaymentBiz {
    final Gson GSON = new GsonBuilder().create();

    @Autowired
    private BorrowService borrowService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private StatisticBiz statisticBiz;
    @Autowired
    private TenderService tenderService;
    @Autowired
    private UserCacheService userCacheService;
    @Autowired
    private BorrowCollectionService borrowCollectionService;
    @Autowired
    private IntegralChangeHelper integralChangeHelper;
    @Autowired
    private BorrowRepaymentService borrowRepaymentService;
    @Autowired
    private AdvanceLogService advanceLogService;
    @Autowired
    private BorrowRepository borrowRepository;
    @Autowired
    private DictItemService dictItemService;
    @Autowired
    private ThirdBatchLogService thirdBatchLogService;
    @Autowired
    private ThirdBatchLogBiz thirdBatchLogBiz;
    @Autowired
    private JixinHelper jixinHelper;
    @Autowired
    private DictValueService dictValueService;
    @Autowired
    private BatchAssetChangeHelper batchAssetChangeHelper;
    @Autowired
    private BatchAssetChangeItemService batchAssetChangeItemService;
    @Autowired
    private AssetChangeProvider assetChangeProvider;
    @Autowired
    private TransferService transferService;
    @Autowired
    private UserService userService;
    @Autowired
    private MqHelper mqHelper;
    @Autowired
    private BatchAssetChangeService batchAssetChangeService;
    @Autowired
    private TransferBuyLogService transferBuyLogService;
    @Autowired
    private TransferBiz transferBiz;
    @Autowired
    private WindmillTenderBiz windmillTenderBiz;

    @Value("${gofobao.javaDomain}")
    private String javaDomain;
    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private JixinManager jixinManager;

    /**
     * pc还款
     *
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcInstantly(VoPcRepay voPcRepay) throws Exception {
        String paramStr = voPcRepay.getParamStr();/* pc还款 */
        if (!SecurityHelper.checkSign(voPcRepay.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc取消借款 签名验证不通过!"));
        }
        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        /* 借款id */
        long repaymentId = NumberHelper.toLong(paramMap.get("repaymentId"));
        /* 是否是垫付 */
        boolean isAdvance = BooleanUtils.toBoolean(paramMap.get("isAdvance"));
        /* 还款记录 */
        BorrowRepayment borrowRepayment = borrowRepaymentService.findById(repaymentId);
        Preconditions.checkNotNull(borrowRepayment, "还款不存在!");
        /* 名义借款人id */
        UserThirdAccount titularBorrowAccount = jixinHelper.getTitularBorrowAccount(borrowRepayment.getBorrowId());

        VoRepayReq voRepayReq = new VoRepayReq();
        voRepayReq.setRepaymentId(repaymentId);
        if (isAdvance) {
            voRepayReq.setUserId(titularBorrowAccount.getUserId());
        } else {
            voRepayReq.setUserId(borrowRepayment.getUserId());
        }
        voRepayReq.setInterestPercent(1d);
        voRepayReq.setIsUserOpen(true);
        return newRepay(voRepayReq);
    }

    /**
     * pc提前结清
     *
     * @param voRepayAllReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcRepayAll(VoRepayAllReq voRepayAllReq) throws Exception {
        String paramStr = voRepayAllReq.getParamStr();/* pc请求提前结清参数 */
        if (!SecurityHelper.checkSign(voRepayAllReq.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc取消借款 签名验证不通过!"));
        }
        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        /* 借款id */
        long borrowId = NumberHelper.toLong(paramMap.get("borrowId"));

        //提前结清操作
        return repayAll(borrowId);
    }

    /**
     * 提前结清处理
     *
     * @param borrowId
     */
    public ResponseEntity<VoBaseResp> repayAllDeal(long borrowId, String batchNo) throws Exception {
        //1.判断借款状态，
        Borrow borrow = borrowService.findByIdLock(borrowId);/* 提前结清操作的借款记录 */
        Preconditions.checkNotNull(borrow, "借款记录不存在!");
        //2.查询提前结清需要回款记录
        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowId)
                .eq("status", 0)
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
        Preconditions.checkNotNull(borrowRepaymentList, "还款记录(提前结清)不存在！");
        //2.1/* 还款对应的投标记录  包括债权转让在里面 */
        Specification<Tender> ts = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(ts);/* 还款对应的投标记录  包括债权转让在里面 */
        Preconditions.checkNotNull(tenderList, "立即还款: 投标记录为空!");
        /* 投标记录id集合 */
        Set<Long> tenderIds = tenderList.stream().map(tender -> tender.getId()).collect(Collectors.toSet());
        //迭代还款集合,逐期还款
        for (BorrowRepayment borrowRepayment : borrowRepaymentList) {
            /* 是否垫付 */
            boolean advance = !ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes());
            /* 查询未转让的投标记录回款记录 */
            Specification<BorrowCollection> bcs = Specifications
                    .<BorrowCollection>and()
                    .in("tenderId", tenderIds.toArray())
                    .eq("status", 0)
                    .eq("order", borrowRepayment.getOrder())
                    .eq("transferFlag", 0)
                    .build();
            List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
            Preconditions.checkNotNull(borrowCollectionList, "立即还款: 回款记录为空!");

            //4.还款成功后变更改还款状态
            changeRepaymentAndRepayStatus(borrow, tenderList, borrowRepayment, borrowCollectionList, advance);
            //5.结束第三方债权并更新借款状态（还款最后一期的时候）
            endThirdTenderAndChangeBorrowStatus(borrow, borrowRepayment);
            //6.发送投资人收到还款站内信
            sendCollectionNotices(borrowCollectionList, advance, borrow);
            //7.发放积分
            giveInterest(borrowCollectionList, borrow);
            //8.还款最后新增统计
            updateRepaymentStatistics(borrow, borrowRepayment);
            //9.更新投资人缓存
            updateUserCacheByReceivedRepay(borrowCollectionList, borrow);
            //10.项目回款短信通知
            smsNoticeByReceivedRepay(borrowCollectionList, borrow, borrowRepayment);
        }
        //2.进行批次资产改变
        //2.处理资金还款人、收款人资金变动
        batchAssetChangeHelper.batchAssetChangeAndCollection(borrowId, batchNo, BatchAssetChangeContants.BATCH_REPAY_ALL);
        return ResponseEntity.ok(VoBaseResp.ok("提前结清处理成功!"));
    }

    /**
     * 检查提前结清参数
     *
     * @param borrow
     * @return
     */
    public ResponseEntity<VoBaseResp> checkRepayAll(Borrow borrow) {
        long borrowId = borrow.getId();
        if ((borrow.getStatus() != 3) || (borrow.getType() != 0 && borrow.getType() != 4)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款状态非可结清状态！"));
        }

        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowId)
                .eq("status", 0)
                .build();
        if (borrowRepaymentService.count(brs) < 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "该借款剩余未还期数小于1期！"));
        }
        return ResponseEntity.ok(VoBaseResp.ok("校验成功!"));
    }


    /**
     * 提前结清操作
     *
     * @param borrowId
     * @throws Exception
     */
    public ResponseEntity<VoBaseResp> repayAll(long borrowId) throws Exception {
        Borrow borrow = borrowService.findByIdLock(borrowId);/* 借款记录 */
        Preconditions.checkNotNull(borrow, "借款记录不存在!");
        /* 名义借款人id */
        UserThirdAccount titularBorrowAccount = jixinHelper.getTitularBorrowAccount(borrow.getId());
        ResponseEntity<VoBaseResp> resp = ThirdAccountHelper.allConditionCheck(titularBorrowAccount);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            return resp;
        }
        Asset titularBorrowAsset = assetService.findByUserId(titularBorrowAccount.getUserId());/* 名义借款人资产账户 */
        Preconditions.checkNotNull(titularBorrowAsset, "名义借款人资产记录不存在!");
        //检查提前结清
        resp = checkRepayAll(borrow);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            return resp;
        }
        /* 获取批次记录 */
        ThirdBatchLog thirdBatchLog = thirdBatchLogBiz.getValidLastBatchLog(String.valueOf(borrowId), ThirdBatchLogContants.BATCH_REPAY_ALL);
        //判断提交还款批次是否多次重复提交
        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(borrowId), ThirdBatchLogContants.BATCH_REPAY_ALL);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款处理中，请勿重复点击!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            //结束债权调用处理
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_THIRD_BATCH);
            mqConfig.setTag(MqTagEnum.BATCH_DEAL);
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.SOURCE_ID, StringHelper.toString(thirdBatchLog.getSourceId()),
                            MqConfig.BATCH_NO, StringHelper.toString(thirdBatchLog.getBatchNo()),
                            MqConfig.MSG_TIME, DateHelper.dateToString(new Date())
                    );

            mqConfig.setMsg(body);
            try {
                log.info(String.format("borrowThirdBizImpl repayAll send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("borrowThirdBizImpl repayAll send mq exception", e);
            }
        }
        /* 有效未还的还款记录 */
        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowId)
                .eq("status", 0)
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
        /* 还款请求集合 */
        List<VoBuildThirdRepayReq> voBuildThirdRepayReqs = new ArrayList<>();
        //构建还款请求集合
        ImmutableList<Long> resultSet = buildRepayReqList(voBuildThirdRepayReqs, borrow);
        Iterator<Long> iterator = resultSet.iterator();
        long penalty = iterator.next();/* 违约金 */
        long repayMoney = iterator.next();/* 提前结清需还总金额 */
        if (titularBorrowAsset.getUseMoney() < (repayMoney)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "结清总共需要还款 " + repayMoney + " 元，您的账户余额不足，请先充值!！"));
        }

        //判断是否有在即信处理的债权转让，有不让还款
        if (checkTenderChange(borrowId)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "这笔债权转让正在操作中，请等待处理完成后在进行操作！"));
        }

        /* 批次号 */
        String batchNo = jixinHelper.getBatchNo();
        /* 资产记录分组流水号 */
        String groupSeqNo = assetChangeProvider.getGroupSeqNo();
        //生成批次资产改变主记录
        BatchAssetChange batchAssetChange = addBatchAssetChangeByRepayAll(batchNo, borrowId);
        //提前结清处理
        //1.生成批次资产变动记录
        //2.发送至存管系统进行备案
        return repayAllProcess(borrowId, borrow, titularBorrowAccount, voBuildThirdRepayReqs, batchNo, groupSeqNo, penalty, batchAssetChange, borrowRepaymentList);
    }


    /**
     * 构建还款请求集合
     *
     * @param voBuildThirdRepayReqs 还款请求
     */
    private ImmutableList<Long> buildRepayReqList(List<VoBuildThirdRepayReq> voBuildThirdRepayReqs, Borrow borrow) {
                /* 有效未还的还款记录 */
        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrow.getId())
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
        long repaymentTotal = 0;/* 还款总金额 */
        long penalty = 0;/* 违约金 */
        long repayMoney = 0;/* 还款总金额+违约金 */
        for (int i = 0; i < borrowRepaymentList.size(); i++) {
            BorrowRepayment borrowRepayment = borrowRepaymentList.get(i);
            if (borrowRepayment.getStatus() != 0) {
                continue;
            }
            /* 开始时间 */
            Date startAt;
            if (borrowRepayment.getOrder() == 0) {
                startAt = DateHelper.beginOfDate(borrow.getSuccessAt());
            } else {
                startAt = DateHelper.beginOfDate(borrowRepaymentList.get((i - 1)).getRepayAt());
            }
            /* 结束时间 */
            Date endAt = DateHelper.beginOfDate(borrowRepayment.getRepayAt());
            int days = DateHelper.diffInDays(endAt, startAt, false);

            //以结清第一期的14天利息作为违约金
            if (penalty == 0) { // 违约金
                penalty = borrowRepayment.getInterest() / days * 7;
            }

            Date nowStartDate = DateHelper.beginOfDate(new Date());  // 现在的凌晨时间
            double interestPercent;/* 利息百分比 */
            if (nowStartDate.getTime() <= startAt.getTime()) {
                interestPercent = 0;
            } else {
                interestPercent = MathHelper.min(DateHelper.diffInDays(nowStartDate, startAt, false) / DateHelper.diffInDays(endAt, startAt, false), 1);
            }
            /* 逾期天数 */
            int lateDays = DateHelper.diffInDays(nowStartDate, endAt, false);
            /* 逾期利息 */
            long lateInterest = calculateLateInterest(lateDays, borrowRepayment, borrow);
            //累加金额用于判断还款账余额是否充足
            repaymentTotal += borrowRepayment.getPrincipal() + borrowRepayment.getInterest() * interestPercent + lateInterest;
            /* 还款请求 */
            VoBuildThirdRepayReq voBuildThirdRepayReq = new VoBuildThirdRepayReq();
            voBuildThirdRepayReq.setInterestPercent(interestPercent);   // 赔偿利息
            voBuildThirdRepayReq.setRepaymentId(borrowRepayment.getId());
            voBuildThirdRepayReq.setUserId(borrowRepayment.getUserId());
            voBuildThirdRepayReq.setIsUserOpen(false);
            voBuildThirdRepayReq.setLateDays(lateDays);
            voBuildThirdRepayReqs.add(voBuildThirdRepayReq);
        }
        repayMoney = repaymentTotal + penalty;/* 提前结清需还总金额 */
        return ImmutableList.of(penalty, repayMoney);
    }

    /**
     * 提前结清操作
     *
     * @param borrowId
     * @param borrow
     * @param titularBorrowAccount
     * @param voBuildThirdRepayReqs
     * @param groupSeqNo
     * @param batchAssetChange
     * @param borrowRepaymentList
     * @throws Exception
     */
    private ResponseEntity<VoBaseResp> repayAllProcess(long borrowId, Borrow borrow, UserThirdAccount titularBorrowAccount, List<VoBuildThirdRepayReq> voBuildThirdRepayReqs, String batchNo,
                                                       String groupSeqNo, long penalty, BatchAssetChange batchAssetChange,
                                                       List<BorrowRepayment> borrowRepaymentList) throws Exception {
        Date nowDate = new Date();
        /* 投资记录：不包含理财计划 */
        Map<Long/* repaymentId */, BorrowRepayment> borrowRepaymentMaps = borrowRepaymentList.stream().collect(Collectors.toMap(BorrowRepayment::getId, Function.identity()));
        Specification<Tender> specification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .notIn("transferFlag", 2)
                .eq("borrowId", borrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(specification);
        Preconditions.checkState(!CollectionUtils.isEmpty(tenderList), "投资记录不存在!");
            /* 投资id集合 */
        List<Long> tenderIds = tenderList.stream().map(p -> p.getId()).collect(Collectors.toList());
        /* 生成存管还款记录(提前结清) */
        List<Repay> repays = new ArrayList<>();
        for (VoBuildThirdRepayReq voBuildThirdRepayReq : voBuildThirdRepayReqs) {
            BorrowRepayment borrowRepayment = borrowRepaymentMaps.get(voBuildThirdRepayReq.getRepaymentId());/* 还款记录 */
            /* 投资人回款记录 */
            Specification<BorrowCollection> bcs = Specifications
                    .<BorrowCollection>and()
                    .in("tenderId", tenderIds.toArray())
                    .eq("status", 0)
                    .eq("transferFlag", 0)
                    .eq("order", borrowRepayment.getOrder())
                    .build();
            List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
            Preconditions.checkNotNull(borrowCollectionList, "生成即信还款计划: 获取回款计划列表为空!");
            List<RepayAssetChange> repayAssetChangeList = new ArrayList<>();
            int lateDays = getLateDays(borrowRepayment);
            // 生成存管投资人还款记录(提前结清)
            List<Repay> tempRepays = calculateRepayPlan(borrow,
                    titularBorrowAccount.getAccountId(),
                    tenderList,
                    borrowCollectionList,
                    lateDays,
                    borrowRepayment,
                    voBuildThirdRepayReq.getInterestPercent(),
                    repayAssetChangeList
            );
            repays.addAll(tempRepays);
            // 生成回款记录 返回值  实际本金和利息
            long repayMoney = doGenerateAssetChangeRecodeByRepay(borrow, borrowRepayment, borrowRepayment.getUserId(), repayAssetChangeList, groupSeqNo, batchAssetChange, false);
            //真实的逾期费用
            /*平台实际收取的逾期费用*/
            long realPlatformOverdueFee = repayAssetChangeList.stream().mapToLong(RepayAssetChange::getPlatformOverdueFee).sum();
            /*投资人实际收取的逾期费用*/
            long realOverdueFee = repayAssetChangeList.stream().mapToLong(RepayAssetChange::getOverdueFee).sum();
            long realLateInterest = realOverdueFee + realPlatformOverdueFee;
            // 是否是垫付
            boolean advance = !ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes());
            // 生成还款人还款批次资金改变记录
            addBatchAssetChangeByBorrower(batchAssetChange.getId(), borrowRepayment, borrow,
                    voBuildThirdRepayReq.getInterestPercent(), voBuildThirdRepayReq.getIsUserOpen(),
                    realLateInterest, titularBorrowAccount.getUserId(), groupSeqNo,
                    advance, repayMoney);
            //更新还款记录
            borrowRepayment.setUpdatedAt(nowDate);
            borrowRepayment.setRepayMoneyYes(repayMoney);
            borrowRepayment.setLateInterest(realLateInterest);
        }
        borrowRepaymentService.save(borrowRepaymentList);
        /* 总还款本金 */
        double sumTxAmount = repays.stream().mapToDouble(repay -> NumberHelper.toDouble(repay.getTxAmount())).sum();
        //========================处理提前结清违约金开始============================
        dealRepayAllPenalty(borrowId, borrow, titularBorrowAccount, groupSeqNo, penalty, batchAssetChange, nowDate, repays, sumTxAmount);
        //========================处理提前结清违约金结束============================
        //所有交易利息
        double intAmount = repays.stream().mapToDouble(r -> NumberHelper.toDouble(r.getIntAmount())).sum();
        //所有还款手续费
        double txFeeOut = MoneyHelper.round(repays.stream().mapToDouble(r -> NumberHelper.toDouble(r.getTxFeeOut())).sum(), 2);
        //冻结金额
        double freezeMoney = sumTxAmount + intAmount + txFeeOut;
        /* 需要冻结资金 */
        long localFreezeMoney = new Double((freezeMoney) * 100).longValue();
        //====================================================================
        //冻结借款人账户资金
        //====================================================================
        String freezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(titularBorrowAccount.getAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(freezeMoney, false));
        balanceFreezeReq.setOrderId(freezeOrderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            throw new Exception("即信批次还款冻结资金失败：" + balanceFreezeResp.getRetMsg());
        }
        try {

            //请求保留参数
            Map<String, Object> acqResMap = new HashMap<>();
            acqResMap.put("borrowId", borrowId);
            acqResMap.put("freezeMoney", freezeMoney);
            acqResMap.put("freezeOrderId", freezeOrderId);
            acqResMap.put("userId", titularBorrowAccount.getUserId());

            //立即还款冻结可用资金
            AssetChange assetChange = new AssetChange();
            assetChange.setType(AssetChangeTypeEnum.freeze);  // 立即还款冻结可用资金
            assetChange.setUserId(titularBorrowAccount.getUserId());
            assetChange.setMoney(localFreezeMoney);
            assetChange.setRemark("立即还款冻结可用资金");
            assetChange.setSourceId(borrow.getId());
            assetChange.setSeqNo(assetChangeProvider.getSeqNo());
            assetChange.setGroupSeqNo(groupSeqNo);
            assetChangeProvider.commonAssetChange(assetChange);

            BatchRepayReq request = new BatchRepayReq();
            request.setBatchNo(batchNo);
            request.setTxAmount(StringHelper.formatDouble(sumTxAmount, false));
            request.setRetNotifyURL(javaDomain + "/pub/borrow/v2/third/repayall/run");
            request.setNotifyURL(javaDomain + "/pub/borrow/v2/third/repayall/check");
            request.setAcqRes(GSON.toJson(acqResMap));
            request.setSubPacks(GSON.toJson(repays));
            request.setChannel(ChannelContant.HTML);
            request.setTxCounts(StringHelper.toString(repays.size()));
            BatchRepayResp response = jixinManager.send(JixinTxCodeEnum.BATCH_REPAY, request, BatchRepayResp.class);
            if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
                throw new Exception("即信批次还款失败：" + response.getRetMsg());
            }

            //记录日志
            ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
            thirdBatchLog.setBatchNo(batchNo);
            thirdBatchLog.setCreateAt(nowDate);
            thirdBatchLog.setUpdateAt(nowDate);
            thirdBatchLog.setSourceId(borrowId);
            thirdBatchLog.setType(ThirdBatchLogContants.BATCH_REPAY_ALL);
            thirdBatchLog.setAcqRes(GSON.toJson(acqResMap));
            thirdBatchLog.setRemark("(提前结清)即信批次还款");
            thirdBatchLogService.save(thirdBatchLog);
        } catch (Exception e) {
            // 申请即信还款解冻
            String unfreezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_UNFREEZE_PREFIX);
            BalanceUnfreezeReq balanceUnfreezeReq = new BalanceUnfreezeReq();
            balanceUnfreezeReq.setAccountId(titularBorrowAccount.getAccountId());
            balanceUnfreezeReq.setTxAmount(StringHelper.formatDouble(freezeMoney, false));
            balanceUnfreezeReq.setOrderId(unfreezeOrderId);
            balanceUnfreezeReq.setOrgOrderId(freezeOrderId);
            balanceUnfreezeReq.setChannel(ChannelContant.HTML);
            BalanceUnfreezeResp balanceUnfreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_UN_FREEZE, balanceUnfreezeReq, BalanceUnfreezeResp.class);
            if ((ObjectUtils.isEmpty(balanceUnfreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceUnfreezeResp.getRetCode()))) {
                throw new Exception("提前结清解冻异常：" + balanceUnfreezeResp.getRetMsg());
            }
            throw new Exception(e);
        }
        return ResponseEntity.ok(VoBaseResp.ok("提前结清成功!"));
    }

    /**
     * 处理提前结清违约金开始
     *
     * @param borrowId
     * @param borrow
     * @param titularBorrowAccount
     * @param groupSeqNo
     * @param penalty
     * @param batchAssetChange
     * @param nowDate
     * @param repays
     * @param sumTxAmount
     */
    private void dealRepayAllPenalty(long borrowId, Borrow borrow, UserThirdAccount titularBorrowAccount, String groupSeqNo, long penalty, BatchAssetChange batchAssetChange, Date nowDate, List<Repay> repays, double sumTxAmount) {
        //扣除提前结清的违约金
        if (penalty > 0) {
            /* 实际收取总违约金 */
            double sumPenalty = 0;
            for (Repay repay : repays) {
                double partPenalty = MoneyHelper.round(NumberHelper.toDouble(repay.getTxAmount()) / sumTxAmount * penalty, 0);/*分摊违约金*/
                sumPenalty += partPenalty;

                UserThirdAccount userThirdAccount = userThirdAccountService.findByAccountId(repay.getForAccountId());
                BatchAssetChangeItem batchAssetChangeItem = new BatchAssetChangeItem();
                batchAssetChangeItem.setBatchAssetChangeId(batchAssetChange.getId());
                batchAssetChangeItem.setState(0);
                batchAssetChangeItem.setType(AssetChangeTypeEnum.receivedPaymentsViolation.getLocalType());  //  收到提前结清的违约金
                batchAssetChangeItem.setUserId(userThirdAccount.getUserId());
                batchAssetChangeItem.setMoney(NumberHelper.toLong(partPenalty));
                batchAssetChangeItem.setRemark(String.format("收到[%s]提前结清的违约金", borrow.getName()));
                batchAssetChangeItem.setCreatedAt(nowDate);
                batchAssetChangeItem.setUpdatedAt(nowDate);
                batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
                batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
                batchAssetChangeItemService.save(batchAssetChangeItem);
                //给每期回款分摊违约金
                repay.setIntAmount(StringHelper.formatDouble(MoneyHelper.round(NumberHelper.toDouble(repay.getIntAmount()) + MoneyHelper.divide(partPenalty, 100d), 0), false));
                repay.setIntAmount(StringHelper.formatDouble(MoneyHelper.round(NumberHelper.toDouble(repay.getIntAmount()) + MoneyHelper.divide(partPenalty, 100d), 0), false));
                repay.setTxFeeOut(StringHelper.formatDouble((MoneyHelper.round(NumberHelper.toDouble(repay.getTxFeeOut()) + MoneyHelper.divide(partPenalty, 100.d), 0)), false));
            }
            //收取借款人违约金
            BatchAssetChangeItem batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChange.getId());
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.repayPaymentsViolation.getLocalType());  // 扣除借款人违约金
            batchAssetChangeItem.setUserId(titularBorrowAccount.getUserId());
            batchAssetChangeItem.setMoney(NumberHelper.toLong(sumPenalty));
            batchAssetChangeItem.setRemark("扣除提前结清的违约金");
            batchAssetChangeItem.setCreatedAt(new Date());
            batchAssetChangeItem.setUpdatedAt(new Date());
            batchAssetChangeItem.setSourceId(borrowId);
            batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);
        }
    }


    /**
     * 新增资产更改记录
     *
     * @param batchNo
     * @param borrowId
     * @return
     */
    private BatchAssetChange addBatchAssetChangeByRepayAll(String batchNo, long borrowId) {
        BatchAssetChange batchAssetChange = new BatchAssetChange();
        batchAssetChange.setSourceId(borrowId);
        batchAssetChange.setState(0);
        batchAssetChange.setType(BatchAssetChangeContants.BATCH_REPAY_ALL);/* 提前结清 */
        batchAssetChange.setCreatedAt(new Date());
        batchAssetChange.setUpdatedAt(new Date());
        batchAssetChange.setBatchNo(batchNo);
        batchAssetChangeService.save(batchAssetChange);
        return batchAssetChange;
    }

    @Override
    public ResponseEntity<VoViewCollectionDaysWarpRes> days(Long userId, String time) {
        VoViewCollectionDaysWarpRes collectionDayWarpRes = VoBaseResp.ok("查询成功", VoViewCollectionDaysWarpRes.class);
        try {
            List<Integer> result = borrowRepaymentService.days(userId, time);
            collectionDayWarpRes.setWarpRes(result);
            return ResponseEntity.ok(collectionDayWarpRes);
        } catch (Throwable e) {
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewCollectionDaysWarpRes.class));

        }
    }

    /**
     * 还款计划
     *
     * @param voCollectionOrderReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewCollectionOrderListWarpResp> repaymentList(VoCollectionOrderReq voCollectionOrderReq) {
        try {
            List<BorrowRepayment> repaymentList = borrowRepaymentService.repaymentList(voCollectionOrderReq);
            if (CollectionUtils.isEmpty(repaymentList)) {
                VoViewCollectionOrderListWarpResp response = VoBaseResp.ok("查询成功", VoViewCollectionOrderListWarpResp.class);
                response.setOrder(0);
                response.setSumCollectionMoneyYes("0");
                return ResponseEntity.ok(response);
            }

            Set<Long> borrowIdSet = repaymentList.stream()
                    .map(p -> p.getBorrowId())
                    .collect(Collectors.toSet());

            List<Borrow> borrowList = borrowRepository.findByIdIn(new ArrayList(borrowIdSet));
            Map<Long, Borrow> borrowMap = borrowList.stream()
                    .collect(Collectors.toMap(Borrow::getId, Function.identity()));

            List<VoViewCollectionOrderListWarpResp> orderListRes = new ArrayList<>(0);
            List<VoViewCollectionOrderRes> orderResList = new ArrayList<>();

            repaymentList.stream().forEach(p -> {
                VoViewCollectionOrderRes collectionOrderRes = new VoViewCollectionOrderRes();
                Borrow borrow = borrowMap.get(p.getBorrowId());
                collectionOrderRes.setCollectionId(p.getId());
                collectionOrderRes.setBorrowName(borrow.getName());
                collectionOrderRes.setOrder(p.getOrder() + 1);
                if (p.getStatus().intValue() == RepaymentContants.STATUS_YES) {
                    collectionOrderRes.setCollectionMoneyYes(StringHelper.formatMon(p.getRepayMoneyYes() / 100d));
                }
                collectionOrderRes.setStatus(p.getStatus());
                collectionOrderRes.setCollectionMoney(StringHelper.formatMon(p.getRepayMoney() / 100d));
                collectionOrderRes.setTimeLime(borrow.getRepayFashion() == BorrowContants.REPAY_FASHION_YCBX_NUM ? 1 : borrow.getTimeLimit());
                orderResList.add(collectionOrderRes);
            });

            VoViewCollectionOrderListWarpResp collectionOrder = VoBaseResp.ok("查询成功", VoViewCollectionOrderListWarpResp.class);
            collectionOrder.setOrderResList(orderResList);
            //总数
            collectionOrder.setOrder(orderResList.size());
            //已还款
            long moneyYesSum = repaymentList.stream()
                    .filter(p -> p.getStatus() == RepaymentContants.STATUS_YES)
                    .mapToLong(w -> w.getRepayMoneyYes())
                    .sum();
            collectionOrder.setSumCollectionMoneyYes(StringHelper.formatMon(moneyYesSum / 100d));
            orderListRes.add(collectionOrder);
            return ResponseEntity.ok(collectionOrder);

        } catch (Throwable e) {
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewCollectionOrderListWarpResp.class));
        }
    }

    /**
     * pc:还款计划
     *
     * @param listReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewOrderListWarpRes> pcRepaymentList(VoOrderListReq listReq) {
        try {
            VoViewOrderListWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewOrderListWarpRes.class);
            Map<String, Object> resultMaps = borrowRepaymentService.pcOrderList(listReq);
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            List<VoOrdersList> orderList = (List<VoOrdersList>) resultMaps.get("orderList");
            warpRes.setTotalCount(totalCount);
            warpRes.setOrdersLists(orderList);
            return ResponseEntity.ok(warpRes);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewOrderListWarpRes.class));
        }
    }

    @Override
    public void toExcel(HttpServletResponse response, VoOrderListReq listReq) {

        List<VoOrdersList> ordersLists = borrowRepaymentService.toExcel(listReq);
        if (!CollectionUtils.isEmpty(ordersLists)) {
            LinkedHashMap<String, String> paramMaps = Maps.newLinkedHashMap();
            paramMaps.put("time", "时间");
            paramMaps.put("collectionMoney", "本息");
            paramMaps.put("principal", "本金");
            paramMaps.put("interest", "利息");
            paramMaps.put("orderCount", "笔数");
            try {
                ExcelUtil.listToExcel(ordersLists, paramMaps, "还款计划", response);
            } catch (ExcelException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 还款详情
     *
     * @param voInfoReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewRepaymentOrderDetailWarpRes> detail(VoInfoReq voInfoReq) {
        try {
            RepaymentOrderDetail voViewOrderDetailResp = borrowRepaymentService.detail(voInfoReq);
            VoViewRepaymentOrderDetailWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewRepaymentOrderDetailWarpRes.class);
            warpRes.setRepaymentOrderDetail(voViewOrderDetailResp);
            return ResponseEntity.ok(warpRes);
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewRepaymentOrderDetailWarpRes.class));
        }
    }


    /**
     * pc:未还款详情
     *
     * @param collectionListReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewCollectionWarpRes> orderList(VoCollectionListReq collectionListReq) {
        try {

            VoViewCollectionWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewCollectionWarpRes.class);
            Map<String, Object> resultMaps = borrowRepaymentService.collectionList(collectionListReq);
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            List<VoCollection> repaymentList = (List<VoCollection>) resultMaps.get("repaymentList");
            warpRes.setTotalCount(totalCount);
            warpRes.setVoCollections(repaymentList);
            return ResponseEntity.ok(warpRes);
        } catch (Throwable e) {
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewCollectionWarpRes.class));
        }
    }

    @Override
    public ResponseEntity<VoViewRepayCollectionLogWarpRes> logs(Long borrowId) {
        try {
            List<RepayCollectionLog> logList = borrowRepaymentService.logs(borrowId);
            VoViewRepayCollectionLogWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewRepayCollectionLogWarpRes.class);
            warpRes.setCollectionLogs(logList);
            return ResponseEntity.ok(warpRes);
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "查询失败", VoViewRepayCollectionLogWarpRes.class));
        }
    }

    /**
     * 新还款处理
     * 1.查询并判断还款记录是否存在!
     * 2.处理资金还款人、收款人资金变动
     * 3.判断是否是还名义借款人垫付，垫付需要改变垫付记录状态
     * 4.还款成功后变更改还款状态
     * 5.结束债权
     * 6.发送投资人收到还款站内信
     * 7.投资人收到积分
     * 8.还款最后新增统计
     *
     * @param repaymentId
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> newRepayDeal(long repaymentId, String batchNo) throws Exception {
        //1.查询并判断还款记录是否存在!
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);/* 当期还款记录 */
        Preconditions.checkNotNull(borrowRepayment, "还款记录不存在!");
        Borrow parentBorrow = borrowService.findById(borrowRepayment.getBorrowId());/* 还款记录对应的借款记录 */
        Preconditions.checkNotNull(parentBorrow, "借款记录不存在!");
        /* 还款对应的投标记录  包括债权转让在里面 */
        Specification<Tender> ts = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", parentBorrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(ts);/* 还款对应的投标记录  包括债权转让在里面 */
        Preconditions.checkNotNull(tenderList, "立即还款: 投标记录为空!");
        /* 投标记录id */
        Set<Long> tenderIds = tenderList.stream().map(tender -> tender.getId()).collect(Collectors.toSet());
        /* 查询未转让的投标记录回款记录 */
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIds.toArray())
                .eq("status", 0)
                .eq("order", borrowRepayment.getOrder())
                .eq("transferFlag", 0)
                .build();
        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull(borrowCollectionList, "立即还款: 回款记录为空!");
        /* 是否垫付 */
        boolean advance = !ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes());
        //2.处理资金还款人、收款人资金变动
        batchAssetChangeHelper.batchAssetChangeAndCollection(repaymentId, batchNo, BatchAssetChangeContants.BATCH_REPAY);
        //4.还款成功后变更改还款状态
        changeRepaymentAndRepayStatus(parentBorrow, tenderList, borrowRepayment, borrowCollectionList, advance);
        //5.结束第三方债权并更新借款状态（还款最后一期的时候）
        endThirdTenderAndChangeBorrowStatus(parentBorrow, borrowRepayment);
        if (!advance) { //非转让标需要统计与发放短信
            //6.发送投资人收到还款站内信
            sendCollectionNotices(borrowCollectionList, advance, parentBorrow);
            //7.发放积分
            giveInterest(borrowCollectionList, parentBorrow);
            //8.还款最后新增统计
            updateRepaymentStatistics(parentBorrow, borrowRepayment);
            //9.更新投资人缓存
            updateUserCacheByReceivedRepay(borrowCollectionList, parentBorrow);
            //10.项目回款短信通知
            smsNoticeByReceivedRepay(borrowCollectionList, parentBorrow, borrowRepayment);
        }
        //通知风车理财用户 回款成功
        windmillTenderBiz.backMoneyNotify(borrowCollectionList);

        return ResponseEntity.ok(VoBaseResp.ok("还款处理成功!"));
    }

    /**
     * 项目回款短信通知
     *
     * @param borrowCollectionList
     * @param parentBorrow
     * @param borrowRepayment
     */
    private void smsNoticeByReceivedRepay(List<BorrowCollection> borrowCollectionList, Borrow parentBorrow, BorrowRepayment borrowRepayment) {
        Set<Long> userIds = borrowCollectionList.stream().map(borrowCollection -> borrowCollection.getUserId()).collect(toSet());/* 回款用户id */
        Map<Long /* 投资会员id */, List<BorrowCollection>> borrowCollrctionMaps = borrowCollectionList.stream().collect(groupingBy(BorrowCollection::getUserId)); /* 回款记录集合 */
        Specification<Users> us = Specifications
                .<Users>and()
                .in("id", userIds.toArray())
                .build();
        List<Users> usersList = userService.findList(us);/* 回款用户缓存记录列表 */
        Map<Long /* 投资会员id */, Users> userMaps = usersList.stream().collect(Collectors.toMap(Users::getId, Function.identity()));/* 回款用户记录列表*/
        userIds.stream().forEach(userId -> {
            List<BorrowCollection> borrowCollections = borrowCollrctionMaps.get(userId);/* 当前用户的所有回款 */
            Users users = userMaps.get(userId);//投资人会员记录
            long principal = borrowCollections.stream().mapToLong(BorrowCollection::getPrincipal).sum(); /* 当前用户的所有回款本金 */
            long interest = borrowCollections.stream().mapToLong(BorrowCollection::getInterest).sum();/* 当前用户的所有回款本金 */
            String phone = users.getPhone();/* 投资人手机号 */
            String name = "";
            if (ObjectUtils.isEmpty(phone)) {
                MqConfig config = new MqConfig();
                config.setQueue(MqQueueEnum.RABBITMQ_SMS);
                config.setTag(MqTagEnum.SMS_REGISTER);
                switch (parentBorrow.getType()) {
                    case BorrowContants.CE_DAI:
                        name = "车贷标";
                        break;
                    case BorrowContants.JING_ZHI:
                        name = "净值标";
                        break;
                    case BorrowContants.QU_DAO:
                        name = "渠道标";
                        break;
                    default:
                        name = "投标还款";
                }
                Map<String, String> body = new HashMap<>();
                body.put(MqConfig.PHONE, phone);
                body.put(MqConfig.IP, "127.0.0.1");
                body.put(MqConfig.MSG_ID, StringHelper.toString(parentBorrow.getId()));
                body.put(MqConfig.MSG_NAME, name);
                body.put(MqConfig.MSG_ORDER, StringHelper.toString(borrowRepayment.getOrder() + 1));
                body.put(MqConfig.MSG_MONEY, StringHelper.formatDouble(principal, 100, true));
                body.put(MqConfig.MSG_INTEREST, StringHelper.formatDouble(interest, 100, true));
                config.setMsg(body);

                boolean state = mqHelper.convertAndSend(config);
                if (!state) {
                    log.error(String.format("发送投资人收到还款短信失败:%s", config));
                }
            }
        });
    }

    /**
     * 更新用户缓存
     *
     * @param borrowCollectionList
     * @param parentBorrow
     */
    private void updateUserCacheByReceivedRepay(List<BorrowCollection> borrowCollectionList, Borrow parentBorrow) {
        Set<Long> userIds = borrowCollectionList.stream().map(borrowCollection -> borrowCollection.getUserId()).collect(toSet());/* 回款用户id */
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        Map<Long, List<BorrowCollection>> borrowCollrctionMaps = borrowCollectionList.stream().collect(groupingBy(BorrowCollection::getUserId)); /* 回款记录集合 */
        Specification<UserCache> ucs = Specifications
                .<UserCache>and()
                .in("userId", userIds.toArray())
                .build();
        List<UserCache> userCaches = userCacheService.findList(ucs);/* 回款用户缓存记录列表 */
        Map<Long, UserCache> userCacheMaps = userCaches.stream().collect(Collectors.toMap(UserCache::getUserId, Function.identity()));/* 回款用户缓存记录列表*/
        userIds.stream().forEach(userId -> {
            List<BorrowCollection> borrowCollections = borrowCollrctionMaps.get(userId);/* 当前用户的所有回款 */
            long principal = borrowCollections.stream().mapToLong(BorrowCollection::getPrincipal).sum(); /* 当前用户的所有回款本金 */
            long interest = borrowCollections.stream().mapToLong(BorrowCollection::getInterest).sum();/* 当前用户的所有回款本金 */
            UserCache userCache = userCacheMaps.get(userId);
            if (parentBorrow.getType() == 0) {
                userCache.setTjWaitCollectionPrincipal(userCache.getTjWaitCollectionPrincipal() - principal);
                userCache.setTjWaitCollectionInterest(userCache.getTjWaitCollectionInterest() - interest);
            } else if (parentBorrow.getType() == 4) {
                userCache.setQdWaitCollectionPrincipal(userCache.getQdWaitCollectionPrincipal() - principal);
                userCache.setQdWaitCollectionInterest(userCache.getQdWaitCollectionInterest() - interest);
            }

            Long collectionMoney = borrowCollections.stream().mapToLong(p -> p.getCollectionMoney()).sum();
            Long collectionMoneyYes = borrowCollections.stream().mapToLong(p -> p.getCollectionMoneyYes()).sum();
            //逾期收入
            if (collectionMoneyYes >= collectionMoney) {
                userCache.setIncomeOverdue(collectionMoneyYes - collectionMoney);
            } else {
                log.error("当前还款批次异常：打印回款期数信息：", GSON.toJson(borrowCollections));
                userCache.setIncomeOverdue(0L);
            }
            userCacheService.save(userCache);
        });
    }

    /**
     * 给投资人发放积分
     *
     * @param borrowCollectionList
     * @param parentBorrow
     */
    private void giveInterest(List<BorrowCollection> borrowCollectionList, Borrow parentBorrow) {
        borrowCollectionList.stream().forEach(borrowCollection -> {
            long actualInterest = borrowCollection.getCollectionMoneyYes() - borrowCollection.getPrincipal();/* 实收利息 */
            //投资积分
            long integral = actualInterest / 100 * 10;
            if ((parentBorrow.getType() == 0 || parentBorrow.getType() == 4) && 0 < integral) {
                Users users = userService.findById(borrowCollection.getUserId());
                if (StringUtils.isEmpty(users.getWindmillId())) {  // 非风车理财派发积分
                    IntegralChangeEntity integralChangeEntity = new IntegralChangeEntity();
                    integralChangeEntity.setType(IntegralChangeEnum.TENDER);
                    integralChangeEntity.setValue(integral);
                    integralChangeEntity.setUserId(borrowCollection.getUserId());
                    try {
                        integralChangeHelper.integralChange(integralChangeEntity);
                    } catch (Exception e) {
                        log.error("投资人回款积分发放失败：", e);
                    }
                }

            }
        });
    }

    /**
     * 发送回款站内信
     *
     * @param borrowCollectionList
     * @param advance
     * @param parentBorrow
     */
    private void sendCollectionNotices(List<BorrowCollection> borrowCollectionList, boolean advance, Borrow parentBorrow) {

        //迭代投标人记录
        borrowCollectionList.stream().forEach(borrowCollection -> {
            long actualInterest = borrowCollection.getCollectionMoneyYes() - borrowCollection.getPrincipal();/* 实收利息 */
            String noticeContent = String.format("客户在%s已将借款[%s]第%s期还款,还款金额为%s元", DateHelper.dateToString(new Date(), "yyyy-MM-dd HH:mm:ss"), parentBorrow.getName(), (borrowCollection.getOrder() + 1), StringHelper.formatDouble(actualInterest, 100, true));
            if (advance) {
                noticeContent = "广富宝在" + DateHelper.dateToString(new Date(), "yyyy-MM-dd HH:mm:ss") + " 已将借款[" + parentBorrow.getName() +
                        "]第" + (borrowCollection.getOrder() + 1) + "期垫付还款,垫付金额为" + StringHelper.formatDouble(actualInterest, 100, true) + "元";
            }

            Notices notices = new Notices();
            notices.setFromUserId(1L);
            notices.setUserId(borrowCollection.getUserId());
            notices.setRead(false);
            notices.setName("客户还款");
            notices.setContent(noticeContent);
            notices.setType("system");
            notices.setCreatedAt(new Date());
            notices.setUpdatedAt(new Date());
            //发送站内信
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_NOTICE);
            mqConfig.setTag(MqTagEnum.NOTICE_PUBLISH);
            Map<String, String> body = GSON.fromJson(GSON.toJson(notices), TypeTokenContants.MAP_TOKEN);
            mqConfig.setMsg(body);
            try {
                log.info(String.format("repaymentBizImpl sendCollectionNotices send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("repaymentBizImpl sendCollectionNotices send mq exception", e);
            }
        });
    }

    /**
     * 还款最后新增统计
     *
     * @param borrowRepayment
     */
    private void updateRepaymentStatistics(Borrow parentBorrow, BorrowRepayment borrowRepayment) {
        //更新统计数据
        try {
            long repayMoney = borrowRepayment.getRepayMoney();/* 还款金额 */
            long principal = borrowRepayment.getPrincipal();/* 还款本金 */
            Statistic statistic = new Statistic();
            statistic.setWaitRepayTotal(-repayMoney);
            if (!parentBorrow.isTransfer()) {//判断非转让标
                if (parentBorrow.getType() == 0) { //车贷标
                    statistic.setTjWaitRepayPrincipalTotal(-principal);
                    statistic.setTjWaitRepayTotal(-repayMoney);
                } else if (parentBorrow.getType() == 1) { //净值标
                    statistic.setJzWaitRepayPrincipalTotal(-principal);
                    statistic.setJzWaitRepayTotal(-repayMoney);
                } else if (parentBorrow.getType() == 4) { //渠道标
                    statistic.setQdWaitRepayPrincipalTotal(-principal);
                    statistic.setQdWaitRepayTotal(-repayMoney);
                }
            }
            if (!ObjectUtils.isEmpty(statistic)) {
                statisticBiz.caculate(statistic);
            }
        } catch (Throwable e) {
            log.error(String.format("repaymentBizImpl updateRepaymentStatistics 立即还款统计错误：", e));
        }
    }

    /**
     * 结束第三方债权并更新借款状态（还款最后一期的时候）
     *
     * @param borrowRepayment
     */
    private void endThirdTenderAndChangeBorrowStatus(Borrow parentBorrow, BorrowRepayment borrowRepayment) {
        // 结束债权：最后一期还款时
        if (borrowRepayment.getOrder().intValue() == (parentBorrow.getTotalOrder() - 1)) {
            parentBorrow.setCloseAt(borrowRepayment.getRepayAtYes());
            //推送队列结束债权
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_CREDIT);
            mqConfig.setTag(MqTagEnum.END_CREDIT);
            mqConfig.setSendTime(DateHelper.addMinutes(new Date(), 1));
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.MSG_BORROW_ID, StringHelper.toString(parentBorrow.getId()),
                            MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
            mqConfig.setMsg(body);
            try {
                log.info(String.format("repaymentBizImpl endThirdTenderAndChangeBorrowStatus send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("repaymentBizImpl endThirdTenderAndChangeBorrowStatus send mq exception", e);
            }
            parentBorrow.setUpdatedAt(new Date());
            borrowService.updateById(parentBorrow);
        }
    }

    /**
     * @param borrowRepayment
     * @throws Exception 3.判断是否是还名义借款人垫付，垫付需要改变垫付记录状态（逾期天数与日期应当在还款前计算完成）
     *                   4.还款成功后变更改还款状态（还款金额在还款前计算完成）
     */
    private void changeRepaymentAndRepayStatus(Borrow parentBorrow, List<Tender> tenderList, BorrowRepayment borrowRepayment, List<BorrowCollection> borrowCollectionList, boolean advance) throws Exception {
        //更改垫付记录、还款记录状态
        borrowRepayment.setStatus(1);
        borrowRepayment.setRepayAtYes(new Date());
        borrowRepaymentService.updateById(borrowRepayment);

        // 结束债权：最后一期还款时
        if (borrowRepayment.getOrder() == (parentBorrow.getTotalOrder() - 1)) {
            tenderList.stream().forEach(tender -> {
                tender.setState(3);
                tender.setUpdatedAt(new Date());
            });
            tenderService.save(tenderList);
        }

        //改变回款状态
        borrowCollectionList.stream().forEach(borrowCollection -> {
            borrowCollection.setStatus(1);
            borrowCollection.setCollectionAtYes(new Date());
            borrowCollection.setUpdatedAt(new Date());
        });
        borrowCollectionService.save(borrowCollectionList);

        if (advance) { //存在垫付时间则当条还款已经被垫付过
            AdvanceLog advanceLog = advanceLogService.findByRepaymentId(borrowRepayment.getId());
            Preconditions.checkNotNull(advanceLog, "RepaymentBizImpl changeRepaymentAndRepayStatus 垫付记录不存在!请联系客服。");

            //更新垫付记录转状态
            advanceLog.setStatus(1);
            advanceLog.setRepayAtYes(new Date());
            advanceLogService.save(advanceLog);
        }
    }

    /**
     * 新版立即还款
     * 1.还款判断
     * 2.
     *
     * @param repayReq
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseEntity<VoBaseResp> newRepay(VoRepayReq repayReq) throws Exception {
        /* 还款人id */
        long repayUserId = repayReq.getUserId();
        /* 还款记录id */
        long borrowRepaymentId = repayReq.getRepaymentId();
        /* 利息百分比 */
        double interestPercent = repayReq.getInterestPercent();
        UserThirdAccount repayUserThirdAccount = userThirdAccountService.findByUserId(repayUserId);
        Preconditions.checkNotNull(repayUserThirdAccount, "批量还款: 还款用户存管账户不存在");
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(borrowRepaymentId);
        Preconditions.checkNotNull(borrowRepayment, "批量还款: 还款记录不存在");
        Borrow parentBorrow = borrowService.findByIdLock(borrowRepayment.getBorrowId());
        Preconditions.checkNotNull(parentBorrow, "批量还款: 还款标的信息不存在");
        Asset repayAsset = assetService.findByUserIdLock(repayUserId);
        Preconditions.checkNotNull(repayAsset, "批量还款: 还款人账户不存在");
        /* 冻结orderId */
        String freezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        //还款参数
        Map<String, Object> acqResMap = new HashMap<>();
        acqResMap.put("userId", repayUserId);
        acqResMap.put("repaymentId", borrowRepayment.getId());
        acqResMap.put("interestPercent", 1d);
        acqResMap.put("isUserOpen", true);
        acqResMap.put("freezeOrderId", freezeOrderId);

        ResponseEntity<VoBaseResp> conditionResponse = repayConditionCheck(repayUserThirdAccount, borrowRepayment, acqResMap);  // 验证参数
        if (!conditionResponse.getStatusCode().equals(HttpStatus.OK)) {
            return conditionResponse;
        }

        // 正常还款
        ResponseEntity resp = normalRepay(freezeOrderId, acqResMap, repayUserThirdAccount, borrowRepayment, parentBorrow,
                interestPercent, repayAsset, repayReq);

        return resp;
    }

    /**
     * 改变还款与垫付记录的值
     *
     * @param borrowRepayment
     * @param lateDays
     * @param lateInterest
     * @param advance
     */
    public void changeRepaymentAndAdvanceRecord(BorrowRepayment borrowRepayment, int lateDays, long repayMoney, long lateInterest, boolean advance) {
        Date nowDate = new Date();
        borrowRepayment.setLateDays(lateDays);
        borrowRepayment.setLateInterest(lateInterest);
        borrowRepayment.setRepayMoneyYes(repayMoney);
        borrowRepayment.setRepayMoney(repayMoney);
        borrowRepayment.setUpdatedAt(nowDate);
        borrowRepaymentService.save(borrowRepayment);
        if (advance) {
            AdvanceLog advanceLog = advanceLogService.findByRepaymentId(borrowRepayment.getId());/* 担保人还款记录 */
            Preconditions.checkNotNull(advanceLog, "垫付记录不存在!请联系客服");
            //更新垫付记录
            advanceLog.setRepayMoneyYes(repayMoney + lateInterest);
            advanceLogService.save(advanceLog);
        }
    }

    /**
     * 新增资产更改记录
     *
     * @param batchNo
     * @param id
     * @param advance
     * @return
     */
    private BatchAssetChange addBatchAssetChange(String batchNo, Long id, boolean advance) {
        BatchAssetChange batchAssetChange = new BatchAssetChange();
        batchAssetChange.setSourceId(id);
        batchAssetChange.setState(0);
        batchAssetChange.setType(BatchAssetChangeContants.BATCH_REPAY);
        batchAssetChange.setCreatedAt(new Date());
        batchAssetChange.setUpdatedAt(new Date());
        batchAssetChange.setBatchNo(batchNo);
        batchAssetChangeService.save(batchAssetChange);
        return batchAssetChange;
    }

    /**
     * 生成还款人还款批次资金改变记录
     *
     * @param batchAssetChangeId
     * @param borrowRepayment
     * @param borrow
     * @param interestPercent
     * @param isUserOpen
     * @param lateInterest
     * @param groupSeqNo
     * @param actualMoney        真是金额
     */
    public void addBatchAssetChangeByBorrower(long batchAssetChangeId,
                                              BorrowRepayment borrowRepayment,
                                              Borrow borrow,
                                              double interestPercent,
                                              boolean isUserOpen,
                                              long lateInterest,
                                              long repayUserId,
                                              String groupSeqNo,
                                              boolean advance,
                                              long actualMoney) {
        Date nowDate = new Date();
        // 借款人还款
        BatchAssetChangeItem batchAssetChangeItem = new BatchAssetChangeItem();
        batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
        batchAssetChangeItem.setState(0);
        batchAssetChangeItem.setType(AssetChangeTypeEnum.repayment.getLocalType());  // 还款
        batchAssetChangeItem.setUserId(repayUserId);
        batchAssetChangeItem.setMoney(actualMoney);
        batchAssetChangeItem.setRemark(String.format("对借款[%s]第%s期的还款",
                borrow.getName(),
                StringHelper.toString(borrowRepayment.getOrder() + 1)));
        if (interestPercent < 1) {
            batchAssetChangeItem.setRemark("（提前结清）");
        } else if (!isUserOpen) {
            batchAssetChangeItem.setRemark("（系统自动还款）");
        }
        batchAssetChangeItem.setCreatedAt(nowDate);
        batchAssetChangeItem.setUpdatedAt(nowDate);
        batchAssetChangeItem.setSourceId(borrowRepayment.getId());
        batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
        batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
        batchAssetChangeItemService.save(batchAssetChangeItem);

        if ((lateInterest > 0) && new Date().getTime() > DateHelper.endOfDate(DateHelper.stringToDate("2017-09-03 23:59:59")).getTime()) { // 扣除借款人还款滞纳金
            batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.repayMentPenaltyFee.getLocalType());  // 扣除借款人还款滞纳金
            batchAssetChangeItem.setUserId(repayUserId);
            batchAssetChangeItem.setMoney(lateInterest);
            batchAssetChangeItem.setRemark(String.format("借款[%s]的逾期罚息", borrow.getName()));
            batchAssetChangeItem.setCreatedAt(nowDate);
            batchAssetChangeItem.setUpdatedAt(nowDate);
            batchAssetChangeItem.setSourceId(borrowRepayment.getId());
            batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);
        }

        if (advance || borrow.getUserId().intValue() == repayUserId) {
            // 扣除借款人待还
            batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.paymentSub.getLocalType());  // 扣除待还
            batchAssetChangeItem.setUserId(repayUserId);
            batchAssetChangeItem.setMoney(borrowRepayment.getRepayMoney());
            batchAssetChangeItem.setInterest(borrowRepayment.getInterest());
            batchAssetChangeItem.setRemark("还款成功扣除待还");
            batchAssetChangeItem.setCreatedAt(nowDate);
            batchAssetChangeItem.setUpdatedAt(nowDate);
            batchAssetChangeItem.setSourceId(borrowRepayment.getId());
            batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);
        }
    }

    /**
     * 正常还款流程
     *
     * @param repayUserThirdAccount
     * @param borrowRepayment
     * @param borrow
     * @return
     * @throws Exception
     */
    private ResponseEntity<VoBaseResp> normalRepay(String freezeOrderId, Map<String, Object> acqResMap,
                                                   UserThirdAccount repayUserThirdAccount,
                                                   BorrowRepayment borrowRepayment,
                                                   Borrow borrow,
                                                   double interestPercent,
                                                   Asset repayAsset,
                                                   VoRepayReq voRepayReq) throws Exception {
        Date nowDate = new Date();
        log.info(String.format("批次还款: 进入正常还款流程 repaymentId->", borrowRepayment.getId()));
        int lateDays = getLateDays(borrowRepayment);   //计算逾期天数
        String batchNo = jixinHelper.getBatchNo();    // 批次号
        String groupSeqNo = assetChangeProvider.getGroupSeqNo(); // 资产记录分组流水号
        boolean advance = !ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes());   // 是否是垫付

        /* 投资记录：不包含理财计划 */
        Specification<Tender> specification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(specification);
        Preconditions.checkState(!CollectionUtils.isEmpty(tenderList), "投资记录不存在!");
        /* 投资id集合 */
        List<Long> tenderIds = tenderList.stream().map(p -> p.getId()).collect(Collectors.toList());
        /* 投资人回款记录 */
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIds.toArray())
                .eq("status", 0)
                .eq("transferFlag", 0)
                .eq("order", borrowRepayment.getOrder())
                .build();
        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull(borrowCollectionList, "生成即信还款计划: 获取回款计划列表为空!");
        /* 资金变动集合 */
        List<RepayAssetChange> repayAssetChanges = new ArrayList<>();
        List<Repay> repays = calculateRepayPlan(borrow,
                repayUserThirdAccount.getAccountId(),
                tenderList,
                borrowCollectionList,
                lateDays,
                borrowRepayment,
                interestPercent,
                repayAssetChanges);

        //所有交易金额 交易金额指的是txAmount字段
        double txAmount = repays.stream().mapToDouble(r -> NumberHelper.toDouble(r.getTxAmount())).sum();
        //所有交易利息
        double intAmount = repays.stream().mapToDouble(r -> NumberHelper.toDouble(r.getIntAmount())).sum();
        //所有还款手续费
        double txFeeOut = repays.stream().mapToDouble(r -> NumberHelper.toDouble(r.getTxFeeOut())).sum();
        double freezeMoney = MoneyHelper.round(txAmount + txFeeOut + intAmount, 2);/* 冻结金额 */
        // 生成投资人还款资金变动记录
        BatchAssetChange batchAssetChange = addBatchAssetChange(batchNo, borrowRepayment.getId(), advance);
        // 生成回款人资金变动记录  返回值实际还款本金和利息  不包括手续费
        long repayMoney = doGenerateAssetChangeRecodeByRepay(borrow, borrowRepayment, borrowRepayment.getUserId(), repayAssetChanges, groupSeqNo, batchAssetChange, advance);
        //真实的逾期费用
        /*平台实际收取的逾期费用*/
        long realPlatformOverdueFee = repayAssetChanges.stream().mapToLong(RepayAssetChange::getPlatformOverdueFee).sum();
        /*投资人实际收取的逾期费用*/
        long realOverdueFee = repayAssetChanges.stream().mapToLong(RepayAssetChange::getOverdueFee).sum();
        long realLateInterest = realOverdueFee + realPlatformOverdueFee;
        // 生成还款人还款批次资金改变记录
        addBatchAssetChangeByBorrower(batchAssetChange.getId(), borrowRepayment, borrow,
                interestPercent, voRepayReq.getIsUserOpen(),
                realLateInterest, voRepayReq.getUserId(), groupSeqNo,
                advance, repayMoney);
        //改变还款与垫付记录的值
        /**
         * @// TODO: 2017/9/8 判断 
         */
        changeRepaymentAndAdvanceRecord(borrowRepayment, lateDays, repayMoney, realLateInterest, advance);
        /*ResponseEntity<VoBaseResp> resp = checkAssetByRepay(repayAsset, money);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            throw new Exception(resp.getBody().getState().getMsg());
        }*/

        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(repayUserThirdAccount.getAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(freezeMoney, false));
        balanceFreezeReq.setOrderId(freezeOrderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            throw new Exception("正常还款流程：" + balanceFreezeResp.getRetMsg());
        }

        try {
            // 冻结还款金额
            long money = new Double(MoneyHelper.round(MoneyHelper.multiply(freezeMoney, 100d), 0)).longValue();
            AssetChange freezeAssetChange = new AssetChange();
            freezeAssetChange.setForUserId(repayUserThirdAccount.getUserId());
            freezeAssetChange.setUserId(repayUserThirdAccount.getUserId());
            freezeAssetChange.setType(AssetChangeTypeEnum.freeze);
            freezeAssetChange.setRemark(String.format("成功还款标的[%s]冻结", borrow.getName(), StringHelper.formatDouble(money, 100D, true)));
            freezeAssetChange.setSeqNo(assetChangeProvider.getSeqNo());
            freezeAssetChange.setMoney(money);
            freezeAssetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
            freezeAssetChange.setSourceId(borrowRepayment.getId());
            assetChangeProvider.commonAssetChange(freezeAssetChange);

            //批量放款
            acqResMap.put("freezeMoney", freezeMoney);

            //批次还款操作
            BatchRepayReq request = new BatchRepayReq();
            request.setBatchNo(batchNo);
            request.setTxAmount(StringHelper.formatDouble(txAmount, false));
            request.setRetNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repayDeal/run");
            request.setNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repayDeal/check");
            request.setAcqRes(GSON.toJson(acqResMap));
            request.setSubPacks(GSON.toJson(repays));
            request.setChannel(ChannelContant.HTML);
            request.setTxCounts(StringHelper.toString(repays.size()));
            BatchRepayResp response = jixinManager.send(JixinTxCodeEnum.BATCH_REPAY, request, BatchRepayResp.class);
            if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
                throw new Exception(response.getRetMsg());
            }

            //记录日志
            ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
            thirdBatchLog.setBatchNo(batchNo);
            thirdBatchLog.setCreateAt(nowDate);
            thirdBatchLog.setUpdateAt(nowDate);
            thirdBatchLog.setSourceId(borrowRepayment.getId());
            thirdBatchLog.setType(ThirdBatchLogContants.BATCH_REPAY);
            thirdBatchLog.setRemark("即信批次还款.");
            thirdBatchLog.setAcqRes(GSON.toJson(acqResMap));
            thirdBatchLogService.save(thirdBatchLog);
        } catch (Exception e) {

            // 申请即信还款解冻
            String unfreezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_UNFREEZE_PREFIX);
            BalanceUnfreezeReq balanceUnfreezeReq = new BalanceUnfreezeReq();
            balanceUnfreezeReq.setAccountId(repayUserThirdAccount.getAccountId());
            balanceUnfreezeReq.setTxAmount(StringHelper.formatDouble(freezeMoney, false));
            balanceUnfreezeReq.setOrderId(unfreezeOrderId);
            balanceUnfreezeReq.setOrgOrderId(freezeOrderId);
            balanceUnfreezeReq.setChannel(ChannelContant.HTML);
            BalanceUnfreezeResp balanceUnFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_UN_FREEZE, balanceUnfreezeReq, BalanceUnfreezeResp.class);
            if ((ObjectUtils.isEmpty(balanceUnfreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceUnFreezeResp.getRetCode()))) {
                throw new Exception("正常还款解冻资金异常：" + balanceUnFreezeResp.getRetMsg());
            }
            throw new Exception(e);
        }
        return ResponseEntity.ok(VoBaseResp.ok("还款正常"));
    }

    /**
     * 检查还款用户资产账户
     *
     * @param repayAsset
     * @param repayMoney
     * @return
     */
    private ResponseEntity<VoBaseResp> checkAssetByRepay(Asset repayAsset, long repayMoney) {
        /* 还款人资产账户 */

        // 查询存管系统资金
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(repayAsset.getUserId());
        BalanceQueryRequest balanceQueryRequest = new BalanceQueryRequest();
        balanceQueryRequest.setChannel(ChannelContant.HTML);
        balanceQueryRequest.setAccountId(userThirdAccount.getAccountId());
        BalanceQueryResponse balanceQueryResponse = jixinManager.send(JixinTxCodeEnum.BALANCE_QUERY, balanceQueryRequest, BalanceQueryResponse.class);
        if ((ObjectUtils.isEmpty(balanceQueryResponse)) || !balanceQueryResponse.getRetCode().equals(JixinResultContants.SUCCESS)) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "当前网络不稳定,请稍后重试!"));
        }

        long availBal = new Double(MoneyHelper.round(MoneyHelper.multiply(NumberHelper.toDouble(balanceQueryResponse.getAvailBal()), 100d), 0)).longValue();// 可用余额  账面余额-可用余额=冻结金额
        long useMoney = repayAsset.getUseMoney().longValue();
        if (availBal < useMoney) {
            log.error(String.format("资金账户未同步:本地:%s 即信:%s", useMoney, availBal));
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "资金账户未同步，请先在个人中心进行资金同步操作!"));
        }
        return ResponseEntity.ok(VoBaseResp.ok("检查成功!"));
    }

    /**
     * 生成存管还款计划(递归调用解决转让问题)
     *
     * @param borrow
     * @param repayAccountId
     * @param repayAssetChanges
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Repay> calculateRepayPlan(Borrow borrow, String repayAccountId, List<Tender> tenderList,
                                          List<BorrowCollection> borrowCollectionList,
                                          int lateDays, BorrowRepayment borrowRepayment, double interestPercent, List<RepayAssetChange> repayAssetChanges) throws Exception {
        List<Repay> repayList = new ArrayList<>();
        Map<Long/* 投资记录*/, BorrowCollection/* 对应的还款计划*/> borrowCollectionMap = borrowCollectionList.stream().collect(Collectors.toMap(BorrowCollection::getTenderId, Function.identity()));
        /* 投标记录集合 */
        Map<Long, Tender> tenderMaps = tenderList.stream().collect(Collectors.toMap(Tender::getId, Function.identity()));
        /* 投资会员id集合 */
        Set<Long> userIds = tenderList.stream().map(p -> p.getUserId()).collect(Collectors.toSet());
        /* 投资人存管记录列表 */
        Specification<UserThirdAccount> uts = Specifications
                .<UserThirdAccount>and()
                .in("userId", userIds.toArray())
                .build();
        List<UserThirdAccount> userThirdAccountList = userThirdAccountService.findList(uts);
        Preconditions.checkNotNull(userThirdAccountList, "生成即信还款计划: 查询用户存管开户记录列表为空!");
        Map<Long/* 用户ID*/, UserThirdAccount /* 用户存管*/> userThirdAccountMap = userThirdAccountList
                .stream()
                .collect(Collectors.toMap(UserThirdAccount::getUserId, Function.identity()));
        /* 当期回款总利息 */
        long sumCollectionInterest = borrowCollectionList.stream()
                .filter(borrowCollection -> tenderMaps.get(borrowCollection.getTenderId()).getTransferFlag() != 2)
                .mapToLong(BorrowCollection::getInterest).sum();
        // 计算逾期产生的总费用
        long lateInterest = calculateLateInterest(lateDays, borrowRepayment, borrow);
        for (Tender tender : tenderList) {
            long inIn = 0; // 出借人的利息
            long inPr = 0; // 出借人的本金
            int inFee = 0; // 出借人利息费用
            int outFee = 0; // 借款人管理费
            BorrowCollection borrowCollection = borrowCollectionMap.get(tender.getId());  // 还款计划
            // 标的转让中时, 需要取消出让信息
            if (tender.getTransferFlag() == 1) {
                transferBiz.cancelTransferByTenderId(tender.getId());
            }
            // 已经转让的债权, 可以跳过还款
            if (tender.getTransferFlag() == 2 || ObjectUtils.isEmpty(borrowCollection)) {
                continue;
            }

            RepayAssetChange repayAssetChange = new RepayAssetChange();
            repayAssetChanges.add(repayAssetChange);
            inIn = new Double(MoneyHelper.round(borrowCollection.getInterest() * interestPercent, 0)).longValue(); // 还款利息
            inPr = borrowCollection.getPrincipal(); // 还款本金
            repayAssetChange.setUserId(tender.getUserId());
            repayAssetChange.setInterest(inIn);
            repayAssetChange.setPrincipal(inPr);
            repayAssetChange.setBorrowCollection(borrowCollection);

            ImmutableSet<Integer> borrowTypeSet = ImmutableSet.of(0, 4);
            if (borrowTypeSet.contains(borrow.getType())) {  // 车贷标和渠道标利息管理费
                ImmutableSet<Long> stockholder = ImmutableSet.of(2480L, 1753L, 1699L,
                        3966L, 1413L, 1857L,
                        183L, 2327L, 2432L,
                        2470L, 2552L, 2739L,
                        3939L, 893L, 608L,
                        1216L);
                boolean between = isBetween(new Date(), DateHelper.stringToDate("2015-12-25 00:00:00"),
                        DateHelper.stringToDate("2017-12-31 23:59:59"));
                if ((stockholder.contains(tender.getUserId())) && (between)) {
                    inFee += 0;
                } else {
                    Long userId = tender.getUserId();
                    Users user = userService.findById(userId);
                    if (!StringUtils.isEmpty(user.getWindmillId())) { // 风车理财用户不收管理费
                        log.info(String.format("风车理财：%s", user));
                        inFee += 0;
                    } else {
                        inFee += new Double(MoneyHelper.round(inIn * 0.1, 0)).longValue();
                    }
                }
            }

            repayAssetChange.setInterestFee(inFee);
            long overdueFee = 0;
            long platformOverdueFee;
            if ((lateDays > 0) && (lateInterest > 0)) {  //借款人逾期罚息
                overdueFee = new Double(MoneyHelper.round(borrowCollection.getInterest() / new Double(sumCollectionInterest) * lateInterest / 2, 0)).longValue();// 出借人收取50% 逾期管理费 ;
                repayAssetChange.setOverdueFee(overdueFee);
                inIn += overdueFee;
                platformOverdueFee = new Double(MoneyHelper.round(borrowCollection.getInterest() / new Double(sumCollectionInterest) * lateInterest / 2, 0)).longValue(); // 平台收取50% 逾期管理费
                repayAssetChange.setPlatformOverdueFee(platformOverdueFee);
                outFee += platformOverdueFee;
            }

            /* 还款orderId */
            String orderId = JixinHelper.getOrderId(JixinHelper.REPAY_PREFIX);
            Repay repay = new Repay();
            repay.setAccountId(repayAccountId);
            repay.setOrderId(orderId);
            repay.setTxAmount( StringHelper.formatDouble(MoneyHelper.round(new Double(inPr), 100),false));
            repay.setIntAmount(StringHelper.formatDouble(MoneyHelper.round(new Double(inIn), 100), false));
            repay.setTxFeeIn(StringHelper.formatDouble(MoneyHelper.round(new Double(inFee), 100), false));
            repay.setTxFeeOut(StringHelper.formatDouble(MoneyHelper.round(new Double(outFee), 100), false));
            repay.setProductId(borrow.getProductId());
            repay.setAuthCode(tender.getAuthCode());
            UserThirdAccount userThirdAccount = userThirdAccountMap.get(tender.getUserId());
            Preconditions.checkNotNull(userThirdAccount, "投资人未开户!");
            repay.setForAccountId(userThirdAccount.getAccountId());
            repayList.add(repay);
            //改变回款状态
            borrowCollection.setTRepayOrderId(orderId);
            borrowCollection.setLateInterest(overdueFee);
            borrowCollection.setCollectionMoneyYes(inPr + inIn);
            borrowCollection.setUpdatedAt(new Date());
            borrowCollectionService.updateById(borrowCollection);
        }
        return repayList;
    }

    /**
     * 生成回款记录
     *
     * @param borrow
     * @param borrowRepayment
     * @param repayUserId
     * @param repayAssetChanges
     * @param batchAssetChange
     */
    private long doGenerateAssetChangeRecodeByRepay(Borrow borrow, BorrowRepayment borrowRepayment, long repayUserId, List<RepayAssetChange> repayAssetChanges, String groupSeqNo, BatchAssetChange batchAssetChange, boolean advance) throws ExecutionException {
        long batchAssetChangeId = batchAssetChange.getId();
        Long feeAccountId = assetChangeProvider.getFeeAccountId();  // 平台收费账户ID
        Date nowDate = new Date();
        /* 还款金额 */
        long repayMoney = 0;
        for (RepayAssetChange repayAssetChange : repayAssetChanges) {
            repayMoney += repayAssetChange.getPrincipal() + repayAssetChange.getInterest();
            /* 回款记录 */
            BorrowCollection borrowCollection = repayAssetChange.getBorrowCollection();
            // 归还本金和利息
            BatchAssetChangeItem batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            if (advance) {//判断是否是垫付
                batchAssetChangeItem.setType(AssetChangeTypeEnum.compensatoryReceivedPayments.getLocalType());  // 名义借款人收到垫付还款
            } else {
                batchAssetChangeItem.setType(AssetChangeTypeEnum.receivedPayments.getLocalType()); //借款人收到还款
            }
            batchAssetChangeItem.setUserId(repayAssetChange.getUserId());
            batchAssetChangeItem.setForUserId(repayUserId);  // 还款人
            batchAssetChangeItem.setMoney(repayAssetChange.getPrincipal() + repayAssetChange.getInterest());   // 本金加利息
            batchAssetChangeItem.setInterest(repayAssetChange.getInterest());  // 利息
            batchAssetChangeItem.setCreatedAt(nowDate);
            batchAssetChangeItem.setUpdatedAt(nowDate);
            batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            batchAssetChangeItem.setSourceId(borrowCollection.getId());
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItem.setRemark(String.format("收到客户对借款[%s]第%s期的还款", borrow.getName(), (borrowRepayment.getOrder() + 1)));
            batchAssetChangeItemService.save(batchAssetChangeItem);
            // 扣除利息管理费
            if (repayAssetChange.getInterestFee() > 0) {
                batchAssetChangeItem = new BatchAssetChangeItem();
                batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
                batchAssetChangeItem.setState(0);
                batchAssetChangeItem.setType(AssetChangeTypeEnum.interestManagementFee.getLocalType());  // 扣除投资人利息管理费
                batchAssetChangeItem.setUserId(repayAssetChange.getUserId());
                batchAssetChangeItem.setForUserId(feeAccountId);
                batchAssetChangeItem.setMoney(repayAssetChange.getInterestFee());
                batchAssetChangeItem.setRemark(String.format("扣除借款标的[%s]利息管理费%s元", borrow.getName(), StringHelper.formatDouble(repayAssetChange.getInterestFee() / 100D, false)));
                batchAssetChangeItem.setCreatedAt(nowDate);
                batchAssetChangeItem.setUpdatedAt(nowDate);
                batchAssetChangeItem.setSourceId(borrowRepayment.getId());
                batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
                batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
                batchAssetChangeItemService.save(batchAssetChangeItem);

                // 收费账户添加利息管理费用
                batchAssetChangeItem = new BatchAssetChangeItem();
                batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
                batchAssetChangeItem.setState(0);
                batchAssetChangeItem.setType(AssetChangeTypeEnum.platformInterestManagementFee.getLocalType());  // 收费账户添加利息管理费用
                batchAssetChangeItem.setUserId(feeAccountId);
                batchAssetChangeItem.setForUserId(repayAssetChange.getUserId());
                batchAssetChangeItem.setMoney(repayAssetChange.getInterestFee());
                batchAssetChangeItem.setRemark(String.format("收取借款标的[%s]利息管理费%s元", borrow.getName(), StringHelper.formatDouble(repayAssetChange.getInterestFee() / 100D, false)));
                batchAssetChangeItem.setCreatedAt(nowDate);
                batchAssetChangeItem.setUpdatedAt(nowDate);
                batchAssetChangeItem.setSourceId(borrowRepayment.getId());
                batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
                batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
                batchAssetChangeItemService.save(batchAssetChangeItem);
            }

            // 收取逾期管理费
            if (repayAssetChange.getOverdueFee() > 0) {
                batchAssetChangeItem = new BatchAssetChangeItem();
                batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
                batchAssetChangeItem.setState(0);
                batchAssetChangeItem.setType(AssetChangeTypeEnum.platformRepayMentPenaltyFee.getLocalType());  // 收取逾期管理费
                batchAssetChangeItem.setUserId(feeAccountId);
                batchAssetChangeItem.setForUserId(repayAssetChange.getUserId());
                batchAssetChangeItem.setMoney(repayAssetChange.getPlatformOverdueFee());
                batchAssetChangeItem.setRemark(String.format("收取借款标的[%s]逾期管理费%s元", borrow.getName(), StringHelper.formatDouble(repayAssetChange.getPlatformOverdueFee() / 100D, false)));
                batchAssetChangeItem.setCreatedAt(nowDate);
                batchAssetChangeItem.setUpdatedAt(nowDate);
                batchAssetChangeItem.setSourceId(borrowRepayment.getId());
                batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
                batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
                batchAssetChangeItemService.save(batchAssetChangeItem);
            }

            //扣除投资人待收
            batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.collectionSub.getLocalType());  //  扣除投资人待收
            batchAssetChangeItem.setUserId(repayAssetChange.getUserId());
            batchAssetChangeItem.setMoney(borrowCollection.getCollectionMoney());
            batchAssetChangeItem.setInterest(borrowCollection.getInterest());
            batchAssetChangeItem.setRemark(String.format("收到客户对[%s]借款的还款,扣除待收", borrow.getName()));
            batchAssetChangeItem.setCreatedAt(nowDate);
            batchAssetChangeItem.setUpdatedAt(nowDate);
            batchAssetChangeItem.setSourceId(borrowRepayment.getId());
            batchAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);
        }
        return repayMoney;
    }


    /**
     * 获取用户逾期费用
     * 逾期规则: 未还款本金之和 * 0.4$ 的费用, 平台收取 0.2%, 出借人 0.2%
     *
     * @param borrowRepayment
     * @param repaymentBorrow
     * @return
     */

    private int calculateLateInterest(int lateDays, BorrowRepayment borrowRepayment, Borrow repaymentBorrow) {
        if (0 <= lateDays) {
            return 0;
        }

        long overPrincipal = borrowRepayment.getPrincipal();
        if (borrowRepayment.getOrder() < (repaymentBorrow.getTotalOrder() - 1)) { //
            Specification<BorrowRepayment> brs = Specifications
                    .<BorrowRepayment>and()
                    .eq("borrowId", repaymentBorrow.getId())
                    .eq("status", 0)
                    .build();
            List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
            Preconditions.checkNotNull(borrowRepayment, "垫付: 计算逾期费用时还款计划为空");
            //剩余未还本金
            overPrincipal = borrowRepaymentList.stream().mapToLong(w -> w.getPrincipal()).sum();
        }

        return new Double(MoneyHelper.round(overPrincipal * 0.004 * lateDays, 2)).intValue();
    }

    /**
     * 获取逾期天数
     *
     * @param borrowRepayment
     * @return
     */
    private int getLateDays(BorrowRepayment borrowRepayment) {
        Date nowDateOfBegin = DateHelper.beginOfDate(new Date());
        Date repayDateOfBegin = DateHelper.beginOfDate(borrowRepayment.getRepayAt());
        int lateDays = DateHelper.diffInDays(nowDateOfBegin, repayDateOfBegin, false);
        if (borrowRepayment.getRepayAt().getTime() < DateHelper.stringToDate("2017-09-07 00:00:00").getTime()) {
            lateDays = lateDays - 4;
        }
        lateDays = lateDays < 0 ? 0 : lateDays;
        return lateDays;
    }

    /**
     * 根据
     *
     * @param tranferedTender
     * @return
     */
    private Map<Long, Borrow> findTranferedBorrowByTender(List<Tender> tranferedTender) {
        Map<Long, Borrow> refMap = new HashMap<>();
        tranferedTender.forEach((Tender tender) -> {
            Specification<Borrow> bs = Specifications
                    .<Borrow>and()
                    .eq("tenderId", tender.getId())
                    .eq("status", 3)
                    .build();
            List<Borrow> borrowList = borrowService.findList(bs);
            Preconditions.checkNotNull(borrowList, "批量还款: 查询转让标的为空");
            Borrow borrow = borrowList.get(0);
            refMap.put(tender.getId(), borrow);
        });

        return refMap;
    }


    /**
     * 查询已经债权转让成功投资记录
     *
     * @param tranferedTender
     * @return
     */
    private Map<Long, List<Tender>> findTranferedTenderRecord(List<Tender> tranferedTender) {

        Map<Long, List<Tender>> refMap = new HashMap<>();
        tranferedTender.forEach((Tender tender) -> {
            Specification<Borrow> bs = Specifications
                    .<Borrow>and()
                    .eq("tenderId", tender.getId())
                    .eq("status", 3)
                    .build();
            List<Borrow> borrowList = borrowService.findList(bs);
            Preconditions.checkNotNull(borrowList, "批量还款: 查询转让标的为空");
            Borrow borrow = borrowList.get(0);

            Specification<Tender> specification = Specifications
                    .<Tender>and()
                    .eq("status", 1)
                    .eq("borrowId", borrow.getId())
                    .build();

            List<Tender> tranferedTenderList = tenderService.findList(specification);
            Preconditions.checkNotNull(tranferedTenderList, "批量还款: 获取投资记录列表为空");
            refMap.put(tender.getId(), tranferedTenderList);
        });
        return refMap;
    }

    /**
     * 查询还款计划
     *
     * @param order
     * @param tenderList
     * @return
     */
    private List<BorrowCollection> queryBorrowCollectionByTender(int order, List<Tender> tenderList) {
        Set<Long> tenderIdSet = tenderList.stream().map(p -> p.getId()).collect(Collectors.toSet());
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIdSet.toArray())
                .eq("status", 0)
                .eq("order", order)
                .build();

        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull(borrowCollectionList, "批量还款: 查询还款计划为空");
        return borrowCollectionList;
    }

    /**
     * 获取正常投标记录
     *
     * @param borrowRepayment
     * @return
     */
    private List<Tender> queryTenderByRepayment(BorrowRepayment borrowRepayment) {
        Specification<Tender> specification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrowRepayment.getBorrowId())
                .build();

        List<Tender> tenderList = tenderService.findList(specification);
        Preconditions.checkNotNull(tenderList, "批量还款: 获取投资记录列表为空");
        return tenderList;
    }


    /**
     * 用户还款前期判断
     * 1. 还款用户是否与还款计划用户一致
     * 2. 是否重复提交
     * 3. 判断是否跳跃还款
     *
     * @param userThirdAccount 用户开户
     * @param borrowRepayment  还款计划
     * @return
     */
    private ResponseEntity<VoBaseResp> repayConditionCheck(UserThirdAccount userThirdAccount, BorrowRepayment borrowRepayment, Map<String, Object> acqResMap) {
        /* 名义借款人id */
        UserThirdAccount titularBorrowAccount = jixinHelper.getTitularBorrowAccount(borrowRepayment.getBorrowId());
        // 1. 还款用户是否与还款计划用户一致
        if (!userThirdAccount.getUserId().equals(borrowRepayment.getUserId()) &&
                !userThirdAccount.getUserId().equals(titularBorrowAccount.getUserId())) {
            log.error("批量还款: 还款前期判断, 还款计划用户与主动请求还款用户不匹配");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "非法操作: 还款计划与当前请求用户不一致!"));
        }

        // 2判断提交还款批次是否多次重复提交
        ThirdBatchLog thirdBatchLog = thirdBatchLogBiz.getValidLastBatchLog(String.valueOf(borrowRepayment.getId()),
                ThirdBatchLogContants.BATCH_REPAY_BAIL,
                ThirdBatchLogContants.BATCH_REPAY);

        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(borrowRepayment.getId()),
                ThirdBatchLogContants.BATCH_REPAY_BAIL,
                ThirdBatchLogContants.BATCH_REPAY);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款处理中，请勿重复点击!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            //墊付批次处理
            //触发处理批次放款处理结果队列
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_THIRD_BATCH);
            mqConfig.setTag(MqTagEnum.BATCH_DEAL);
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.SOURCE_ID, StringHelper.toString(thirdBatchLog.getSourceId()),
                            MqConfig.ACQ_RES, GSON.toJson(acqResMap),
                            MqConfig.BATCH_NO, StringHelper.toString(thirdBatchLog.getBatchNo()),
                            MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
            mqConfig.setMsg(body);
            try {
                log.info(String.format("tenderThirdBizImpl repayConditionCheck send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("tenderThirdBizImpl repayConditionCheck send mq exception", e);
            }
            log.info("即信批次回调处理结束");
        }

        //  3. 判断是否跳跃还款
        Specification<BorrowRepayment> borrowRepaymentSpe = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowRepayment.getBorrowId())
                .eq("status", 0)
                .predicate(new LtSpecification<BorrowRepayment>("order", new DataObject(borrowRepayment.getOrder())))
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(borrowRepaymentSpe);
        if (!CollectionUtils.isEmpty(borrowRepaymentList)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("该借款上一期还未还!")));
        }

        //4.判断是否在晚上9点30前还款
        Date endDate = DateHelper.beginOfDate(new Date());
        endDate = DateHelper.setHours(endDate, 21);
        endDate = DateHelper.setMinutes(endDate, 30);
        /*if (new Date().getTime() > endDate.getTime()) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款截止时间为每天晚上9点30!")));
        }*/

        //5.判断是否有在即信处理的债权转让，有不让还款
        if (checkTenderChange(borrowRepayment.getBorrowId())) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "这笔债权转让正在操作中，请等待处理完成后在进行操作！"));
        }
        return ResponseEntity.ok(VoBaseResp.ok("验证成功"));
    }

    /**
     * 立即还款
     *
     * @param voPcInstantlyRepaymentReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcRepay(VoPcInstantlyRepaymentReq voPcInstantlyRepaymentReq) throws Exception {

        String paramStr = voPcInstantlyRepaymentReq.getParamStr();
        if (!SecurityHelper.checkSign(voPcInstantlyRepaymentReq.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc取消借款 签名验证不通过!"));
        }
        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long repaymentId = NumberHelper.toLong(paramMap.get("repaymentId"));
        BorrowRepayment borrowRepayment = borrowRepaymentService.findById(repaymentId);

        VoRepayReq voRepayReq = new VoRepayReq();
        voRepayReq.setRepaymentId(repaymentId);
        voRepayReq.setUserId(borrowRepayment.getUserId());
        return newRepay(voRepayReq);
    }

    /**
     * 垫付检查
     *
     * @param borrowRepayment
     * @return
     */
    public ResponseEntity<VoBaseResp> advanceCheck(BorrowRepayment borrowRepayment, Map<String, Object> acqMap) throws Exception {
        Preconditions.checkNotNull(borrowRepayment, "还款记录不存在！");
        if (borrowRepayment.getStatus() != 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "还款状态已发生改变!"));
        }

        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());
        Preconditions.checkNotNull(borrow, "借款记录不存在！");
        if (borrow.getType() != 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "只有净值标才能垫付!"));
        }
        if (StringUtils.isEmpty(borrow.getTitularBorrowAccountId())) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前借款没有登记名义借款人账号!"));
        }
        UserThirdAccount titularBorrowAccount = userThirdAccountService.findByAccountId(borrow.getTitularBorrowAccountId());
        Preconditions.checkNotNull(titularBorrowAccount, "当前名义收款账户开户信息为空");
        Asset advanceUserAsses = assetService.findByUserIdLock(titularBorrowAccount.getUserId());
        Specification<BorrowRepayment> brs = null;
        int order = borrowRepayment.getOrder();
        if (order > 0) {
            brs = Specifications
                    .<BorrowRepayment>and()
                    .eq("borrowId", borrowRepayment.getBorrowId())
                    .predicate(new LtSpecification("order", new DataObject(order)))
                    .eq("status", 0)
                    .build();
            if (borrowRepaymentService.count(brs) > 0) {
                return ResponseEntity
                        .badRequest()
                        .body(VoBaseResp.error(VoBaseResp.ERROR, "该借款上一期还未还，请先把上一期的还上!"));
            }
        }

        /* 获取批次记录 */
        ThirdBatchLog thirdBatchLog = thirdBatchLogBiz.getValidLastBatchLog(String.valueOf(borrowRepayment.getId()), ThirdBatchLogContants.BATCH_BAIL_REPAY);
        //判断提交还款批次是否多次重复提交
        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(borrowRepayment.getId()), ThirdBatchLogContants.BATCH_BAIL_REPAY);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("垫付处理中，请勿重复点击!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            //墊付批次处理
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_THIRD_BATCH);
            mqConfig.setTag(MqTagEnum.BATCH_DEAL);
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.SOURCE_ID, StringHelper.toString(thirdBatchLog.getSourceId()),
                            MqConfig.ACQ_RES, GSON.toJson(acqMap),
                            MqConfig.BATCH_NO, StringHelper.toString(thirdBatchLog.getBatchNo()),
                            MqConfig.MSG_TIME, DateHelper.dateToString(new Date())
                    );

            mqConfig.setMsg(body);
            try {
                log.info(String.format("borrowThirdBizImpl advanceCheck send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("borrowThirdBizImpl advanceCheck send mq exception", e);
            }
        }


        int lateDays = getLateDays(borrowRepayment);//逾期天数
        long lateInterest = calculateLateInterest(lateDays,borrowRepayment,borrow);//逾期利息
        long repayInterest = borrowRepayment.getInterest();//还款利息
        long repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额
        if (advanceUserAsses.getUseMoney() < (repayMoney + lateInterest)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "账户余额不足，请先充值"));
        }

        //5.判断是否有在即信处理的债权转让，有不让还款
        if (checkTenderChange(borrow.getId())) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "这笔债权转让正在操作中，请等待处理完成后在进行操作！"));
        }
        return ResponseEntity.ok(VoBaseResp.ok("垫付成功!"));
    }

    /**
     * 检查是否存在处理中债权转让
     *
     * @param borrowId
     * @return
     */
    private boolean checkTenderChange(long borrowId) {
        /* 还款记录 */
        Borrow borrow = borrowService.findById(borrowId);
        Preconditions.checkNotNull(borrow, "还款不存在!");
        //1.检查债权转让
        Specification<Tender> ts = Specifications
                .<Tender>and()
                .eq("borrowId", borrowId)
                .eq("status", 1)
                .in("transferFlag", 1)
                .build();

        List<Tender> tenderList = tenderService.findList(ts);
        if (!CollectionUtils.isEmpty(tenderList)) {
            List<Long> tenderIds = tenderList.stream().map(Tender::getId).collect(Collectors.toList());
            Specification<Transfer> transferSpecification = Specifications
                    .<Transfer>and()
                    .in("tenderId", tenderIds.toArray())
                    .eq("state", 1)
                    .build();
            List<Transfer> transferList = transferService.findList(transferSpecification);
            if (!CollectionUtils.isEmpty(transferList)) {
                List<Long> transferIds = transferList.stream().map(Transfer::getId).collect(Collectors.toList());
                //查询即信批次记录
                Specification<ThirdBatchLog> tbls = Specifications
                        .<ThirdBatchLog>and()
                        .in("state", 0, 1)
                        .in("sourceId", transferIds.toArray())
                        .eq("type", ThirdBatchLogContants.BATCH_CREDIT_INVEST)
                        .build();
                long count = thirdBatchLogService.count(tbls);
                if (count > 0) {
                    return true;
                }
            }
        }
        //2.检查还款、垫付、提前还款是否还在在处理中
        Specification<BorrowRepayment> bcs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowId)
                .eq("status", 0)
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(bcs);
        if (!CollectionUtils.isEmpty(borrowRepaymentList)) {
            /* 还款id 跟标的id */
            Set<Long> sourceIds = borrowRepaymentList.stream().map(BorrowRepayment::getId).collect(toSet());
            sourceIds.addAll(borrowRepaymentList.stream().map(BorrowRepayment::getBorrowId).collect(toSet()));

            //查询即信批次记录
            Specification<ThirdBatchLog> tbls = Specifications
                    .<ThirdBatchLog>and()
                    .in("state", 0, 1)
                    .in("sourceId", sourceIds.toArray())
                    .in("type", ThirdBatchLogContants.BATCH_REPAY, ThirdBatchLogContants.BATCH_BAIL_REPAY, ThirdBatchLogContants.BATCH_REPAY_ALL)
                    .build();
            long count = thirdBatchLogService.count(tbls);
            if (count > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * pc垫付
     *
     * @param voPcAdvanceReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcAdvance(VoPcAdvanceReq voPcAdvanceReq) throws Exception {
        String paramStr = voPcAdvanceReq.getParamStr();
        if (!SecurityHelper.checkSign(voPcAdvanceReq.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc取消借款 签名验证不通过!"));
        }
        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long repaymentId = NumberHelper.toLong(paramMap.get("repaymentId"));

        VoAdvanceReq voAdvanceReq = new VoAdvanceReq();
        voAdvanceReq.setRepaymentId(repaymentId);
        return newAdvance(voAdvanceReq);
    }

    /**
     * 新版垫付
     *
     * @param voAdvanceReq
     * @return
     * @throws Exception
     */
    public ResponseEntity<VoBaseResp> newAdvance(VoAdvanceReq voAdvanceReq) throws Exception {
        long repaymentId = voAdvanceReq.getRepaymentId();/* 垫付还款id */
        //垫付前置判断
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);/* 垫付还款记录 */
        Preconditions.checkNotNull(borrowRepayment, "垫付还款记录不存在!");
        Borrow parentBorrow = borrowService.findById(borrowRepayment.getBorrowId());/* 借款记录 */
        Preconditions.checkNotNull(parentBorrow, "借款记录不存在!");
        /* 冻结存管可用资金orderId */
        String freezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        /* 名义借款人id */
        UserThirdAccount titularBorrowAccount = jixinHelper.getTitularBorrowAccount(parentBorrow.getId());
        ResponseEntity<VoBaseResp> resp = ThirdAccountHelper.allConditionCheck(titularBorrowAccount);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            return resp;
        }
        //请求保留参数
        Map<String, Object> acqResMap = new HashMap<>();
        acqResMap.put("repaymentId", borrowRepayment.getId());
        acqResMap.put("freezeOrderId", freezeOrderId);
        acqResMap.put("accountId", titularBorrowAccount.getAccountId());

        resp = advanceCheck(borrowRepayment, acqResMap);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            return resp;
        }
        Asset advanceUserAsses = assetService.findByUserIdLock(titularBorrowAccount.getUserId());/* 垫付人资产记录 */
        /* 批次号 */
        String batchNo = jixinHelper.getBatchNo();
        /* 资产记录分组流水号 */
        String groupSeqNo = assetChangeProvider.getGroupSeqNo();
        /* 逾期天数 */
        int lateDays = getLateDays(borrowRepayment);
        long lateInterest = calculateLateInterest(lateDays, borrowRepayment, parentBorrow);/* 获取逾期利息 */
        long repayInterest = borrowRepayment.getInterest();//还款利息
        long repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额
        if (advanceUserAsses.getUseMoney() < (repayMoney + lateInterest)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "账户余额不足，请先充值"));
        }
        // 生成垫付还款主记录
        BatchAssetChange batchAssetChange = addBatchAssetChangeByAdvance(repaymentId, batchNo);
        // 生成名义借款人垫付批次资产变更记录
        addBatchAssetChangeItemByAdvance(batchAssetChange.getId(), titularBorrowAccount.getUserId(), borrowRepayment, parentBorrow, lateInterest, groupSeqNo);
        // 存管系统登记垫付
        resp = newAdvanceProcess(parentBorrow, borrowRepayment, batchAssetChange, lateInterest, lateDays, freezeOrderId, acqResMap, titularBorrowAccount, groupSeqNo, batchNo);
        if (resp.getBody().getState().getCode() != VoBaseResp.OK) {
            return resp;
        }

        return ResponseEntity.ok(VoBaseResp.ok("垫付成功!"));
    }

    /**
     * 生成债权转让记录
     *
     * @param borrow
     * @param tenderList
     * @param borrowCollectionMaps
     * @param titularBorrowUserId
     */
    private void addTransferTenderByAdvance(Borrow borrow, List<Transfer> transferList, List<TransferBuyLog> transferBuyLogList, List<Tender> tenderList, Map<Long/* tenderId */, BorrowCollection> borrowCollectionMaps, long titularBorrowUserId) {
        //创建债权转让信息，生成债权转让购买记录
        Map<Long/* 投资id */, Transfer> transferMaps = transferList.stream().collect(Collectors.toMap(Transfer::getTenderId, Function.identity()));
        Map<Long/* 债权转让id */, TransferBuyLog> transferBuyLogMaps = transferBuyLogList.stream().collect(Collectors.toMap(TransferBuyLog::getTransferId, Function.identity()));


        tenderList.stream().forEach(tender -> {
            /* 债权转让转让是否在存管系统登记成功 */
            boolean flag = false;
            Transfer transfer = transferMaps.get(tender.getId());
            if (!ObjectUtils.isEmpty(transfer)) {
                TransferBuyLog transferBuyLog = transferBuyLogMaps.get(transfer.getId());
                if (transferBuyLog.getThirdTransferFlag()) {
                    flag = true;
                }
            }
            if (flag) {
                return;
            }
            /* 回款记录 */
            BorrowCollection borrowCollection = borrowCollectionMaps.get(tender.getId());
            //计算当期应计利息
            long interest = borrowCollection.getInterest();/* 当期理论应计利息 */
            long principal = borrowCollection.getPrincipal();/* 当期应还本金 */
            Date startAt = DateHelper.beginOfDate(borrowCollection.getStartAt());//理论开始计息时间
            Date collectionAt = DateHelper.beginOfDate(borrowCollection.getCollectionAt());//理论结束还款时间
            Date startAtYes = DateHelper.beginOfDate(borrowCollection.getStartAtYes());//实际开始计息时间
            Date endAt = DateHelper.beginOfDate(new Date());//结束计息时间

            /* 当期应计利息 */
            long alreadyInterest = new Double(MoneyHelper.round(interest *
                    Math.max(DateHelper.diffInDays(endAt, startAtYes, false), 0) /
                    DateHelper.diffInDays(collectionAt, startAt, false), 0)).longValue();
            //新增债权转让记录
            Date nowDate = new Date();
            transfer = new Transfer();
            transfer.setUpdatedAt(nowDate);
            transfer.setType(2);
            transfer.setUserId(tender.getUserId());
            transfer.setTransferMoney(principal + alreadyInterest);
            transfer.setTransferMoneyYes(1l);
            transfer.setVerifyAt(nowDate);
            transfer.setSuccessAt(nowDate);
            transfer.setDel(false);
            transfer.setBorrowId(borrow.getId());
            transfer.setPrincipal(principal);
            transfer.setAlreadyInterest(alreadyInterest);
            transfer.setApr(borrow.getApr());
            transfer.setCreatedAt(nowDate);
            transfer.setTimeLimit(borrow.getTimeLimit());/* 垫付是全部期限 */
            transfer.setLowest(1000 * 100L);
            transfer.setState(1);
            transfer.setTenderCount(0);
            transfer.setTenderId(tender.getId());
            transfer.setStartOrder(borrowCollection.getOrder());
            transfer.setEndOrder(borrowCollection.getOrder());
            transfer.setIsAll(true);
            transfer.setBorrowCollectionIds(String.valueOf(borrowCollection.getId()));
            transfer.setTitle(borrow.getName() + "流转");
            transfer.setRepayAt(collectionAt);
            transferList.add(transfer);
            transferService.save(transfer);
            //生成债权购买记录
            TransferBuyLog transferBuyLog = new TransferBuyLog();
            transferBuyLog.setUserId(titularBorrowUserId);
            transferBuyLog.setType(2);
            transferBuyLog.setState(0);
            transferBuyLog.setAuto(false);
            transferBuyLog.setBuyMoney(principal + alreadyInterest);
            transferBuyLog.setValidMoney(principal + alreadyInterest);
            transferBuyLog.setPrincipal(principal);
            transferBuyLog.setCreatedAt(nowDate);
            transferBuyLog.setUpdatedAt(nowDate);
            transferBuyLog.setDel(false);
            transferBuyLog.setAutoOrder(0);
            transferBuyLog.setTransferId(transfer.getId());
            transferBuyLog.setAlreadyInterest(NumberHelper.toLong(alreadyInterest));
            transferBuyLog.setSource(0);
            transferBuyLogList.add(transferBuyLog);

            //改变tender状态
            tender.setTransferFlag(1);
            tender.setUpdatedAt(nowDate);
        });
        transferBuyLogService.save(transferBuyLogList);
    }

    /**
     * 新垫付流程
     *
     * @param borrow
     * @param borrowRepayment
     * @param batchAssetChange
     * @param lateInterest
     * @param lateDays
     * @param groupSeqNo
     * @throws Exception
     */
    private ResponseEntity<VoBaseResp> newAdvanceProcess(Borrow borrow,
                                                         BorrowRepayment borrowRepayment,
                                                         BatchAssetChange batchAssetChange,
                                                         long lateInterest,
                                                         int lateDays,
                                                         String freezeOrderId,
                                                         Map<String, Object> acqResMap,
                                                         UserThirdAccount titularBorrowAccount,
                                                         String groupSeqNo,
                                                         String batchNo) throws Exception {
        log.info("垫付流程: 进入新的垫付流程");
        /* 查询投资列表 */
        Specification<Tender> specification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(specification);
        Preconditions.checkNotNull(tenderList, "投资人投标信息不存在!");
        /* 投资记录id集合 */
        List<Long> tenderIds = tenderList.stream().map(tender -> tender.getId()).collect(Collectors.toList());
        /* 查询未转让的回款记录 */
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIds.toArray())
                .eq("status", 0)
                .eq("order", borrowRepayment.getOrder())
                .eq("transferFlag", 0)
                .build();
        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull("投资回款记录不存在!");
        Map<Long/* tenderId */, BorrowCollection> borrowCollectionMaps = borrowCollectionList.stream().collect(Collectors.toMap(BorrowCollection::getTenderId, Function.identity()));
        Date nowDate = new Date();
        //垫付资产改变集合
        List<AdvanceAssetChange> advanceAssetChangeList = new ArrayList<>();
        //垫付记录 已经存在的债权转让记录
        Specification<Transfer> transferSpecification = Specifications
                .<Transfer>and()
                .in("tenderId", tenderIds.toArray())
                .eq("type", 2)
                .eq("state", 1)
                .build();
        List<Transfer> transferList = transferService.findList(transferSpecification);
        //垫付记录 已经存在购买债权转让记录
        List<TransferBuyLog> transferBuyLogList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(transferList)) {
            List<Long> transferIds = transferList.stream().map(Transfer::getId).collect(Collectors.toList());
            Specification<TransferBuyLog> tbls = Specifications
                    .<TransferBuyLog>and()
                    .in("transferId", transferIds.toArray())
                    .in("state", 0, 1)/* 购买中、成功购买 */
                    .build();
            transferBuyLogList = transferBuyLogService.findList(tbls);
        }
        // 生成债权转让记录
        addTransferTenderByAdvance(borrow, transferList, transferBuyLogList, tenderList, borrowCollectionMaps, titularBorrowAccount.getUserId());

        Map<Long, Transfer> transferMaps = transferList.stream().collect(Collectors.toMap(Transfer::getTenderId, Function.identity()));
        Map<Long, TransferBuyLog> transferBuyLogMaps = transferBuyLogList.stream().collect(Collectors.toMap(TransferBuyLog::getTransferId, Function.identity()));
        //获取名义借款人垫付记录
        List<CreditInvest> creditInvestList = calculateAdvancePlan(borrow, titularBorrowAccount, transferMaps, transferBuyLogMaps, tenderList, borrowCollectionMaps, advanceAssetChangeList, lateDays, lateInterest);
        Preconditions.checkNotNull(creditInvestList, "存管垫付记录不存在!");
        // 生成还款记录
        doGenerateAssetChangeRecodeByAdvance(borrow, borrowRepayment, advanceAssetChangeList, batchAssetChange, titularBorrowAccount, assetChangeProvider.getSeqNo(), groupSeqNo);
        // 垫付金额 = sum(垫付本金 + 垫付利息)
        double txAmount = creditInvestList.stream().mapToDouble(w -> NumberHelper.toDouble(w.getTxAmount())).sum();
        try {

            // 垫付还款冻结
            long frozenMoney = new Double(MoneyHelper.multiply(txAmount, 100)).longValue();
            AssetChange freezeAssetChange = new AssetChange();
            freezeAssetChange.setSourceId(borrowRepayment.getId());
            freezeAssetChange.setGroupSeqNo(groupSeqNo);
            freezeAssetChange.setSeqNo(assetChangeProvider.getSeqNo());
            freezeAssetChange.setMoney(frozenMoney);
            freezeAssetChange.setUserId(titularBorrowAccount.getUserId());
            freezeAssetChange.setRemark(String.format("垫付还款,冻结资金%s元", StringHelper.formatDouble(MoneyHelper.divide(frozenMoney, 100D), true)));
            freezeAssetChange.setSourceId(borrowRepayment.getId());
            freezeAssetChange.setType(AssetChangeTypeEnum.freeze);
            assetChangeProvider.commonAssetChange(freezeAssetChange);

            BatchCreditInvestReq request = new BatchCreditInvestReq();
            request.setChannel(ChannelContant.HTML);
            request.setBatchNo(batchNo);
            request.setTxAmount(StringHelper.formatDouble(txAmount, false));
            request.setTxCounts(StringHelper.toString(creditInvestList.size()));
            request.setNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/advance/check");
            request.setRetNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/advance/run");
            request.setAcqRes(GSON.toJson(acqResMap));
            request.setSubPacks(GSON.toJson(creditInvestList));
            BatchCreditEndResp response = jixinManager.send(JixinTxCodeEnum.BATCH_CREDIT_INVEST, request, BatchCreditEndResp.class);
            if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
                return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "批次名义借款人垫付失败!"));
            }

            //记录日志
            ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
            thirdBatchLog.setBatchNo(batchNo);
            thirdBatchLog.setCreateAt(nowDate);
            thirdBatchLog.setUpdateAt(nowDate);
            thirdBatchLog.setSourceId(borrowRepayment.getId());
            thirdBatchLog.setType(ThirdBatchLogContants.BATCH_BAIL_REPAY);
            thirdBatchLog.setRemark("批次名义借款人垫付");
            thirdBatchLog.setAcqRes(GSON.toJson(acqResMap));
            thirdBatchLogService.save(thirdBatchLog);
        } catch (Exception e) {
            // 申请即信还款解冻
            String unfreezeOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_UNFREEZE_PREFIX);
            BalanceUnfreezeReq balanceUnfreezeReq = new BalanceUnfreezeReq();
            balanceUnfreezeReq.setAccountId(titularBorrowAccount.getAccountId());
            balanceUnfreezeReq.setTxAmount(StringHelper.formatDouble(txAmount, false));
            balanceUnfreezeReq.setOrderId(unfreezeOrderId);
            balanceUnfreezeReq.setOrgOrderId(freezeOrderId);
            balanceUnfreezeReq.setChannel(ChannelContant.HTML);
            BalanceUnfreezeResp balanceUnfreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_UN_FREEZE, balanceUnfreezeReq, BalanceUnfreezeResp.class);
            if ((ObjectUtils.isEmpty(balanceUnfreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceUnfreezeResp.getRetCode()))) {
                throw new Exception("名义借款人垫付解冻异常：" + balanceUnfreezeResp.getRetMsg());
            }
        }
        return ResponseEntity.ok(VoBaseResp.ok("批次名义借款人垫付成功!"));
    }

    /**
     * 生成名义借款人垫付记录
     *
     * @param borrow
     * @param titularBorrowAccount
     * @param tenderList
     * @param borrowCollectionMaps
     * @param advanceAssetChanges
     * @param lateDays
     * @param lateInterest
     * @return
     * @throws Exception
     */
    public List<CreditInvest> calculateAdvancePlan(Borrow borrow, UserThirdAccount titularBorrowAccount, Map<Long, Transfer> transferMaps, Map<Long, TransferBuyLog> transferBuyLogMaps,
                                                   List<Tender> tenderList, Map<Long/* tenderId */, BorrowCollection> borrowCollectionMaps, List<AdvanceAssetChange> advanceAssetChanges,
                                                   int lateDays, long lateInterest) throws Exception {

        /* 垫付记录集合 */
        List<CreditInvest> creditInvestList = new ArrayList<>();

        long txAmount = 0;/* 垫付金额 = 垫付本金 + 垫付利息 */
        long intAmount = 0;/* 交易利息 */
        long principal = 0;/* 还款本金 */
        for (Tender tender : tenderList) {
            Transfer transfer = transferMaps.get(tender.getId());
            TransferBuyLog transferBuyLog = transferBuyLogMaps.get(transfer.getId());

            //投标人银行存管账户
            UserThirdAccount tenderUserThirdAccount = userThirdAccountService.findByUserId(tender.getUserId());
            Preconditions.checkNotNull(tenderUserThirdAccount, "投资人存管账户未开户!");
            //当前投资的回款记录
            BorrowCollection borrowCollection = borrowCollectionMaps.get(tender.getId());

            //判断这笔回款是否已经在即信登记过批次垫付
            if (BooleanHelper.isTrue(transferBuyLog.getThirdTransferFlag())) {
                continue;
            }
            if (tender.getTransferFlag() == 2) {  // 已经转让的债权, 可以跳过还款
                continue;
            }

            //垫付资金变动
            AdvanceAssetChange advanceAssetChange = new AdvanceAssetChange();
            advanceAssetChanges.add(advanceAssetChange);

            intAmount = borrowCollection.getInterest();//还款利息
            principal = borrowCollection.getPrincipal(); //还款本金
            advanceAssetChange.setUserId(borrowCollection.getUserId());
            advanceAssetChange.setInterest(intAmount);
            advanceAssetChange.setPrincipal(principal);
            advanceAssetChange.setBorrowCollection(borrowCollection);

            if ((lateDays > 0) && (lateInterest > 0)) {  //借款人逾期罚息
                int overdueFee = new Double(tender.getValidMoney() / new Double(borrow.getMoney()) * lateInterest / 2D).intValue();// 出借人收取50% 逾期管理费 ;
                advanceAssetChange.setOverdueFee(overdueFee);
                intAmount += overdueFee;
            }
            /* 垫付金额 */
            txAmount = principal + intAmount; //= 垫付本金 + 垫付利息
            String orderId = JixinHelper.getOrderId(JixinHelper.REPAY_BAIL_PREFIX);
            /* 存管垫付记录 */
            CreditInvest creditInvest = new CreditInvest();
            creditInvest.setAccountId(titularBorrowAccount.getAccountId());
            creditInvest.setOrderId(orderId);
            creditInvest.setTxAmount(StringHelper.formatDouble(txAmount, 100, false));
            creditInvest.setTxFee("0");
            creditInvest.setTsfAmount(StringHelper.formatDouble(principal, 100, false));
            creditInvest.setForAccountId(tenderUserThirdAccount.getAccountId());
            creditInvest.setOrgOrderId(tender.getThirdTenderOrderId());
            creditInvest.setOrgTxAmount(StringHelper.formatDouble(tender.getValidMoney(), 100, false));
            creditInvest.setProductId(borrow.getProductId());
            creditInvest.setContOrderId(tenderUserThirdAccount.getAutoTransferBondOrderId());
            creditInvestList.add(creditInvest);
            //更新回款记录
            transferBuyLog.setThirdTransferOrderId(orderId);
            transferBuyLog.setUpdatedAt(new Date());
            transferBuyLogService.save(transferBuyLog);

        }
        return creditInvestList;
    }

    /**
     * 生成还款记录
     *
     * @param borrow
     * @param borrowRepayment
     * @param advanceAsserChangeList
     * @param batchAssetChange
     * @param titularBorrowAccount
     * @param seqNo
     * @param groupSeqNo
     */
    private void doGenerateAssetChangeRecodeByAdvance(Borrow borrow, BorrowRepayment borrowRepayment, List<AdvanceAssetChange> advanceAsserChangeList, BatchAssetChange batchAssetChange, UserThirdAccount titularBorrowAccount, String seqNo, String groupSeqNo) throws ExecutionException {
        long batchAssetChangeId = batchAssetChange.getId();
        Date nowDate = new Date();

        for (AdvanceAssetChange advanceAssetChange : advanceAsserChangeList) {
            /* 回款记录 */
            BorrowCollection borrowCollection = advanceAssetChange.getBorrowCollection();

            BatchAssetChangeItem batchAssetChangeItem = new BatchAssetChangeItem();   // 还款本金和利息
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.receivedPayments.getLocalType());  // 投资人收到还款
            batchAssetChangeItem.setUserId(advanceAssetChange.getUserId());
            batchAssetChangeItem.setForUserId(titularBorrowAccount.getUserId());  // 垫付人
            batchAssetChangeItem.setMoney(advanceAssetChange.getPrincipal() + advanceAssetChange.getInterest());   // 本金加利息
            batchAssetChangeItem.setInterest(advanceAssetChange.getInterest());  // 利息
            batchAssetChangeItem.setRemark(String.format("收到客户对借款[%s]第%s期的还款", borrow.getName(), (borrowRepayment.getOrder() + 1)));
            batchAssetChangeItem.setSourceId(borrowCollection.getId());
            batchAssetChangeItem.setSeqNo(seqNo);
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);

            if (advanceAssetChange.getOverdueFee() > 0 && new Date().getTime() > DateHelper.endOfDate(DateHelper.stringToDate("2017-09-03 23:59:59")).getTime()) {  // 用户收取逾期管理费
                batchAssetChangeItem = new BatchAssetChangeItem();
                batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
                batchAssetChangeItem.setState(0);
                batchAssetChangeItem.setType(AssetChangeTypeEnum.receivedPaymentsPenalty.getLocalType());  // 收取用户逾期费
                batchAssetChangeItem.setUserId(advanceAssetChange.getUserId());
                batchAssetChangeItem.setForUserId(titularBorrowAccount.getUserId());
                batchAssetChangeItem.setMoney(advanceAssetChange.getOverdueFee());
                batchAssetChangeItem.setRemark(String.format("收取借款标的[%s]滞纳金%s元", borrow.getName(), StringHelper.formatDouble(advanceAssetChange.getOverdueFee() / 100D, false)));
                batchAssetChangeItem.setCreatedAt(nowDate);
                batchAssetChangeItem.setUpdatedAt(nowDate);
                batchAssetChangeItem.setSourceId(borrowRepayment.getId());
                batchAssetChangeItem.setSeqNo(seqNo);
                batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
                batchAssetChangeItemService.save(batchAssetChangeItem);
            }

            //扣除投资人待收
            batchAssetChangeItem = new BatchAssetChangeItem();
            batchAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            batchAssetChangeItem.setState(0);
            batchAssetChangeItem.setType(AssetChangeTypeEnum.collectionSub.getLocalType());  //  扣除投资人待收
            batchAssetChangeItem.setUserId(advanceAssetChange.getUserId());
            batchAssetChangeItem.setMoney(advanceAssetChange.getInterest() + advanceAssetChange.getPrincipal());
            batchAssetChangeItem.setInterest(advanceAssetChange.getInterest());
            batchAssetChangeItem.setRemark(String.format("收到客户对[%s]借款的还款,扣除待收", borrow.getName()));
            batchAssetChangeItem.setCreatedAt(nowDate);
            batchAssetChangeItem.setUpdatedAt(nowDate);
            batchAssetChangeItem.setSourceId(borrowRepayment.getId());
            batchAssetChangeItem.setSeqNo(seqNo);
            batchAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(batchAssetChangeItem);
        }
    }

    /**
     * 新增垫付记录与更改还款记录
     *
     * @param bailAccountId
     * @param borrowRepayment
     * @param lateDays
     * @param lateInterest
     */
    private void addAdvanceLogAndChangeBorrowRepayment(long bailAccountId, BorrowRepayment borrowRepayment, int lateDays, long lateInterest) {
        //新增垫付记录
        AdvanceLog advanceLog = new AdvanceLog();
        advanceLog.setUserId(bailAccountId);
        advanceLog.setRepaymentId(borrowRepayment.getId());
        advanceLog.setAdvanceAtYes(new Date());
        advanceLog.setAdvanceMoneyYes((borrowRepayment.getRepayMoney() + lateInterest));
        advanceLogService.save(advanceLog);
        //更改付款记录
        borrowRepayment.setLateDays(lateDays);
        borrowRepayment.setLateInterest(lateInterest);
        borrowRepayment.setAdvanceAtYes(new Date());
        borrowRepayment.setAdvanceMoneyYes((borrowRepayment.getRepayMoney() + lateInterest));
        borrowRepaymentService.updateById(borrowRepayment);
    }

    /**
     * 生成名义借款人垫付批次资产变更记录
     *
     * @param batchAssetChangeId
     * @param bailAccountId
     * @param borrowRepayment
     * @param parentBorrow
     * @param lateInterest
     * @param groupSeqNo
     */
    private void addBatchAssetChangeItemByAdvance(long batchAssetChangeId, long bailAccountId, BorrowRepayment borrowRepayment,
                                                  Borrow parentBorrow, Long lateInterest, String groupSeqNo) {
        Date nowDate = new Date();
        // 名义借款人垫付还款
        BatchAssetChangeItem advanceBailAssetChangeItem = new BatchAssetChangeItem();
        advanceBailAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
        advanceBailAssetChangeItem.setState(0);
        advanceBailAssetChangeItem.setType(AssetChangeTypeEnum.compensatoryRepayment.getLocalType());  // 名义借款人垫付还款
        advanceBailAssetChangeItem.setUserId(bailAccountId);
        advanceBailAssetChangeItem.setMoney(borrowRepayment.getRepayMoney());
        advanceBailAssetChangeItem.setRemark(String.format("对借款[%s]第%s期的垫付还款", parentBorrow.getName(), DateHelper.dateToString(new Date())));
        advanceBailAssetChangeItem.setCreatedAt(nowDate);
        advanceBailAssetChangeItem.setUpdatedAt(nowDate);
        advanceBailAssetChangeItem.setSourceId(borrowRepayment.getId());
        advanceBailAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
        advanceBailAssetChangeItem.setGroupSeqNo(groupSeqNo);
        batchAssetChangeItemService.save(advanceBailAssetChangeItem);

        if (lateInterest > 0 && new Date().getTime() > DateHelper.endOfDate(DateHelper.stringToDate("2017-09-03 23:59:59")).getTime()) {
            BatchAssetChangeItem overdueAssetChangeItem = new BatchAssetChangeItem();  // 滞纳金
            overdueAssetChangeItem.setBatchAssetChangeId(batchAssetChangeId);
            overdueAssetChangeItem.setState(0);
            overdueAssetChangeItem.setType(AssetChangeTypeEnum.compensatoryRepaymentOverdueFee.getLocalType());  // 名义借款人垫付还款
            overdueAssetChangeItem.setUserId(bailAccountId);
            overdueAssetChangeItem.setMoney(new Double(MoneyHelper.divide(lateInterest.doubleValue(), 2D)).longValue());
            overdueAssetChangeItem.setRemark(String.format("对借款[%s]第%s期的垫付滞纳金", parentBorrow.getName(), (borrowRepayment.getOrder() + 1)));
            overdueAssetChangeItem.setCreatedAt(nowDate);
            overdueAssetChangeItem.setUpdatedAt(nowDate);
            overdueAssetChangeItem.setSourceId(borrowRepayment.getId());
            overdueAssetChangeItem.setSeqNo(assetChangeProvider.getSeqNo());
            overdueAssetChangeItem.setGroupSeqNo(groupSeqNo);
            batchAssetChangeItemService.save(overdueAssetChangeItem);
        }
    }

    /**
     * 生成垫付还款主记录
     *
     * @param repaymentId
     * @param batchNo
     */
    private BatchAssetChange addBatchAssetChangeByAdvance(long repaymentId, String batchNo) {
        BatchAssetChange batchAssetChange = new BatchAssetChange();
        batchAssetChange.setSourceId(repaymentId);
        batchAssetChange.setState(0);
        batchAssetChange.setType(BatchAssetChangeContants.BATCH_BAIL_REPAY);/* 名义借款人垫付 */
        batchAssetChange.setCreatedAt(new Date());
        batchAssetChange.setUpdatedAt(new Date());
        batchAssetChange.setBatchNo(batchNo);
        batchAssetChangeService.save(batchAssetChange);
        return batchAssetChange;
    }

    /**
     * 新版垫付处理
     *
     * @param repaymentId
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> newAdvanceDeal(long repaymentId, String batchNo) throws Exception {
        //1.查询判断还款记录是否存在
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);/* 当期还款记录 */
        Preconditions.checkNotNull(borrowRepayment, "还款记录不存在!");
        Borrow parentBorrow = borrowService.findById(borrowRepayment.getBorrowId());/* 还款记录对应的借款记录 */
        Preconditions.checkNotNull(parentBorrow, "借款记录不存在!");
        /* 还款对应的投标记录  包括债权转让在里面 */
        Specification<Tender> ts = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", parentBorrow.getId())
                .build();
        List<Tender> tenderList = tenderService.findList(ts);/* 还款对应的投标记录  包括债权转让在里面 */
        Preconditions.checkNotNull(tenderList, "立即还款: 投标记录为空!");
        Map<Long, Tender> tenderMaps = tenderList.stream().collect(Collectors.toMap(Tender::getId, Function.identity()));
        /* 投标记录id */
        Set<Long> tenderIds = tenderList.stream().map(tender -> tender.getId()).collect(Collectors.toSet());
        /* 查询未转让的投标记录回款记录 */
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIds.toArray())
                .eq("status", 0)
                .eq("order", borrowRepayment.getOrder())
                .eq("transferFlag", 0)
                .build();
        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull(borrowCollectionList, "立即还款: 回款记录为空!");
        /* 是否垫付 */
        boolean advance = !ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes());
        //2.处理资金还款人、收款人资金变动
        batchAssetChangeHelper.batchAssetChangeAndCollection(repaymentId, batchNo, BatchAssetChangeContants.BATCH_BAIL_REPAY);
        /* 逾期天数 */
        int lateDays = getLateDays(borrowRepayment);
        long lateInterest = new Double(calculateLateInterest(lateDays, borrowRepayment, parentBorrow) / 2D).longValue();/* 获取逾期利息的一半*/
        long titularBorrowUserId = assetChangeProvider.getTitularBorrowUserId();  // 平台担保人ID
        //3.新增垫付记录与更改还款状态
        addAdvanceLogAndChangeBorrowRepayment(titularBorrowUserId, borrowRepayment, lateDays, lateInterest);
        //3.5完成垫付债权转让操作
        transferTenderByAdvance(parentBorrow, tenderMaps, tenderIds, borrowRepayment);
        //5.发送投资人收到还款站内信
        sendCollectionNotices(borrowCollectionList, advance, parentBorrow);
        //6.发放积分
        giveInterest(borrowCollectionList, parentBorrow);
        //7.还款最后新增统计
        updateRepaymentStatistics(parentBorrow, borrowRepayment);
        //8.更新投资人缓存
        updateUserCacheByReceivedRepay(borrowCollectionList, parentBorrow);
        //9.项目回款短信通知
        smsNoticeByReceivedRepay(borrowCollectionList, parentBorrow, borrowRepayment);
        //10修改垫付原回款状态
        updateCollectionByAdvance(borrowCollectionList);

        return ResponseEntity.ok(VoBaseResp.ok("垫付处理成功!"));
    }

    /**
     * 更新垫付回款记录状态
     *
     * @param borrowCollectionList
     */
    private void updateCollectionByAdvance(List<BorrowCollection> borrowCollectionList) {
        borrowCollectionList.stream().forEach(borrowCollection -> {
            borrowCollection.setCollectionMoney(borrowCollection.getCollectionMoney());
            borrowCollection.setUpdatedAt(new Date());
            borrowCollection.setStatus(1);
            borrowCollection.setCollectionAtYes(new Date());
            borrowCollection.setCollectionMoneyYes(borrowCollection.getLateInterest() + borrowCollection.getCollectionMoney());
        });
        borrowCollectionService.save(borrowCollectionList);
    }

    /**
     * 5完成垫付债权转让操作
     *
     * @param tenderMaps
     * @param tenderIds
     */
    public void transferTenderByAdvance(Borrow parentBorrow, Map<Long, Tender> tenderMaps, Set<Long> tenderIds, BorrowRepayment borrowRepayment) {
        /* 查询债权转让记录 */
        Specification<Transfer> ts = Specifications
                .<Transfer>and()
                .in("tenderId", tenderIds.toArray())
                .eq("state", 1)
                .eq("type", 2)
                .build();
        List<Transfer> transferList = transferService.findList(ts);
        Preconditions.checkState(!CollectionUtils.isEmpty(transferList), "转让记录不存在!");
        /* 债权转让id */
        List<Long> transferIds = transferList.stream().map(Transfer::getId).collect(Collectors.toList());
        Specification<TransferBuyLog> tbls = Specifications
                .<TransferBuyLog>and()
                .in("transferId", transferIds.toArray())
                .build();
        /* 债权转让购买记录 */
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls);
        Map<Long, List<TransferBuyLog>> transferBuyLogMaps = transferBuyLogList.stream().collect(groupingBy(TransferBuyLog::getTransferId));

        Preconditions.checkState(!CollectionUtils.isEmpty(transferBuyLogList), "购买债权转让记录不存在!");
        // 新增子级投标记录,更新老债权记录
        Date nowDate = new Date();
        transferList.stream().forEach(transfer -> {
            Tender parentTender = tenderMaps.get(transfer.getTenderId());
            List<TransferBuyLog> transferBuyLogs = transferBuyLogMaps.get(transfer.getId());
            List<Tender> childTenderList = transferBiz.addChildTender(nowDate, transfer, parentTender, transferBuyLogs);
            // 生成子级债权回款记录，标注老债权回款已经转出
            try {
                transferBiz.addChildTenderCollection(nowDate, transfer, parentBorrow, childTenderList);
            } catch (Exception e) {
                log.error("repaymentBizImpl updateTransferTenderByAdvance error", e);
            }
            if ((parentBorrow.getTotalOrder() - 1) == borrowRepayment.getOrder().intValue()) {
                parentTender.setState(3);
            }
        });

    }
}
