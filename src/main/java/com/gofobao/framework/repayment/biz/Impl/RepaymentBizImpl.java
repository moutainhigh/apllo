package com.gofobao.framework.repayment.biz.Impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.api.contants.ChannelContant;
import com.gofobao.framework.api.contants.DesLineFlagContant;
import com.gofobao.framework.api.contants.JixinResultContants;
import com.gofobao.framework.api.helper.JixinManager;
import com.gofobao.framework.api.helper.JixinTxCodeEnum;
import com.gofobao.framework.api.model.balance_freeze.BalanceFreezeReq;
import com.gofobao.framework.api.model.balance_freeze.BalanceFreezeResp;
import com.gofobao.framework.api.model.batch_bail_repay.BailRepay;
import com.gofobao.framework.api.model.batch_bail_repay.BailRepayRun;
import com.gofobao.framework.api.model.batch_bail_repay.BatchBailRepayReq;
import com.gofobao.framework.api.model.batch_bail_repay.BatchBailRepayResp;
import com.gofobao.framework.api.model.batch_repay.BatchRepayReq;
import com.gofobao.framework.api.model.batch_repay.BatchRepayResp;
import com.gofobao.framework.api.model.batch_repay.Repay;
import com.gofobao.framework.api.model.batch_repay_bail.BatchRepayBailReq;
import com.gofobao.framework.api.model.batch_repay_bail.BatchRepayBailResp;
import com.gofobao.framework.api.model.batch_repay_bail.RepayBail;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayRequest;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayResponse;
import com.gofobao.framework.asset.entity.AdvanceLog;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.service.AdvanceLogService;
import com.gofobao.framework.asset.service.AssetService;
import com.gofobao.framework.borrow.biz.BorrowBiz;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.repository.BorrowRepository;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.borrow.vo.request.VoCancelBorrow;
import com.gofobao.framework.collection.entity.BorrowCollection;
import com.gofobao.framework.collection.service.BorrowCollectionService;
import com.gofobao.framework.collection.vo.request.VoCollectionListReq;
import com.gofobao.framework.collection.vo.request.VoCollectionOrderReq;
import com.gofobao.framework.collection.vo.response.VoViewCollectionDaysWarpRes;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderListWarpResp;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderRes;
import com.gofobao.framework.common.capital.CapitalChangeEntity;
import com.gofobao.framework.common.capital.CapitalChangeEnum;
import com.gofobao.framework.common.constans.JixinContants;
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
import com.gofobao.framework.helper.project.BorrowHelper;
import com.gofobao.framework.helper.project.CapitalChangeHelper;
import com.gofobao.framework.helper.project.IntegralChangeHelper;
import com.gofobao.framework.helper.project.SecurityHelper;
import com.gofobao.framework.member.entity.UserCache;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.service.UserCacheService;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.repayment.biz.BorrowRepaymentThirdBiz;
import com.gofobao.framework.repayment.biz.RepaymentBiz;
import com.gofobao.framework.repayment.entity.BorrowRepayment;
import com.gofobao.framework.repayment.service.BorrowRepaymentService;
import com.gofobao.framework.repayment.vo.request.*;
import com.gofobao.framework.repayment.vo.response.*;
import com.gofobao.framework.repayment.vo.response.pc.VoCollection;
import com.gofobao.framework.repayment.vo.response.pc.VoOrdersList;
import com.gofobao.framework.repayment.vo.response.pc.VoViewCollectionWarpRes;
import com.gofobao.framework.repayment.vo.response.pc.VoViewOrderListWarpRes;
import com.gofobao.framework.system.biz.StatisticBiz;
import com.gofobao.framework.system.biz.ThirdBatchLogBiz;
import com.gofobao.framework.system.contants.ThirdBatchLogContants;
import com.gofobao.framework.system.entity.*;
import com.gofobao.framework.system.service.DictItemServcie;
import com.gofobao.framework.system.service.DictValueService;
import com.gofobao.framework.system.service.ThirdBatchLogService;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.service.TenderService;
import com.gofobao.framework.tender.vo.response.VoAutoTenderInfo;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private CapitalChangeHelper capitalChangeHelper;
    @Autowired
    private StatisticBiz statisticBiz;
    @Autowired
    private TenderService tenderService;
    @Autowired
    private UserCacheService userCacheService;
    @Autowired
    private BorrowCollectionService borrowCollectionService;
    @Autowired
    private BorrowBiz borrowBiz;
    @Autowired
    private MqHelper mqHelper;
    @Autowired
    private IntegralChangeHelper integralChangeHelper;

    @Autowired
    private BorrowRepaymentService borrowRepaymentService;
    @Autowired
    private AdvanceLogService advanceLogService;
    @Autowired
    private BorrowRepository borrowRepository;
    @Autowired
    private DictItemServcie dictItemServcie;
    @Autowired
    private ThirdBatchLogService thirdBatchLogService;
    @Autowired
    private ThirdBatchLogBiz thirdBatchLogBiz;
    @Autowired
    private JixinHelper jixinHelper;
    @Autowired
    private DictValueService dictValueService;
    @Autowired
    private BorrowRepaymentThirdBiz borrowRepaymentThirdBiz;

    @Value("${gofobao.webDomain}")
    private String webDomain;

    @Value("${gofobao.javaDomain}")
    private String javaDomain;


    LoadingCache<String, DictValue> jixinCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build(new CacheLoader<String, DictValue>() {
                @Override
                public DictValue load(String bankName) throws Exception {
                    DictItem dictItem = dictItemServcie.findTopByAliasCodeAndDel("JIXIN_PARAM", 0);
                    if (ObjectUtils.isEmpty(dictItem)) {
                        return null;
                    }

                    return dictValueService.findTopByItemIdAndValue01(dictItem.getId(), bankName);
                }
            });
    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private JixinManager jixinManager;


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
                collectionOrderRes.setCollectionMoneyYes(StringHelper.formatMon(p.getRepayMoneyYes() / 100d));
                collectionOrderRes.setCollectionMoney(StringHelper.formatMon(p.getRepayMoney() / 100d));
                collectionOrderRes.setTimeLime(borrow.getTimeLimit());
                orderResList.add(collectionOrderRes);
            });

            VoViewCollectionOrderListWarpResp collectionOrder = VoBaseResp.ok("查询成功", VoViewCollectionOrderListWarpResp.class);
            collectionOrder.setOrderResList(orderResList);
            //总数
            collectionOrder.setOrder(orderResList.size());
            //已还款
            Integer moneyYesSum = repaymentList.stream()
                    .filter(p -> p.getStatus() == 1)
                    .mapToInt(w -> w.getRepayMoneyYes())
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
     * 前置判断
     *
     * @param voRepayReq
     * @return
     */
    private ResponseEntity<VoBaseResp> checkRepay(VoRepayReq voRepayReq) {
        int lateInterest = 0;// 逾期利息
        Double interestPercent = voRepayReq.getInterestPercent();
        Long userId = voRepayReq.getUserId();
        Long repaymentId = voRepayReq.getRepaymentId();
        interestPercent = (ObjectUtils.isEmpty(interestPercent) || interestPercent == 0) ? 1 : interestPercent;
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
        Preconditions.checkNotNull(borrowRepayment, "还款不存在!");
        if (borrowRepayment.getStatus() != 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款状态已发生改变!")));
        }

        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());
        int borrowType = borrow.getType();//借款type
        Long borrowUserId = borrow.getUserId();
        Asset borrowUserAsset = assetService.findByUserIdLock(borrowUserId);
        Preconditions.checkNotNull(borrowRepayment, "用户资产查询失败!");
        if ((!ObjectUtils.isEmpty(userId))
                && (!StringHelper.toString(borrowUserId).equals(StringHelper.toString(userId)))) {   // 存在userId时 判断是否是当前用户
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("操作用户不是借款用户!")));
        }

        //===================================================================
        //检查还款账户是否完成存管操作  与  完成必需操作
        //===================================================================
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        if (ObjectUtils.isEmpty(userThirdAccount)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_INIT_BANK_PASSWORD, "还款会员未开户！", VoAutoTenderInfo.class));
        }

        if (userThirdAccount.getPasswordState() != 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_INIT_BANK_PASSWORD, "请初始化江西银行存管账户密码！", VoAutoTenderInfo.class));
        }

        if (userThirdAccount.getAutoTransferState() != 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_CREDIT, "请先签订自动债权转让协议！", VoAutoTenderInfo.class));
        }


        if (userThirdAccount.getAutoTenderState() != 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_CREDIT, "请先签订自动投标协议！", VoAutoTenderInfo.class));
        }


        int repayInterest = (int) (borrowRepayment.getInterest() * interestPercent); //还款利息
        int repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额

        if (borrowType == 2) { // 秒表处理
            if (borrowUserAsset.getNoUseMoney() < (borrowRepayment.getRepayMoney() + lateInterest)) {
                return ResponseEntity
                        .badRequest()
                        .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("账户余额不足，请先充值!")));
            }
        } else {
            if (borrowUserAsset.getUseMoney() < MathHelper.myRound(repayMoney + lateInterest, 2)) {
                return ResponseEntity
                        .badRequest()
                        .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("账户余额不足，请先充值!")));
            }
        }

        //判断提交还款批次是否多次重复提交
        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(repaymentId), ThirdBatchLogContants.BATCH_REPAY_BAIL, ThirdBatchLogContants.BATCH_REPAY);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款处理中，请勿重复点击!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            /**
             * @// TODO: 2017/7/18 增加本地查询
             */
        }


        List<BorrowRepayment> borrowRepaymentList = null;
        if (borrowRepayment.getOrder() > 0) {
            Specification<BorrowRepayment> brs = Specifications
                    .<BorrowRepayment>and()
                    .eq("id", repaymentId)
                    .eq("status", 0)
                    .predicate(new LtSpecification<BorrowRepayment>("order", new DataObject(borrowRepayment.getOrder())))
                    .build();
            borrowRepaymentList = borrowRepaymentService.findList(brs);

            if (!CollectionUtils.isEmpty(borrowRepaymentList)) {
                return ResponseEntity
                        .badRequest()
                        .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("该借款上一期还未还!")));
            }
        }
        return null;
    }

    /**
     * 立即还款
     *
     * @param voRepayReq
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> repayDeal(VoRepayReq voRepayReq) throws Exception {


        ResponseEntity resp = checkRepay(voRepayReq);
        if (!ObjectUtils.isEmpty(resp)) {
            return null;
        }
        Date nowDate = new Date();
        int lateInterest = 0;//逾期利息
        Double interestPercent = voRepayReq.getInterestPercent();
        Long repaymentId = voRepayReq.getRepaymentId();
        Boolean isUserOpen = voRepayReq.getIsUserOpen();//是否是用户主动还款
        interestPercent = interestPercent == 0 ? 1 : interestPercent;//回款 利息百分比
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);//还款记录
        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());//借款记录

        Long borrowId = borrow.getId();//借款ID
        int borrowType = borrow.getType();//借款type
        Long borrowUserId = borrow.getUserId();
        int repayInterest = (int) (borrowRepayment.getInterest() * interestPercent);//还款利息
        int repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额

        //逾期天数
        Date nowDateOfBegin = DateHelper.beginOfDate(new Date());
        Date repayDateOfBegin = DateHelper.beginOfDate(borrowRepayment.getRepayAt());
        int lateDays = DateHelper.diffInDays(nowDateOfBegin, repayDateOfBegin, false);
        lateDays = lateDays < 0 ? 0 : lateDays;
        if (0 < lateDays) {
            int overPrincipal = borrowRepayment.getPrincipal();//剩余未还本金
            if (borrowRepayment.getOrder() < (borrow.getTotalOrder() - 1)) {//计算非一次性还本付息 剩余本金

                Specification<BorrowRepayment> brs = Specifications
                        .<BorrowRepayment>and()
                        .eq("borrowId", borrowId)
                        .eq("status", 0)
                        .build();
                List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
                Preconditions.checkNotNull(borrowRepayment, "还款不存在!");

                overPrincipal = 0;
                for (BorrowRepayment temp : borrowRepaymentList) {
                    overPrincipal += temp.getPrincipal();
                }
            }

            lateInterest = (int) MathHelper.myRound(overPrincipal * 0.004 * lateDays, 2);
        }

        CapitalChangeEntity entity = new CapitalChangeEntity();
        entity.setType(CapitalChangeEnum.Repayment);
        entity.setUserId(borrowUserId);
        entity.setMoney(repayMoney);
        entity.setInterest(repayInterest);
        entity.setRemark("对借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (borrowRepayment.getOrder() + 1) + "期的还款");
        if (borrowType == 2) {
            entity.setAsset("sub@no_use_money");
        } else if (interestPercent < 1) {
            entity.setRemark("（提前结清）");
        } else if (!isUserOpen) {
            entity.setRemark("（系统自动还款）");
        }
        try {
            capitalChangeHelper.capitalChange(entity);
        } catch (Throwable e) {
            log.error("立即还款异常:", e);
        }

        //扣除待还
        entity = new CapitalChangeEntity();
        entity.setType(CapitalChangeEnum.PaymentLower);
        entity.setUserId(borrowUserId);
        entity.setMoney(borrowRepayment.getRepayMoney());
        entity.setInterest(borrowRepayment.getInterest());
        entity.setRemark("还款成功扣除待还");
        try {
            capitalChangeHelper.capitalChange(entity);
        } catch (Throwable e) {
            log.error("立即还款异常:", e);
        }

        if ((lateDays > 0) && (lateInterest > 0)) {
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Overdue);
            entity.setUserId(borrowUserId);
            entity.setMoney(lateInterest);
            entity.setRemark("借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]的逾期罚息");
            capitalChangeHelper.capitalChange(entity);
        }

        if (ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes())) {
            receivedReapy(borrow, borrowRepayment.getOrder(), interestPercent, lateDays, lateInterest / 2, false);
        } else {
            AdvanceLog advanceLog = advanceLogService.findByRepaymentId(repaymentId);
            Preconditions.checkNotNull(advanceLog, "垫付记录不存在!请联系客服");

            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.IncomeOther);
            entity.setUserId(advanceLog.getUserId());
            entity.setMoney(repayMoney + lateInterest);
            entity.setRemark("收到客户对借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (borrowRepayment.getOrder() + 1) + "期垫付的还款");
            capitalChangeHelper.capitalChange(entity);
            //更新垫付记录
            advanceLog.setStatus(1);
            advanceLog.setRepayAtYes(new Date());
            advanceLog.setRepayMoneyYes(repayMoney + lateInterest);
            advanceLogService.updateById(advanceLog);
        }

        borrowRepayment.setStatus(1);
        borrowRepayment.setLateDays(NumberHelper.toInt(StringHelper.toString(lateDays)));
        borrowRepayment.setLateInterest(lateInterest);
        borrowRepayment.setRepayAtYes(nowDate);
        borrowRepayment.setRepayMoneyYes(repayMoney);
        borrowRepaymentService.updateById(borrowRepayment);

        //====================================================================
        //结束债权：最后一期还款时
        //====================================================================
        if (borrowRepayment.getOrder() == (borrow.getTotalOrder() - 1)) {
            borrow.setCloseAt(borrowRepayment.getRepayAtYes());

            //推送队列结束债权
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_CREDIT);
            mqConfig.setTag(MqTagEnum.END_CREDIT);
            mqConfig.setSendTime(DateHelper.addMinutes(new Date(), 1));
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.MSG_BORROW_ID, StringHelper.toString(borrowId), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
            mqConfig.setMsg(body);
            try {
                log.info(String.format("repaymentBizImpl repayDeal send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("repaymentBizImpl repayDeal send mq exception", e);
            }
        }
        borrow.setUpdatedAt(nowDate);
        borrowService.updateById(borrow);

        //更新统计数据
        try {
            Statistic statistic = new Statistic();

            statistic.setWaitRepayTotal((long) -repayMoney);
            if (!borrow.isTransfer()) {//判断非转让标
                if (borrow.getType() == 0) {
                    statistic.setTjWaitRepayPrincipalTotal((long) -borrowRepayment.getPrincipal());
                    statistic.setTjWaitRepayTotal((long) -repayMoney);
                } else if (borrow.getType() == 1) {
                    statistic.setJzWaitRepayPrincipalTotal((long) -borrowRepayment.getPrincipal());
                    statistic.setJzWaitRepayTotal((long) -repayMoney);
                } else if (borrow.getType() == 2) {

                } else if (borrow.getType() == 4) {
                    statistic.setQdWaitRepayPrincipalTotal((long) -borrowRepayment.getPrincipal());
                    statistic.setQdWaitRepayTotal((long) -repayMoney);
                }
            }
            if (!ObjectUtils.isEmpty(statistic)) {
                statisticBiz.caculate(statistic);
            }
        } catch (Throwable e) {
            log.error(String.format("立即还款统计错误：", e));
        }
        return ResponseEntity.ok(VoBaseResp.ok("立即还款成功!"));
    }

    /**
     * 收到还款
     *
     * @param borrow
     * @param order
     * @param interestPercent
     * @param lateDays
     * @param lateInterest
     * @param advance
     * @return
     * @throws Exception
     */
    private void receivedReapy(Borrow borrow, int order, double interestPercent, int lateDays, int lateInterest, boolean advance) throws Exception {


        //会员用户集合
        Set<Long> collectionUserIds = new HashSet<>();
        Long borrowId = borrow.getId();

        Specification<Tender> specification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrowId)
                .build();

        List<Tender> tenderList = tenderService.findList(specification);
        Preconditions.checkNotNull(tenderList, "立即还款: 投标记录为空!");

        List<Long> userIds = new ArrayList<>();
        List<Long> tenderIds = new ArrayList<>();
        for (Tender tender : tenderList) {
            userIds.add(tender.getUserId());
            tenderIds.add(tender.getId());
        }

        Specification<UserCache> ucs = Specifications
                .<UserCache>and()
                .in("userId", userIds.toArray())
                .build();

        List<UserCache> userCacheList = userCacheService.findList(ucs);
        Preconditions.checkNotNull(userCacheList, "立即还款: 会员缓存记录为空!");

        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .in("tenderId", tenderIds.toArray())
                .eq("status", 0)
                .eq("order", order)
                .build();

        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
        Preconditions.checkNotNull(userCacheList, "立即还款: 回款记录为空!");

        for (Tender tender : tenderList) {

            UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(tender.getUserId());

            //获取当前借款的回款记录
            BorrowCollection borrowCollection = null;
            for (int i = 0; i < borrowCollectionList.size(); i++) {
                borrowCollection = borrowCollectionList.get(i);
                if (StringHelper.toString(tender.getId()).equals(StringHelper.toString(borrowCollection.getTenderId()))) {
                    break;
                }
                borrowCollection = null;
                continue;
            }

            if (tender.getTransferFlag() == 1) {//转让中
                Specification<Borrow> bs = Specifications
                        .<Borrow>and()
                        .in("status", 0, 1)
                        .eq("tenderId", tender.getId())
                        .build();

                List<Borrow> borrowList = borrowService.findList(bs);
                if (!CollectionUtils.isEmpty(borrowList)) {
                    VoCancelBorrow voCancelBorrow = new VoCancelBorrow();
                    voCancelBorrow.setBorrowId(borrowList.get(0).getId());
                    //取消当前借款
                    borrowBiz.cancelBorrow(voCancelBorrow);
                }
                tender.setTransferFlag(0);//设置转让标识
            }

            if (tender.getTransferFlag() == 2) { //已转让
                Specification<Borrow> bs = Specifications
                        .<Borrow>and()
                        .eq("tenderId", tender.getId())
                        .eq("status", 3)
                        .build();

                List<Borrow> borrowList = borrowService.findList(bs);
                if (CollectionUtils.isEmpty(borrowList)) {
                    continue;
                }

                Borrow tempBorrow = borrowList.get(0);
                int tempOrder = order + tempBorrow.getTotalOrder() - borrow.getTotalOrder();
                int tempLateInterest = tender.getValidMoney() / borrow.getMoney() * lateInterest;
                int accruedInterest = 0;
                if (tempOrder == 0) {//如果是转让后第一期回款, 则计算转让者首期应计利息
                    int interest = borrowCollection.getInterest();
                    Date startAt = DateHelper.beginOfDate((Date) borrowCollection.getStartAt().clone());//获取00点00分00秒
                    Date collectionAt = DateHelper.beginOfDate((Date) borrowCollection.getCollectionAt().clone());
                    Date startAtYes = DateHelper.beginOfDate((Date) borrowCollection.getStartAtYes().clone());
                    Date endAt = DateHelper.beginOfDate((Date) tempBorrow.getSuccessAt().clone());

                    if (endAt.getTime() > collectionAt.getTime()) {
                        endAt = (Date) collectionAt.clone();
                    }

                    accruedInterest = Math.round(interest *
                            Math.max(DateHelper.diffInDays(endAt, startAtYes, false), 0) /
                            DateHelper.diffInDays(collectionAt, startAt, false));

                    if (accruedInterest > 0) {
                        CapitalChangeEntity entity = new CapitalChangeEntity();
                        entity.setType(CapitalChangeEnum.IncomeOther);
                        entity.setUserId(tender.getUserId());
                        entity.setMoney(accruedInterest);
                        entity.setRemark("收到借款标[" + BorrowHelper.getBorrowLink(tempBorrow.getId(), tempBorrow.getName()) + "]转让当期应计利息。");
                        capitalChangeHelper.capitalChange(entity);

                        //通过红包账户发放
                        //调用即信发放债权转让人应收利息
                        //查询红包账户
                        DictValue dictValue = jixinCache.get(JixinContants.RED_PACKET_USER_ID);
                        UserThirdAccount redPacketAccount = userThirdAccountService.findByUserId(NumberHelper.toLong(dictValue.getValue03()));

                        VoucherPayRequest voucherPayRequest = new VoucherPayRequest();
                        voucherPayRequest.setAccountId(redPacketAccount.getAccountId());
                        voucherPayRequest.setTxAmount(StringHelper.formatDouble(accruedInterest * 0.9, 100, false));//扣除手续费
                        voucherPayRequest.setForAccountId(userThirdAccount.getAccountId());
                        voucherPayRequest.setDesLineFlag(DesLineFlagContant.TURE);
                        voucherPayRequest.setDesLine("发放债权转让人应收利息");
                        voucherPayRequest.setChannel(ChannelContant.HTML);
                        VoucherPayResponse response = jixinManager.send(JixinTxCodeEnum.SEND_RED_PACKET, voucherPayRequest, VoucherPayResponse.class);
                        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
                            String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
                            log.error("BorrowRepaymentThirdBizImpl 调用即信发送发放债权转让人应收利息异常:" + msg);
                        }


                        //利息管理费
                        entity = new CapitalChangeEntity();
                        entity.setType(CapitalChangeEnum.InterestManager);
                        entity.setUserId(tender.getUserId());
                        entity.setMoney((int) (accruedInterest * 0.1));
                        capitalChangeHelper.capitalChange(entity);

                        Integer integral = accruedInterest * 10;
                        if (borrow.getType() == 0 && 0 < integral) {
                            IntegralChangeEntity integralChangeEntity = new IntegralChangeEntity();
                            integralChangeEntity.setUserId(borrow.getUserId());
                            integralChangeEntity.setType(IntegralChangeEnum.TENDER);
                            integralChangeEntity.setValue(integral);
                            integralChangeHelper.integralChange(integralChangeEntity);
                        }
                    }
                }

                borrowCollection.setCollectionAtYes(new Date());
                borrowCollection.setStatus(1);
                borrowCollection.setCollectionMoneyYes(accruedInterest);
                borrowCollectionService.updateById(borrowCollection);

                //回调
                receivedReapy(tempBorrow, tempOrder, interestPercent, lateDays, tempLateInterest, advance);

                if (tempOrder == (tempBorrow.getTotalOrder() - 1)) {
                    tempBorrow.setCloseAt(borrowCollection.getCollectionAtYes());
                    borrowService.updateById(tempBorrow);
                }
                continue;
            }

            int collectionInterest = (int) (borrowCollection.getInterest() * interestPercent);
            int collectionMoney = borrowCollection.getPrincipal() + collectionInterest;

            CapitalChangeEntity entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.IncomeRepayment);
            entity.setUserId(tender.getUserId());
            entity.setToUserId(borrow.getUserId());
            entity.setMoney(collectionMoney);
            entity.setInterest(collectionInterest);
            entity.setRemark("收到客户对借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (borrowCollection.getOrder() + 1) + "期的还款");

            if (advance) {
                entity.setRemark("收到广富宝对借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (borrowCollection.getOrder() + 1) + "期的垫付还款");
            }

            if (interestPercent < 1) {
                entity.setRemark("（提前结清）");
            }
            capitalChangeHelper.capitalChange(entity);

            int interestLower = 0;//应扣除利息
            if (borrow.isTransfer()) {
                int interest = borrowCollection.getInterest();
                Date startAt = DateHelper.beginOfDate((Date) borrowCollection.getStartAt().clone());
                Date collectionAt = DateHelper.beginOfDate((Date) borrowCollection.getCollectionAt().clone());
                Date startAtYes = DateHelper.beginOfDate((Date) borrowCollection.getStartAtYes().clone());
                Date endAt = (Date) collectionAt.clone();

                interestLower = Math.round(interest -
                        interest * Math.max(DateHelper.diffInDays(endAt, startAtYes, false), 0) /
                                DateHelper.diffInDays(collectionAt, startAt, false)
                );

                Long transferUserId = borrow.getUserId();
                entity = new CapitalChangeEntity();
                entity.setType(CapitalChangeEnum.ExpenditureOther);
                entity.setUserId(tender.getUserId());
                entity.setToUserId(transferUserId);
                entity.setMoney(interestLower);
                entity.setRemark("扣除借款标[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]转让方当期应计的利息。");
                capitalChangeHelper.capitalChange(entity);
            }

            //扣除待收
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.CollectionLower);
            entity.setUserId(tender.getUserId());
            entity.setToUserId(borrow.getUserId());
            entity.setMoney(borrowCollection.getCollectionMoney());
            entity.setInterest(borrowCollection.getInterest());
            entity.setRemark("收到客户对[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]借款的还款,扣除待收");
            capitalChangeHelper.capitalChange(entity);

            //利息管理费
            if (((borrow.getType() == 0) || (borrow.getType() == 4)) && collectionInterest > interestLower) {
                /**
                 * '2480 : 好人好梦',1753 : 红运当头',1699 : tasklist',3966 : 苗苗606',1413 : ljc_201',1857 : fanjunle',183 : 54435410',2327 : 栗子',2432 : 高翠西'2470 : sadfsaag',2552 : sadfsaag1',2739 : sadfsaag3',3939 : TinsonCheung',893 : kobayashi',608 : 0211',1216 : zqc9988'
                 */
                Set<String> stockholder = new HashSet<>(Arrays.asList("2480", "1753", "1699", "3966", "1413", "1857", "183", "2327", "2432", "2470", "2552", "2739", "3939", "893", "608", "1216"));
                if (!stockholder.contains(tender.getUserId())) {
                    entity = new CapitalChangeEntity();
                    entity.setType(CapitalChangeEnum.InterestManager);
                    entity.setUserId(tender.getUserId());
                    entity.setMoney((int) MathHelper.myRound((collectionInterest - interestLower) * 0.1, 2));
                    entity.setRemark("收到借款标[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]利息管理费");
                    capitalChangeHelper.capitalChange(entity);
                }
            }

            //逾期收入
            if ((lateDays > 0) && (lateInterest > 0)) {
                int tempLateInterest = (int) MathHelper.myRound((double) tender.getValidMoney() / (double) borrow.getMoney() * lateInterest, 0);
                String remark = "收到借款标'" + borrow.getName() + "'的逾期罚息";

                //调用即信发送红包接口
                //查询红包账户
                DictValue dictValue = jixinCache.get(JixinContants.RED_PACKET_USER_ID);
                UserThirdAccount redPacketAccount = userThirdAccountService.findByUserId(NumberHelper.toLong(dictValue.getValue03()));

                VoucherPayRequest voucherPayRequest = new VoucherPayRequest();
                voucherPayRequest.setAccountId(redPacketAccount.getAccountId());
                voucherPayRequest.setTxAmount(StringHelper.formatDouble(tempLateInterest, 100, false));
                voucherPayRequest.setForAccountId(userThirdAccount.getAccountId());
                voucherPayRequest.setDesLineFlag(DesLineFlagContant.TURE);
                voucherPayRequest.setChannel(ChannelContant.HTML);
                voucherPayRequest.setDesLine(remark);
                VoucherPayResponse response = jixinManager.send(JixinTxCodeEnum.SEND_RED_PACKET, voucherPayRequest, VoucherPayResponse.class);
                if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
                    String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
                    throw new Exception("逾期收入发送异常：" + msg);
                }

                entity = new CapitalChangeEntity();
                entity.setType(CapitalChangeEnum.IncomeOverdue);
                entity.setUserId(tender.getUserId());
                entity.setToUserId(borrow.getUserId());
                entity.setMoney(tempLateInterest);
                entity.setRemark(remark);
                capitalChangeHelper.capitalChange(entity);
            }

            Long tenderUserId = tender.getUserId();
            if (!collectionUserIds.contains(tenderUserId)) {
                collectionUserIds.add(tenderUserId);

                String noticeContent = "客户在 " + DateHelper.dateToString(new Date(), "yyyy-MM-dd HH:mm:ss") + " 已将借款["
                        + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (borrowCollection.getOrder() + 1) + "期还款,还款金额为" + StringHelper.formatDouble(collectionMoney, 100, true) + "元";
                if (advance) {
                    noticeContent = "广富宝在" + DateHelper.dateToString(new Date(), "yyyy-MM-dd HH:mm:ss") + " 已将借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) +
                            "]第" + (borrowCollection.getOrder() + 1) + "期垫付还款,垫付金额为" + StringHelper.formatDouble(collectionMoney, 100, true) + "元";
                }

                Notices notices = new Notices();
                notices.setFromUserId(1L);
                notices.setUserId(tenderUserId);
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
                    log.info(String.format("borrowProvider doAgainVerify send mq %s", GSON.toJson(body)));
                    mqHelper.convertAndSend(mqConfig);
                } catch (Throwable e) {
                    log.error("borrowProvider doAgainVerify send mq exception", e);
                }
            }

            //投资积分
            int integral = (collectionInterest - interestLower) * 10;
            if ((borrow.getType() == 0 || borrow.getType() == 4) && 0 < integral) {
                IntegralChangeEntity integralChangeEntity = new IntegralChangeEntity();
                integralChangeEntity.setType(IntegralChangeEnum.TENDER);
                integralChangeEntity.setUserId(tender.getUserId());
                integralChangeEntity.setValue(integral);
                integralChangeHelper.integralChange(integralChangeEntity);
            }

            borrowCollection.setCollectionAtYes(new Date());
            borrowCollection.setStatus(1);
            borrowCollection.setLateDays(NumberHelper.toInt(StringHelper.toString(lateDays)));
            borrowCollection.setLateInterest(lateInterest);
            borrowCollection.setCollectionMoneyYes(collectionMoney);

            //
            borrowCollectionService.updateById(borrowCollection);

            //更新投标
            tender.setState(3);
            tenderService.updateById(tender);

            /**
             * @// TODO: 2017/7/17
             */

            //收到车贷标回款扣除 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
            //updateUserCacheByReceivedRepay(borrowCollection, tender, borrow);
            //项目回款短信通知
            //smsNoticeByReceivedRepay(borrowCollection, tender, borrow);
            //事件event(new ReceivedRepay($collection, $tender, $borrow));
        }
    }

    /**
     * 新版立即还款
     * 1.还款判断
     * 2.
     *
     * @param userId
     * @param borrowRepaymentId
     * @return
     */
    public ResponseEntity<VoBaseResp> newRepay(Long userId, Long borrowRepaymentId) throws Exception {
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        Preconditions.checkNotNull(userThirdAccount, "批量还款: 还款用户存管账户不存在");
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(borrowRepaymentId);
        Preconditions.checkNotNull(borrowRepayment, "批量还款: 还款记录不存在");
        Borrow normalBorrow = borrowService.findByIdLock(borrowRepayment.getBorrowId());
        Preconditions.checkNotNull(normalBorrow, "批量还款: 还款标的信息不存在");
        ResponseEntity<VoBaseResp> conditionResponse = repayConditionCheck(userThirdAccount, borrowRepayment);
        if (!conditionResponse.getStatusCode().equals(HttpStatus.OK)) {
            return conditionResponse;
        }

        // 获取投标记录
        List<Tender> normalTenderList = queryTenderByRepayment(borrowRepayment);
        findTranferAndCancelTranfer(normalTenderList);
        List<BorrowCollection> normalBorrowCollections = queryBorrowCollectionByTender(borrowRepayment.getOrder(), normalTenderList);

        List<Tender> tranferedTender = normalTenderList
                .stream()
                .filter(p -> p.getTransferFlag() == 2)
                .collect(Collectors.toList());   // 已经债权转让成功的投资记录

        Map<Long, List<Tender>> tranferedTenderMap = findTranferedTenderRecord(tranferedTender);   // 获取债权装让成功的投资记录
        Map<Long, Borrow> tranferedBorrowMap = findTranferedBorrowByTender(tranferedTender);

        Map<Long, List<BorrowCollection>> tranferedBorrowCollections = findTranferBorrowCollection(borrowRepayment, normalBorrow, tranferedTenderMap, tranferedBorrowMap);

        // 计算逾期产生的总费用
        int lateInterest = calculateLateInterest(borrowRepayment, normalBorrow);

        List<Repay> repayPlans = programRepayPlan(borrowRepayment,
                normalBorrow,
                normalTenderList,
                normalBorrowCollections,
                tranferedBorrowMap,
                tranferedTenderMap,
                tranferedBorrowCollections,
                lateInterest);


        if (ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes())) {
            log.info("批次还款: 进入正常还款流程");
        } else {
            log.info("垫付批次划款");

        }

        return null;

    }


    /**
     * 生成还款计划
     *
     * @param borrowRepayment
     * @param normalBorrow
     * @param normalTenderList
     * @param normalBorrowCollections
     * @param tranferedBorrowMap
     * @param tranferedTenderMap
     * @param tranferedBorrowCollections
     * @param lateInterest               @return
     */
    private List<Repay> programRepayPlan(BorrowRepayment borrowRepayment,
                                         Borrow normalBorrow,
                                         List<Tender> normalTenderList,
                                         List<BorrowCollection> normalBorrowCollections,
                                         Map<Long, Borrow> tranferedBorrowMap,
                                         Map<Long, List<Tender>> tranferedTenderMap,
                                         Map<Long, List<BorrowCollection>> tranferedBorrowCollections,
                                         int lateInterest) {
        long currInterestAll = borrowRepayment.getInterest(); // 当期还款总额
        long currPrincipal = borrowRepayment.getPrincipal();  //  当期还款本金
        Long repaymentUserId = borrowRepayment.getUserId();
        UserThirdAccount repaymentUserThirdAccount = userThirdAccountService.findByUserId(repaymentUserId);
        // 获取用户开户信息
        Map<Long, UserThirdAccount> userThirdAccounts = findUserThirdAccountByTenderList(normalTenderList, tranferedTenderMap);
        Map<Long, BorrowCollection> borrowCollectionRefMap = normalBorrowCollections
                .stream()
                .collect(Collectors.toMap(BorrowCollection::getTenderId, Function.identity()));
        List<Repay> repays = new ArrayList<>();
        for (Tender tender : normalTenderList) {
            Repay repay = null;
            int inIn = 0;  // 出借人利息
            int inPr = 0;  // 出借人本金
            int inFee = 0; // 出借人费用
            int ouFee = 0; // 借款人费用
            if (tender.getTransferFlag().equals(2)) {   // 债权转让成功
                List<Tender> tranferedTenderList = tranferedTenderMap.get(tender.getId());
                List<BorrowCollection> tranferedBorrowCollectionList = tranferedBorrowCollections.get(tender.getId());
                Map<Long, BorrowCollection> tranferedBorrowCollectionMap = tranferedBorrowCollectionList
                        .stream()
                        .collect(Collectors.toMap(BorrowCollection::getTenderId, Function.identity()));


                for (Tender tranferTender : tranferedTenderList) {
                    inIn = 0;  // 出借人利息
                    inPr = 0;  // 出借人本金
                    inFee = 0; // 出借人费用
                    ouFee = 0; // 借款人费用
                    BorrowCollection borrowCollection = tranferedBorrowCollectionMap.get(tranferTender.getId());
                    inPr = borrowCollection.getPrincipal();    // 本金
                    inIn = borrowCollection.getInterest();     // 利息
                    inFee = new Double(MathHelper.myRound(inIn * 0.1, 2)).intValue();  // 收取费用
                    inIn += new Double(MathHelper.myRound((lateInterest / 2D) * (borrowCollection.getPrincipal() / borrowRepayment.getPrincipal()), 2)).intValue();  // 用户收取的逾期费用 / 2
                    ouFee = new Double(MathHelper.myRound((lateInterest / 2D) * (borrowCollection.getPrincipal() / borrowRepayment.getPrincipal()), 2)).intValue();  // 平台收取的逾期费油 / 2
                    repay = new Repay();
                    String orderId = JixinHelper.getOrderId(JixinHelper.REPAY_PREFIX);
                    UserThirdAccount userThirdAccount = userThirdAccounts.get(tender.getUserId());
                    Preconditions.checkNotNull(userThirdAccount, "批量还款: 生成还款, 出借人为开户");
                    repay.setAccountId(repaymentUserThirdAccount.getAccountId());  // 还款
                    repay.setAuthCode(tender.getAuthCode()); // 授权码
                    repay.setForAccountId(userThirdAccount.getAccountId()); // 出借人电子账户信息
                    repay.setIntAmount(StringHelper.formatDouble(inIn, 100, false));  // 利息
                    repay.setOrderId(orderId);
                    repay.setProductId(normalBorrow.getProductId()); // 标的信息
                    repay.setTxAmount(StringHelper.formatDouble(inPr, 100, false));  // 本金
                    repay.setTxFeeIn(StringHelper.formatDouble(inFee, 100, false));  // 出借人手续费
                    repay.setTxFeeOut(StringHelper.formatDouble(ouFee, 100, false)); // 融资人手续费
                    repays.add(repay);
                }
            } else {
                inIn = 0;  // 出借人利息
                inPr = 0;  // 出借人本金
                inFee = 0; // 出借人费用
                ouFee = 0; // 借款人费用
                BorrowCollection borrowCollection = borrowCollectionRefMap.get(tender.getId());
                inPr = borrowCollection.getPrincipal();    // 本金
                inIn = borrowCollection.getInterest();     // 利息
                inFee = new Double(MathHelper.myRound(inIn * 0.1, 2)).intValue();  // 收取费用
                inIn += new Double(MathHelper.myRound((lateInterest / 2D) * (borrowCollection.getPrincipal() / borrowRepayment.getPrincipal()), 2)).intValue();  // 用户收取的逾期费用 / 2
                ouFee = new Double(MathHelper.myRound((lateInterest / 2D) * (borrowCollection.getPrincipal() / borrowRepayment.getPrincipal()), 2)).intValue();  // 平台收取的逾期费油 / 2
                repay = new Repay();
                String orderId = JixinHelper.getOrderId(JixinHelper.REPAY_PREFIX);
                UserThirdAccount userThirdAccount = userThirdAccounts.get(tender.getUserId());
                Preconditions.checkNotNull(userThirdAccount, "批量还款: 生成还款, 出借人为开户");
                repay.setAccountId(repaymentUserThirdAccount.getAccountId());  // 还款
                repay.setAuthCode(tender.getAuthCode()); // 授权码
                repay.setForAccountId(userThirdAccount.getAccountId()); // 出借人电子账户信息
                repay.setIntAmount(StringHelper.formatDouble(inIn, 100, false));  // 利息
                repay.setOrderId(orderId);
                repay.setProductId(normalBorrow.getProductId()); // 标的信息
                repay.setTxAmount(StringHelper.formatDouble(inPr, 100, false));  // 本金
                repay.setTxFeeIn(StringHelper.formatDouble(inFee, 100, false));  // 出借人手续费
                repay.setTxFeeOut(StringHelper.formatDouble(ouFee, 100, false)); // 融资人手续费
                repays.add(repay);
            }
        }

        return repays;
    }


    /**
     * 根据投资记录获取用户开户信息
     *
     * @param normalTenderList
     * @param tranferedTenderMap
     * @return
     */
    private Map<Long, UserThirdAccount> findUserThirdAccountByTenderList(List<Tender> normalTenderList, Map<Long, List<Tender>> tranferedTenderMap) {
        List<Tender> allTender = new ArrayList<>();
        allTender.addAll(normalTenderList);
        tranferedTenderMap.keySet().stream().forEach(key -> {
            allTender.addAll(tranferedTenderMap.get(key));
        });

        Set<Long> userIdSet = allTender.stream().map(p -> p.getUserId()).collect(Collectors.toSet());
        Specification<UserThirdAccount> userThirderAccountSpe = Specifications
                .<UserThirdAccount>and()
                .in("userId", userIdSet)
                .build();
        List<UserThirdAccount> userThirdAccountList = userThirdAccountService.findList(userThirderAccountSpe);

        return userThirdAccountList
                .stream()
                .collect(Collectors.toMap(UserThirdAccount::getUserId, Function.identity()));
    }

    /**
     * 获取用户逾期费用
     * 逾期规则: 未还款本金之和 * 0.4$ 的费用, 平台收取 0.2%, 出借人 0.2%
     *
     * @param borrowRepayment
     * @param repaymentBorrow
     * @return
     */
    private int calculateLateInterest(BorrowRepayment borrowRepayment, Borrow repaymentBorrow) {
        Date nowDateOfBegin = DateHelper.beginOfDate(new Date());
        Date repayDateOfBegin = DateHelper.beginOfDate(borrowRepayment.getRepayAt());
        int lateDays = DateHelper.diffInDays(nowDateOfBegin, repayDateOfBegin, false);
        lateDays = lateDays < 0 ? 0 : lateDays;
        if (0 == lateDays) {
            return 0;
        }

        int overPrincipal = borrowRepayment.getPrincipal();
        if (borrowRepayment.getOrder() < (repaymentBorrow.getTotalOrder() - 1)) { //
            Specification<BorrowRepayment> brs = Specifications
                    .<BorrowRepayment>and()
                    .eq("borrowId", repaymentBorrow.getId())
                    .eq("status", 0)
                    .build();
            List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
            Preconditions.checkNotNull(borrowRepayment, "批量放款: 计算逾期费用时还款计划为空");
            overPrincipal = 0;
            for (BorrowRepayment temp : borrowRepaymentList) {
                overPrincipal += temp.getPrincipal();
            }
        }
        return new Double(MathHelper.myRound(overPrincipal * 0.004 * lateDays, 2)).intValue();

    }


    /**
     * 获取债权转让还款计划
     *
     * @param borrowRepayment
     * @param repaymentBorrow
     * @param tranferedTenderMap
     * @param tranferedBorrowMap
     * @return
     */
    private Map<Long, List<BorrowCollection>> findTranferBorrowCollection(BorrowRepayment borrowRepayment,
                                                                          Borrow repaymentBorrow,
                                                                          Map<Long, List<Tender>> tranferedTenderMap,
                                                                          Map<Long, Borrow> tranferedBorrowMap) {

        Map<Long, List<BorrowCollection>> tranferedBorrowCollections = new HashMap<>(tranferedTenderMap.size());

        tranferedTenderMap.keySet().stream().forEach((Long key) -> {
            Borrow borrow = tranferedBorrowMap.get(key);
            int order = borrowRepayment.getOrder() + borrow.getTotalOrder() - repaymentBorrow.getTotalOrder();  // 获取还款
            tranferedBorrowCollections.put(key,
                    queryBorrowCollectionByTender(order, tranferedTenderMap.get(key)));
        });

        return tranferedBorrowCollections;
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
     * 查询投标记录中是否存在债权转让进行中的记录, 如果存在则进行取消债权转让
     *
     * @param tenderList
     */
    private void findTranferAndCancelTranfer(List<Tender> tenderList) throws Exception {
        // 债转进行中的记录
        List<Tender> tranferingTender = tenderList
                .stream()
                .filter(p -> p.getTransferFlag() == 1)
                .collect(Collectors.toList());
        for (Tender tender : tranferingTender) {
            // TODO 取消债权转让
            doCancelTranfer(tender);
        }
    }

    /**
     * 取消债权转让
     *
     * @param tender
     * @throws Exception
     */
    private void doCancelTranfer(Tender tender) throws Exception {

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
    private ResponseEntity<VoBaseResp> repayConditionCheck(UserThirdAccount userThirdAccount, BorrowRepayment borrowRepayment) {
        // 1. 还款用户是否与还款计划用户一致
        if (!userThirdAccount.getUserId().equals(borrowRepayment.getUserId())) {
            log.error("批量还款: 还款前期判断, 还款计划用户与主动请求还款用户不匹配");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "非法操作: 还款计划与当前请求用户不一致!"));
        }

        // 2判断提交还款批次是否多次重复提交
        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(borrowRepayment.getId()),
                ThirdBatchLogContants.BATCH_REPAY_BAIL,
                ThirdBatchLogContants.BATCH_REPAY);
        if (flag > 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("还款处理中，请勿重复点击!")));
        }

        //  3. 判断是否跳跃还款
        Specification<BorrowRepayment> borrowRepaymentSpe = Specifications
                .<BorrowRepayment>and()
                .eq("id", borrowRepayment.getId())
                .eq("status", 0)
                .predicate(new LtSpecification<BorrowRepayment>("order", new DataObject(borrowRepayment.getOrder())))
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(borrowRepaymentSpe);
        if (!CollectionUtils.isEmpty(borrowRepaymentList)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("该借款上一期还未还!")));
        }

        return ResponseEntity.ok(VoBaseResp.ok("验证成功"));

    }

    /**
     * 立即还款
     *
     * @param voRepayReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> repay(VoRepayReq voRepayReq) throws Exception {
        // ====================================
        //  1. 平台可用用金额
        //  2. 存管账户是否够用
        //  3. 冻结还款
        //  4. 还款
        // ===================================
        ResponseEntity<VoBaseResp> resp = checkRepay(voRepayReq);
        if (!ObjectUtils.isEmpty(resp)) {
            return resp;
        }
        VoThirdBatchRepay voThirdBatchRepay = new VoThirdBatchRepay();
        voThirdBatchRepay.setUserId(voRepayReq.getUserId());
        voThirdBatchRepay.setRepaymentId(voRepayReq.getRepaymentId());
        voThirdBatchRepay.setInterestPercent(0d);
        voThirdBatchRepay.setIsUserOpen(true);

        // ====================================
        // 存管第三方还款操作
        // ====================================
        Date nowDate = new Date();
        Long repaymentId = voThirdBatchRepay.getRepaymentId();
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
        Preconditions.checkNotNull(borrowRepayment, "还款不存在!");
        UserThirdAccount borrowUserThirdAccount = userThirdAccountService.findByUserId(borrowRepayment.getUserId());
        Preconditions.checkNotNull(borrowUserThirdAccount, "借款人未开户!");

        List<Repay> repayList = null;
        if (ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes())) {  // 正常还款
            repayList = borrowRepaymentThirdBiz.getRepayList(voThirdBatchRepay);
        } else {  //批次融资人还担保账户垫款
            //====================================================================
            // 批次融资人还担保账户垫款
            //====================================================================
            return thirdBatchRepayBail(voRepayReq);
        }

        if (CollectionUtils.isEmpty(repayList)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "还款不存在"));
        }

        double txAmount = 0;
        for (Repay repay : repayList) {
            txAmount += NumberHelper.toDouble(repay.getTxAmount());
        }

        String batchNo = jixinHelper.getBatchNo();

        //====================================================================
        //冻结借款人账户资金
        //====================================================================
        String orderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(borrowUserThirdAccount.getAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(txAmount, false));
        balanceFreezeReq.setOrderId(orderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, balanceFreezeResp.getRetMsg()));
        }

        BatchRepayReq request = new BatchRepayReq();
        request.setBatchNo(batchNo);
        request.setTxAmount(StringHelper.formatDouble(txAmount, false));
        request.setRetNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repayDeal/run");
        request.setNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repayDeal/check");
        request.setAcqRes(GSON.toJson(voThirdBatchRepay));
        request.setSubPacks(GSON.toJson(repayList));
        request.setChannel(ChannelContant.HTML);
        request.setTxCounts(StringHelper.toString(repayList.size()));
        BatchRepayResp response = jixinManager.send(JixinTxCodeEnum.BATCH_REPAY, request, BatchRepayResp.class);
        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, response.getRetMsg()));
        }

        //记录日志
        ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
        thirdBatchLog.setBatchNo(batchNo);
        thirdBatchLog.setCreateAt(nowDate);
        thirdBatchLog.setUpdateAt(nowDate);
        thirdBatchLog.setSourceId(repaymentId);
        thirdBatchLog.setType(ThirdBatchLogContants.BATCH_REPAY);
        thirdBatchLog.setRemark("即信批次还款");
        thirdBatchLogService.save(thirdBatchLog);

        return ResponseEntity.ok(VoBaseResp.ok("还款成功"));
    }

    /**
     * 收到代偿还款
     *
     * @param borrow
     * @param order
     * @param interestPercent
     * @param lateInterest
     * @return
     * @throws Exception
     */
    private void receivedRepayBail(List<RepayBail> repayBails, Borrow borrow, String borrowUserThirdAccount, int order, double interestPercent, int lateInterest) throws Exception {
        do {
            //===================================还款校验==========================================
            if (ObjectUtils.isEmpty(borrow)) {
                break;
            }

            Long borrowId = borrow.getId();
            Specification<Tender> specification = Specifications
                    .<Tender>and()
                    .eq("status", 1)
                    .eq("borrowId", borrowId)
                    .build();

            List<Tender> tenderList = tenderService.findList(specification);
            if (CollectionUtils.isEmpty(tenderList)) {
                break;
            }

            List<Long> userIds = new ArrayList<>();
            List<Long> tenderIds = new ArrayList<>();
            for (Tender tender : tenderList) {
                userIds.add(tender.getUserId());
                tenderIds.add(tender.getId());
            }

            Specification<UserCache> ucs = Specifications
                    .<UserCache>and()
                    .in("userId", userIds.toArray())
                    .build();

            List<UserCache> userCacheList = userCacheService.findList(ucs);
            if (CollectionUtils.isEmpty(userCacheList)) {
                break;
            }

            Specification<BorrowCollection> bcs = Specifications
                    .<BorrowCollection>and()
                    .in("tenderId", tenderIds.toArray())
                    .eq("status", 1)
                    .eq("order", order)
                    .build();

            List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
            if (CollectionUtils.isEmpty(borrowCollectionList)) {
                break;
            }
            //==================================================================================
            RepayBail repayBail = null;
            int txAmount = 0;//融资人实际付出金额=交易金额+交易利息+还款手续费
            int intAmount = 0;//交易利息
            int principal = 0;
            int txFeeOut = 0;
            for (Tender tender : tenderList) {
                repayBail = new RepayBail();
                txAmount = 0;
                intAmount = 0;
                txFeeOut = 0;

                BorrowCollection borrowCollection = null;//当前借款的回款记录
                for (int i = 0; i < borrowCollectionList.size(); i++) {
                    borrowCollection = borrowCollectionList.get(i);
                    if (StringHelper.toString(tender.getId()).equals(StringHelper.toString(borrowCollection.getTenderId()))) {
                        break;
                    }
                    borrowCollection = null;
                    continue;
                }

                if (tender.getTransferFlag() == 1) {//转让中
                    Specification<Borrow> bs = Specifications
                            .<Borrow>and()
                            .eq("tenderId", tender.getId())
                            .in("status", 0, 1)
                            .build();

                    List<Borrow> borrowList = borrowService.findList(bs);
                    if (CollectionUtils.isEmpty(borrowList)) {
                        continue;
                    }
                }

                if (tender.getTransferFlag() == 2) { //已转让
                    Specification<Borrow> bs = Specifications
                            .<Borrow>and()
                            .eq("tenderId", tender.getId())
                            .eq("status", 3)
                            .build();

                    List<Borrow> borrowList = borrowService.findList(bs);
                    if (CollectionUtils.isEmpty(borrowList)) {
                        continue;
                    }

                    Borrow tempBorrow = borrowList.get(0);
                    int tempOrder = order + tempBorrow.getTotalOrder() - borrow.getTotalOrder();
                    int tempLateInterest = tender.getValidMoney() / borrow.getMoney() * lateInterest;

                    //回调
                    receivedRepayBail(repayBails, tempBorrow, borrowUserThirdAccount, tempOrder, interestPercent, tempLateInterest);
                    continue;
                }

                intAmount = (int) (borrowCollection.getInterest() * interestPercent);
                principal = borrowCollection.getPrincipal();


                //借款人逾期罚息
                if (lateInterest > 0) {
                    txFeeOut += lateInterest;
                }

                txAmount = principal + intAmount + txFeeOut;

                String orderId = JixinHelper.getOrderId(JixinHelper.BAIL_REPAY_PREFIX);
                repayBail.setOrderId(orderId);
                repayBail.setAccountId(borrowUserThirdAccount);
                repayBail.setTxAmount(StringHelper.formatDouble(txAmount, 100, false));
                repayBail.setIntAmount(StringHelper.formatDouble(intAmount, 100, false));
                repayBail.setForAccountId(borrow.getBailAccountId());
                repayBail.setTxFeeOut(StringHelper.formatDouble(txFeeOut, 100, false));
                repayBail.setOrgOrderId(borrowCollection.getTBailRepayOrderId());
                repayBail.setAuthCode(borrowCollection.getTBailAuthCode());

                repayBails.add(repayBail);

                borrowCollection.setTRepayBailOrderId(orderId);
                borrowCollectionService.updateById(borrowCollection);
            }
        } while (false);
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
        return repay(voRepayReq);
    }

    /**
     * 垫付检查
     *
     * @param repaymentId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> advanceCheck(Long repaymentId) throws Exception {
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
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

        Long advanceUserId = 22L;//垫付账号
        Asset advanceUserAsses = assetService.findByUserIdLock(advanceUserId);

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

        //判断提交还款批次是否多次重复提交
        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(repaymentId), ThirdBatchLogContants.BATCH_BAIL_REPAY);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("垫付处理中，请勿重复点击!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            /**
             * @// TODO: 2017/7/18 增加本地查询
             */
        }

        long lateInterest = 0;//逾期利息
        int lateDays = 0;//逾期天数
        int diffDay = DateHelper.diffInDays(DateHelper.beginOfDate(new Date()), DateHelper.beginOfDate(borrowRepayment.getRepayAt()), false);
        if (diffDay > 0) {
            lateDays = diffDay;
            int overPrincipal = borrowRepayment.getPrincipal();//剩余未还本金
            if (order < (borrow.getTotalOrder() - 1)) {
                brs = Specifications
                        .<BorrowRepayment>and()
                        .eq("borrowId", borrow.getId())
                        .eq("status", 0)
                        .build();
                List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
                for (BorrowRepayment tempBorrowRepayment : borrowRepaymentList) {
                    overPrincipal += tempBorrowRepayment.getPrincipal();
                }
            }
            lateInterest = Math.round(overPrincipal * 0.004 * lateDays);
        }

        long repayInterest = borrowRepayment.getInterest();//还款利息
        long repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额
        if (advanceUserAsses.getUseMoney() < (repayMoney + lateInterest)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "账户余额不足，请先充值"));
        }

        return null;
    }

    /**
     * pc垫付
     *
     * @param voPcAdvanceReq
     * @return
     * @throws Exception
     */
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
        return advance(voAdvanceReq);
    }

    /**
     * 垫付
     *
     * @param voAdvanceReq
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> advance(VoAdvanceReq voAdvanceReq) throws Exception {
        Long repaymentId = voAdvanceReq.getRepaymentId();

        ResponseEntity resp = advanceCheck(repaymentId);
        if (!ObjectUtils.isEmpty(resp)) {
            return resp;
        }
        VoBatchBailRepayReq voBatchBailRepayReq = new VoBatchBailRepayReq();
        voBatchBailRepayReq.setRepaymentId(repaymentId);

        //=======================================================
        // 调用存管担保人代偿
        //=======================================================
        Date nowDate = new Date();

        List<BailRepay> bailRepayList = borrowRepaymentThirdBiz.getBailRepayList(voBatchBailRepayReq);
        if (CollectionUtils.isEmpty(bailRepayList)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "代偿不存在"));
        }

        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());
        UserThirdAccount borrowUserThirdAccount = userThirdAccountService.findByUserId(borrow.getUserId());

        double txAmount = 0;
        for (BailRepay bailRepay : bailRepayList) {
            txAmount += NumberHelper.toDouble(bailRepay.getTxAmount());
        }

        //批次号
        String batchNo = jixinHelper.getBatchNo();
        //请求保留参数
        Map<String, Object> acqResMap = new HashMap<>();
        acqResMap.put("repaymentId", repaymentId);

        //====================================================================
        //冻结借款人账户资金
        //====================================================================
        String orderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(borrowUserThirdAccount.getAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(txAmount, false));
        balanceFreezeReq.setOrderId(orderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            throw new Exception("即信批次还款冻结资金失败：" + balanceFreezeResp.getRetMsg());
        }

        BatchBailRepayReq request = new BatchBailRepayReq();
        request.setChannel(ChannelContant.HTML);
        request.setBatchNo(batchNo);
        request.setAccountId(borrow.getBailAccountId());
        request.setProductId(borrow.getProductId());
        request.setTxAmount(StringHelper.formatDouble(txAmount, false));
        request.setTxCounts(StringHelper.toString(bailRepayList.size()));
        request.setNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/bailrepay/check");
        request.setRetNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/bailrepay/run");
        request.setAcqRes(GSON.toJson(acqResMap));
        request.setSubPacks(GSON.toJson(bailRepayList));
        BatchBailRepayResp response = jixinManager.send(JixinTxCodeEnum.BATCH_BAIL_REPAY, request, BatchBailRepayResp.class);
        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "批次担保账户代偿失败!"));
        }

        //记录日志
        ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
        thirdBatchLog.setBatchNo(batchNo);
        thirdBatchLog.setCreateAt(nowDate);
        thirdBatchLog.setUpdateAt(nowDate);
        thirdBatchLog.setSourceId(repaymentId);
        thirdBatchLog.setType(ThirdBatchLogContants.BATCH_BAIL_REPAY);
        thirdBatchLog.setRemark("即信担保人还垫付");
        thirdBatchLogService.save(thirdBatchLog);

        return ResponseEntity.ok(VoBaseResp.ok("批次担保账户代偿成功!"));
    }

    /**
     * 垫付处理
     *
     * @param voAdvanceReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> advanceDeal(VoAdvanceCall voAdvanceReq) throws Exception {

        ResponseEntity resp = advanceCheck(voAdvanceReq.getRepaymentId());//垫付检查
        if (!ObjectUtils.isEmpty(resp)) {
            return resp;
        }
        Long repaymentId = voAdvanceReq.getRepaymentId();
        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());

        Long advanceUserId = 22L;//垫付账号
        Asset advanceUserAsses = assetService.findByUserIdLock(advanceUserId);

        Specification<BorrowRepayment> brs = null;
        int order = borrowRepayment.getOrder();

        long lateInterest = 0;//逾期利息
        int lateDays = 0;//逾期天数
        int diffDay = DateHelper.diffInDays(DateHelper.beginOfDate(new Date()), DateHelper.beginOfDate(borrowRepayment.getRepayAt()), false);
        if (diffDay > 0) {
            lateDays = diffDay;
            int overPrincipal = borrowRepayment.getPrincipal();//剩余未还本金
            if (order < (borrow.getTotalOrder() - 1)) {
                brs = Specifications
                        .<BorrowRepayment>and()
                        .eq("borrowId", borrow.getId())
                        .eq("status", 0)
                        .build();
                List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
                for (BorrowRepayment tempBorrowRepayment : borrowRepaymentList) {
                    overPrincipal += tempBorrowRepayment.getPrincipal();
                }
            }
            lateInterest = Math.round(overPrincipal * 0.004 * lateDays);
        }

        long repayInterest = borrowRepayment.getInterest();//还款利息
        long repayMoney = borrowRepayment.getPrincipal() + repayInterest;//还款金额
        if (advanceUserAsses.getUseMoney() < (repayMoney + lateInterest)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "账户余额不足，请先充值"));
        }

        CapitalChangeEntity entity = new CapitalChangeEntity();
        entity.setUserId(advanceUserId);
        entity.setType(CapitalChangeEnum.ExpenditureOther);
        entity.setMoney((int) (repayMoney + lateInterest));
        entity.setRemark("对借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]第" + (order + 1) + "期的垫付还款");
        capitalChangeHelper.capitalChange(entity);

        receivedReapy(borrow, order, 1, lateDays, (int) (lateInterest / 2), true);//还款

        AdvanceLog advanceLog = new AdvanceLog();
        advanceLog.setUserId(advanceUserId);
        advanceLog.setRepaymentId(repaymentId);
        advanceLog.setAdvanceAtYes(new Date());
        advanceLog.setAdvanceMoneyYes((int) (repayMoney + lateInterest));
        advanceLogService.insert(advanceLog);

        borrowRepayment.setLateDays(lateDays);
        borrowRepayment.setLateInterest((int) lateInterest);
        borrowRepayment.setAdvanceAtYes(new Date());
        borrowRepayment.setAdvanceMoneyYes((int) (repayMoney + lateInterest));
        borrowRepaymentService.updateById(borrowRepayment);

        return ResponseEntity.ok(VoBaseResp.ok("垫付成功!"));
    }

    /**
     * 批次融资人还担保账户垫款
     *
     * @param voRepayReq
     */
    public ResponseEntity<VoBaseResp> thirdBatchRepayBail(VoRepayReq voRepayReq) throws Exception {
        Date nowDate = new Date();
        int lateInterest = 0;//逾期利息
        Double interestPercent = voRepayReq.getInterestPercent();
        Long repaymentId = voRepayReq.getRepaymentId();
        interestPercent = ObjectUtils.isEmpty(interestPercent) ? 1 : interestPercent;

        BorrowRepayment borrowRepayment = borrowRepaymentService.findByIdLock(repaymentId);
        Borrow borrow = borrowService.findById(borrowRepayment.getBorrowId());
        Long borrowId = borrow.getId();//借款ID

        UserThirdAccount borrowUserThirdAccount = userThirdAccountService.findByUserId(borrow.getUserId());

        // 逾期天数
        Date nowDateOfBegin = DateHelper.beginOfDate(new Date());
        Date repayDateOfBegin = DateHelper.beginOfDate(borrowRepayment.getRepayAt());
        int lateDays = DateHelper.diffInDays(nowDateOfBegin, repayDateOfBegin, false);
        lateDays = lateDays < 0 ? 0 : lateDays;
        if (0 < lateDays) {  // 产生逾期
            int overPrincipal = borrowRepayment.getPrincipal();
            if (borrowRepayment.getOrder() < (borrow.getTotalOrder() - 1)) {
                Specification<BorrowRepayment> brs = Specifications.<BorrowRepayment>and()
                        .eq("status", 0)
                        .eq("borrowId", borrowId)
                        .build();
                List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
                Preconditions.checkNotNull(borrowRepayment, "还款信息不存在");

                overPrincipal = 0;
                for (BorrowRepayment temp : borrowRepaymentList) {
                    overPrincipal += temp.getPrincipal();
                }
            }
            lateInterest = (int) MathHelper.myRound(overPrincipal * 0.004 * lateDays, 2);
        }

        List<RepayBail> repayBails = null;
        if (!ObjectUtils.isEmpty(borrowRepayment.getAdvanceAtYes())) {
            repayBails = new ArrayList<>();
            receivedRepayBail(repayBails, borrow, borrowUserThirdAccount.getAccountId(), borrowRepayment.getOrder(), interestPercent, lateInterest);
        }

        if (CollectionUtils.isEmpty(repayBails)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "代偿不存在"));
        }

        double txAmount = 0;
        for (RepayBail bailRepay : repayBails) {
            txAmount += NumberHelper.toDouble(bailRepay.getTxAmount());
        }

        String batchNo = jixinHelper.getBatchNo();

        //====================================================================
        //冻结担保人账户资金
        //====================================================================
        String orderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);
        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(borrow.getBailAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(txAmount, false));
        balanceFreezeReq.setOrderId(orderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            throw new Exception("即信批次还款冻结资金失败：" + balanceFreezeResp.getRetMsg());
        }


        BatchRepayBailReq request = new BatchRepayBailReq();
        request.setBatchNo(batchNo);
        request.setTxAmount(StringHelper.formatDouble(txAmount, false));
        request.setSubPacks(GSON.toJson(repayBails));
        request.setTxCounts(StringHelper.toString(repayBails.size()));
        request.setNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repaybail/check");
        request.setRetNotifyURL(javaDomain + "/pub/repayment/v2/third/batch/repaybail/run");
        request.setAcqRes(GSON.toJson(voRepayReq));
        request.setChannel(ChannelContant.HTML);
        BatchRepayBailResp response = jixinManager.send(JixinTxCodeEnum.BATCH_REPAY_BAIL, request, BatchRepayBailResp.class);
        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.BATCH_SUCCESS.equalsIgnoreCase(response.getReceived()))) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "批次融资人还担保账户垫款失败!"));
        }

        //记录日志
        ThirdBatchLog thirdBatchLog = new ThirdBatchLog();
        thirdBatchLog.setBatchNo(batchNo);
        thirdBatchLog.setCreateAt(nowDate);
        thirdBatchLog.setUpdateAt(nowDate);
        thirdBatchLog.setSourceId(repaymentId);
        thirdBatchLog.setType(ThirdBatchLogContants.BATCH_REPAY_BAIL);
        thirdBatchLog.setRemark("批次融资人还担保账户垫款");
        thirdBatchLogService.save(thirdBatchLog);

        return ResponseEntity.ok(VoBaseResp.ok("批次融资人还担保账户垫款成功!"));
    }

    /**
     * 构建存管还款项
     *
     * @param voBuildThirdRepayReq
     * @return
     * @throws Exception
     */
    public VoBuildThirdRepayResp buildThirdRepay(VoBuildThirdRepayReq voBuildThirdRepayReq) throws Exception {
        VoBuildThirdRepayResp resp = new VoBuildThirdRepayResp();

        return null;
    }

}
