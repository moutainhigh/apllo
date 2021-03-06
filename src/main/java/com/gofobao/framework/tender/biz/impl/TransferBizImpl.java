package com.gofobao.framework.tender.biz.impl;

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
import com.gofobao.framework.asset.contants.BatchAssetChangeContants;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.service.AssetService;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.borrow.vo.request.VoBorrowListReq;
import com.gofobao.framework.borrow.vo.response.BorrowInfoRes;
import com.gofobao.framework.borrow.vo.response.VoBorrowTenderUserRes;
import com.gofobao.framework.borrow.vo.response.VoPcBorrowList;
import com.gofobao.framework.borrow.vo.response.VoViewBorrowList;
import com.gofobao.framework.collection.contants.BorrowCollectionContants;
import com.gofobao.framework.collection.entity.BorrowCollection;
import com.gofobao.framework.collection.service.BorrowCollectionService;
import com.gofobao.framework.common.assets.AssetChange;
import com.gofobao.framework.common.assets.AssetChangeProvider;
import com.gofobao.framework.common.assets.AssetChangeTypeEnum;
import com.gofobao.framework.common.constans.MoneyConstans;
import com.gofobao.framework.common.constans.TypeTokenContants;
import com.gofobao.framework.common.data.DataObject;
import com.gofobao.framework.common.data.LeSpecification;
import com.gofobao.framework.common.rabbitmq.MqConfig;
import com.gofobao.framework.common.rabbitmq.MqHelper;
import com.gofobao.framework.common.rabbitmq.MqQueueEnum;
import com.gofobao.framework.common.rabbitmq.MqTagEnum;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.finance.entity.FinancePlan;
import com.gofobao.framework.finance.entity.FinancePlanBuyer;
import com.gofobao.framework.finance.service.FinancePlanBuyerService;
import com.gofobao.framework.finance.service.FinancePlanService;
import com.gofobao.framework.helper.*;
import com.gofobao.framework.helper.project.BatchAssetChangeHelper;
import com.gofobao.framework.helper.project.BorrowCalculatorHelper;
import com.gofobao.framework.helper.project.SecurityHelper;
import com.gofobao.framework.helper.project.UserHelper;
import com.gofobao.framework.member.entity.UserCache;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.repository.UsersRepository;
import com.gofobao.framework.member.service.UserCacheService;
import com.gofobao.framework.member.service.UserService;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.repayment.entity.BorrowRepayment;
import com.gofobao.framework.repayment.service.BorrowRepaymentService;
import com.gofobao.framework.system.biz.IncrStatisticBiz;
import com.gofobao.framework.system.biz.StatisticBiz;
import com.gofobao.framework.system.biz.ThirdBatchLogBiz;
import com.gofobao.framework.system.contants.ThirdBatchLogContants;
import com.gofobao.framework.system.entity.IncrStatistic;
import com.gofobao.framework.system.entity.Notices;
import com.gofobao.framework.system.entity.Statistic;
import com.gofobao.framework.system.entity.ThirdBatchLog;
import com.gofobao.framework.system.service.ThirdBatchLogService;
import com.gofobao.framework.tender.biz.TransferBiz;
import com.gofobao.framework.tender.contants.BorrowContants;
import com.gofobao.framework.tender.contants.TransferBuyLogContants;
import com.gofobao.framework.tender.contants.TransferContants;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.entity.Transfer;
import com.gofobao.framework.tender.entity.TransferBuyLog;
import com.gofobao.framework.tender.repository.TransferRepository;
import com.gofobao.framework.tender.service.TenderService;
import com.gofobao.framework.tender.service.TransferBuyLogService;
import com.gofobao.framework.tender.service.TransferService;
import com.gofobao.framework.tender.vo.request.*;
import com.gofobao.framework.tender.vo.response.*;
import com.gofobao.framework.tender.vo.response.web.TransferBuy;
import com.gofobao.framework.tender.vo.response.web.VoViewTransferBuyWarpRes;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by admin on 2017/6/12.
 */
@Slf4j
@Service
public class TransferBizImpl implements TransferBiz {

    @Autowired
    private TransferService transferService;
    @Autowired
    private TenderService tenderService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private BorrowCollectionService borrowCollectionService;
    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private JixinManager jixinManager;
    @Autowired
    private MqHelper mqHelper;
    @Autowired
    private StatisticBiz statisticBiz;
    @Autowired
    private UserCacheService userCacheService;
    @Autowired
    private IncrStatisticBiz incrStatisticBiz;
    @Autowired
    private UserService userService;
    @Autowired
    private BatchAssetChangeHelper batchAssetChangeHelper;
    @Autowired
    AssetChangeProvider assetChangeProvider;
    @Autowired
    private BorrowRepaymentService borrowRepaymentService;
    @Autowired
    private ThirdBatchLogService thirdBatchLogService;
    @Autowired
    private TransferBuyLogService transferBuyLogService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private TransferRepository transferRepository;
    @Value("${gofobao.imageDomain}")
    private String imageDomain;
    @Autowired
    private FinancePlanService financePlanService;
    @Autowired
    private FinancePlanBuyerService financePlanBuyerService;
    @Autowired
    private ThirdBatchLogBiz thirdBatchLogBiz;
    @Value("${gofobao.adminDomain}")
    private String adminDomain;

    final Gson GSON = new GsonBuilder().create();

    public static final String MSG = "msg";
    public static final String LEFT_MONEY = "leftMoney";

    /**
     * 查询债权转让购买记录
     *
     * @return
     */
    @Override
    public ResponseEntity<VoViewTransferBuyLogList> findTransferBuyLog(VoFindTransferBuyLog voFindTransferBuyLog) {
        /* 债权转让id */
        Long transferId = voFindTransferBuyLog.getTransferId();
        /* 债权转让购买人id */
        long userId = voFindTransferBuyLog.getUserId();
        Specification<TransferBuyLog> tbls = null;
        if (!ObjectUtils.isEmpty(transferId)) {
            /* 债权转让记录 */
            Transfer transfer = transferService.findById(transferId);
            Preconditions.checkNotNull(transfer, "债权转让记录不存在!");
            tbls = Specifications
                    .<TransferBuyLog>and()
                    .eq("transferId", transferId)
                    .eq("userId", userId)
                    .eq("state", 0)
                    .build();
        } else {
            tbls = Specifications
                    .<TransferBuyLog>and()
                    .eq("userId", userId)
                    .eq("state", 0)
                    .build();
        }
        /* 购买债权转让集合 */
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls, new PageRequest(voFindTransferBuyLog.getPageIndex(),
                voFindTransferBuyLog.getPageSize(), new Sort(Sort.Direction.DESC, "createdAt")));
        Map<Long/* 债权转让id */, Transfer> transferMaps = new HashMap<>();
        if (!CollectionUtils.isEmpty(transferBuyLogList)) {
            Set<Long> transferIds = transferBuyLogList.stream().map(TransferBuyLog::getTransferId).collect(Collectors.toSet());
            Specification<Transfer> ts = Specifications
                    .<Transfer>and()
                    .in("id", transferIds.toArray())
                    .eq("state", 1)
                    .build();
            List<Transfer> transferList = transferService.findList(ts);
            Preconditions.checkState(!CollectionUtils.isEmpty(transferList), "债权转让不存在!");
            transferMaps = transferList.stream().collect(Collectors.toMap(Transfer::getId, Function.identity()));
        }
        /* 查询购买债权记录显示集合 */
        List<VoViewTransferBuyLog> voViewTransferBuyLogList = new ArrayList<>();
        for (TransferBuyLog transferBuyLog : transferBuyLogList) {
            Transfer transfer = transferMaps.get(transferBuyLog.getTransferId());
            Borrow borrow = borrowService.findById(transfer.getBorrowId());
            //预期收益
            BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(new Double(transferBuyLog.getPrincipal()),
                    new Double(transfer.getApr()),
                    transfer.getTimeLimit(),
                    new Date());
            Map<String, Object> calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
            Integer earnings = NumberHelper.toInt(StringHelper.toString(calculatorMap.get("earnings")));

            VoViewTransferBuyLog voViewTransferBuyLog = new VoViewTransferBuyLog();
            voViewTransferBuyLog.setTitle(transfer.getTitle());
            voViewTransferBuyLog.setApr(StringHelper.formatDouble(transfer.getApr(), 100, false));
            voViewTransferBuyLog.setBuyAt(DateHelper.dateToString(transferBuyLog.getCreatedAt()));
            voViewTransferBuyLog.setMoney(StringHelper.formatDouble(transferBuyLog.getValidMoney(), 100, true));
            voViewTransferBuyLog.setEarning(StringHelper.formatDouble(earnings, 100, true));
            voViewTransferBuyLog.setPrincipal(StringHelper.formatDouble(transferBuyLog.getPrincipal(), 100, true));
            voViewTransferBuyLog.setAlreadyInterest(StringHelper.formatDouble(transferBuyLog.getAlreadyInterest(), 100, true));
            voViewTransferBuyLog.setTimeLimit(String.format("%s月", transfer.getTimeLimit()));
            voViewTransferBuyLogList.add(voViewTransferBuyLog);
        }
        VoViewTransferBuyLogList resp = VoBaseResp.ok("查询成功", VoViewTransferBuyLogList.class);
        resp.setVoViewTransferBuyLogs(voViewTransferBuyLogList);
        return ResponseEntity.ok(resp);
    }

    /**
     * 结束债权转让
     *
     * @param voEndTransfer
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> endTransfer(VoEndTransfer voEndTransfer) throws Exception {
        long userId = voEndTransfer.getUserId();/* 转让人id */
        long transferId = voEndTransfer.getTransferId();/* 债权转让id */
        //1.获取债权转让记录
        Transfer transfer = transferService.findByIdLock(transferId);/* 债权转让记录 */
        Preconditions.checkNotNull(transfer, "债权转让记录不存在!");
        Tender tender = tenderService.findById(transfer.getTenderId());
        Preconditions.checkNotNull(tender, "投资记录不存在!");
        if (!transfer.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "非本人操作结束债权转让。"));
        }
        // 2.判断提交还款批次是否多次重复提交
        ThirdBatchLog thirdBatchLog = thirdBatchLogBiz.getValidLastBatchLog(String.valueOf(transfer.getId()),
                ThirdBatchLogContants.BATCH_CREDIT_INVEST);

        int flag = thirdBatchLogBiz.checkBatchOftenSubmit(String.valueOf(transfer.getId()),
                ThirdBatchLogContants.BATCH_CREDIT_INVEST);
        if (flag == ThirdBatchLogContants.AWAIT) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("债权转让处理中，暂不能取消转让!")));
        } else if (flag == ThirdBatchLogContants.SUCCESS) {
            //墊付批次处理
            //触发处理批次放款处理结果队列
            //推送批次处理到队列中
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_THIRD_BATCH);
            mqConfig.setTag(MqTagEnum.BATCH_DEAL);
            ImmutableMap<String, String> body = new ImmutableMap.Builder<String, String>()
                    .put(MqConfig.SOURCE_ID, StringHelper.toString(thirdBatchLog.getSourceId()))
                    .put(MqConfig.BATCH_NO, thirdBatchLog.getBatchNo())
                    .put(MqConfig.BATCH_TYPE, String.valueOf(thirdBatchLog.getType()))
                    .put(MqConfig.MSG_TIME, DateHelper.dateToString(new Date()))
                    .put(MqConfig.ACQ_RES, thirdBatchLog.getAcqRes())
                    .put(MqConfig.BATCH_RESP, "")
                    .build();

            mqConfig.setMsg(body);
            try {
                log.info(String.format("transferBizImpl endTransfer send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("transferBizImpl endTransfer send mq exception", e);
            }

            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, StringHelper.toString("债权转让处理中，暂不能取消转让!")));
        }

        //2.判断是否存在已经与存管通信的记录
        Specification<TransferBuyLog> tbls = Specifications
                .<TransferBuyLog>and()
                .eq("transferId", transfer.getId())
                .notIn("state", 2)
                .build();
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls);
        if (!CollectionUtils.isEmpty(transferBuyLogList)) {
            Set<Long> userIds = transferBuyLogList.stream().map(TransferBuyLog::getUserId).collect(Collectors.toSet());
            Specification<UserThirdAccount> utas = Specifications
                    .<UserThirdAccount>and()
                    .in("userId", userIds.toArray())
                    .build();
            List<UserThirdAccount> userThirdAccountList = userThirdAccountService.findList(utas);
            Map<Long/*用户id*/, UserThirdAccount> userThirdAccountMap = userThirdAccountList.stream().collect(Collectors.toMap(UserThirdAccount::getUserId, Function.identity()));

            /* 已跟即信通信的债权转让记录条数 */
            long count = transferBuyLogList.stream().filter(transferBuyLog -> BooleanHelper.isTrue(transferBuyLog.getThirdTransferFlag())).count();
            if (count > 0) {
                return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "已存在售出的债权，无法取消债权转让!"));
            }
            //3.更改债权转让与购买债权转让记录状态
            /* 总购买金额 */
            long sumBuyMoney = transferBuyLogList.stream().mapToLong(TransferBuyLog::getBuyMoney).sum();
            transfer.setTransferMoneyYes(transfer.getTransferMoneyYes() - sumBuyMoney);
            transfer.setTenderCount(transfer.getTenderCount() - transferBuyLogList.size());

            //4.取消购买债权并解冻金额
            transferBuyLogList.stream().forEach(transferBuyLog -> {
                if (!ObjectUtils.isEmpty(transferBuyLog.getFreezeOrderId())) {
                    UserThirdAccount userThirdAccount = userThirdAccountMap.get(transferBuyLog.getUserId());
                    //解除存管资金冻结
                    String orderId = JixinHelper.getOrderId(JixinHelper.BALANCE_UNFREEZE_PREFIX);
                    BalanceUnfreezeReq balanceUnfreezeReq = new BalanceUnfreezeReq();
                    balanceUnfreezeReq.setAccountId(String.valueOf(userThirdAccount.getAccountId()));
                    balanceUnfreezeReq.setTxAmount(String.valueOf(transferBuyLog.getValidMoney()));
                    balanceUnfreezeReq.setChannel(ChannelContant.HTML);
                    balanceUnfreezeReq.setOrderId(orderId);
                    balanceUnfreezeReq.setOrgOrderId(String.valueOf(transferBuyLog.getFreezeOrderId()));
                    BalanceUnfreezeResp balanceUnfreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_UN_FREEZE, balanceUnfreezeReq, BalanceUnfreezeResp.class);
                    if ((ObjectUtils.isEmpty(balanceUnfreezeResp)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceUnfreezeResp.getRetCode()))) {
                        log.error("===========================================================================");
                        log.error("即信批次取消债权转让解除冻结资金失败：" + balanceUnfreezeResp.getRetMsg());
                        log.error("===========================================================================");
                    }
                }

                transferBuyLog.setState(2);
                transferBuyLog.setUpdatedAt(new Date());
                //解冻债权转让人购买债权转让冻结资金
                AssetChange assetChange = new AssetChange();
                assetChange.setSourceId(transferBuyLog.getId());
                assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
                assetChange.setMoney(transferBuyLog.getValidMoney());
                assetChange.setSeqNo(assetChangeProvider.getSeqNo());
                assetChange.setRemark(String.format("购买债权转让[%s]失败, 冻结解冻资金%s元",
                        transfer.getTitle(),
                        StringHelper.formatDouble(transferBuyLog.getValidMoney() / 100D,
                                true)));
                assetChange.setType(AssetChangeTypeEnum.unfreeze);
                assetChange.setUserId(transferBuyLog.getUserId());
                try {
                    assetChangeProvider.commonAssetChange(assetChange);
                } catch (Exception e) {
                    log.error("结束债权转让解冻直接失败!", e);
                }
            });
            transferBuyLogService.save(transferBuyLogList);
        }
        //更新债权转让
        transfer.setState(4);
        transfer.setUpdatedAt(new Date());
        transfer.setSuccessAt(null);
        transferService.save(transfer);
        //更新tender
        tender.setTransferFlag(0);
        tender.setUpdatedAt(new Date());
        tenderService.save(tender);

        return ResponseEntity.ok(VoBaseResp.ok("结束债权转让成功!"));
    }


    /**
     * 理财计划债权转让复审
     *
     * @param transferId
     * @param repurchaseFlag 理财计划债权转让是否是赎回
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseEntity<VoBaseResp> againFinanceVerifyTransfer(long transferId, String batchNo, boolean repurchaseFlag) throws Exception {
        Date nowDate = new Date();
        /*
        1.查询债权转让记录与购买记录
        2并且判断债权转让购买记录是否成功购买（在存管登记），
        3.生成tender记录，将购买债权转让信息对应加入tender
        4.生成新的borrowCollection记录,并添加新的待收
        5.将资金扣减
         */
        Transfer transfer = transferService.findByIdLock(transferId);/* 理财计划债权转让记录 */
        Preconditions.checkNotNull(transfer, "理财计划债权转让记录不存在!");
        Preconditions.checkState(transfer.getState() != 2, "债权状态为已转出状态!");
        Tender parentTender = tenderService.findById(transfer.getTenderId());/* 理财计划债权转让原投资记录 */
        Preconditions.checkNotNull(parentTender, "理财计划债权转让源投资记录不存在!tenderId:" + transfer.getTenderId());
        Borrow parentBorrow = borrowService.findById(parentTender.getBorrowId());
        Preconditions.checkNotNull(parentBorrow, "原始借款记录不存在!");
        Specification<TransferBuyLog> tbls = Specifications
                .<TransferBuyLog>and()
                .eq("transferId", transfer.getId())
                .eq("state", 0)
                .build();
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls);/* 购买理财计划债权转让记录 */
        Preconditions.checkState(!CollectionUtils.isEmpty(transferBuyLogList), "购买理财计划债权转让记录不存在!");
        long failure = transferBuyLogList
                .stream()
                .filter(transferBuyLog -> BooleanHelper.isFalse(transferBuyLog.getThirdTransferFlag())).count(); /* 登记即信存管失败条数 */
        if (failure > 0) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "存在未登记即信存管的购买" + failure + "失败记录"));
        }
        //部分转让id
        String borrowCollectionIds = transfer.getBorrowCollectionIds();
        Specification<BorrowCollection> bcs = null;
        if (transfer.getIsAll()) {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("status", 0)
                    .build();
        } else {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("id", borrowCollectionIds.split(","))
                    .eq("status", 0)
                    .build();
        }
        /* 债权转让原投资回款记录 */
        List<BorrowCollection> oldBorrowCollectionList = borrowCollectionService.findList(bcs);
        // 新增子级投标记录,更新老债权记录
        List<Tender> childTenderList = null;
        //理财计划债权转让是否是赎回
        if (repurchaseFlag) {
            log.info("理财计划债权匹配赎回生成子tender开始！");
            //理财计划债权匹配赎回生成子tender
            childTenderList = addChildTender(nowDate, transfer, parentTender, transferBuyLogList);
            log.info("理财计划债权匹配赎回生成子tender结束！");
        } else {
            log.info("理财计划债权匹配购买生成子tender开始！");
            //理财计划债权匹配购买生成子tender
            childTenderList = addFinanceChildTender(nowDate, transfer, parentTender, transferBuyLogList);
            log.info("理财计划债权匹配购买生成子tender结束！");
        }
        //理财计划债权转让是否是赎回
        List<BorrowCollection> borrowCollectionList = null;
        if (repurchaseFlag) {
            // 赎回理财计划债权转让，生成子级债权回款记录，标注老债权回款已经转出
            log.info("赎回理财计划赎回债权转让，生成子级债权回款记录开始！");
            borrowCollectionList = addChildTenderCollection(nowDate, transfer, parentBorrow, childTenderList, oldBorrowCollectionList);
            log.info("赎回理财计划赎回债权转让，生成子级债权回款记录结束！");
        } else {
            log.info("理财计划债权匹配购买，生成子级债权回款记录开始！");
            // 理财计划债权转让，生成子级债权回款记录，标注老债权回款已经转出
            borrowCollectionList = addFinanceChildTenderCollection(nowDate, transfer, parentBorrow, childTenderList);
            log.info("理财计划债权匹配购买，生成子级债权回款记录结束！");
        }
        //理财计划债权转让是否是赎回
        if (repurchaseFlag) {
            //赎回
            //更新购债权人用户资产记录
            updateUserCacheByBuyTransfer(parentBorrow, childTenderList, borrowCollectionList);
            //更新购债权人用户资产记录
            updateUserAssetByBuyTransfer(childTenderList, transfer, borrowCollectionList);
        } else {
            //更新债权转让人用户缓存记录
            updateUserCacheByTransfer(parentBorrow, transfer, borrowCollectionList);
            //更新购买债权人用户缓存
            updateUserAssetByTransfer(transfer, borrowCollectionList);
        }

        // 发放债权转让资金
        log.info("赎回理财计划债权转让，发放债权转让金开始！");
        batchAssetChangeHelper.batchAssetChangeDeal(transferId, batchNo, BatchAssetChangeContants.BATCH_FINANCE_CREDIT_INVEST);
        log.info("赎回理财计划债权转让，发放债权转让金结束！");
        //理财计划债权转让是否是赎回
        if (repurchaseFlag) {
            /* 债权转让本金 */
            long principal = transfer.getPrincipal();
            /* 理财计划购买id */
            long financeBuyId = parentTender.getFinanceBuyId();
            /* 理财计划购买记录 */
            FinancePlanBuyer financePlanBuyer = financePlanBuyerService.findByIdLock(financeBuyId);
            financePlanBuyer.setLeftMoney(financePlanBuyer.getLeftMoney() + principal);
            financePlanBuyer.setRightMoney(financePlanBuyer.getRightMoney() - principal);
            /* 理财计划记录 */
            FinancePlan financePlan = financePlanService.findByIdLock(financePlanBuyer.getPlanId());
            financePlan.setLeftMoney(financePlan.getLeftMoney() + principal);
            financePlan.setRightMoney(financePlan.getRightMoney() - principal);
            //存在理财计划时通知后台
            String resultStr = OKHttpHelper.postJson(adminDomain + "/api/open/finance-plan/auto-match", GSON.toJson(new HashMap<>()), null);
            log.info(" 理财计划债权转让复审 url:" + adminDomain + "/api/open/finance-plan/auto-match");
            log.info(" 理财计划债权转让复审 调用后台返回响应:" + resultStr);

        }

        return ResponseEntity.ok(VoBaseResp.ok("理财计划债权转让复审成功!"));
    }

    /**
     * 生成子级债权回款记录，标注老债权回款已经转出
     *
     * @param nowDate
     * @param transfer
     * @param parentBorrow
     * @param childTenderList
     */
    public List<BorrowCollection> addFinanceChildTenderCollection(Date nowDate, Transfer transfer, Borrow parentBorrow, List<Tender> childTenderList) throws Exception {
        List<BorrowCollection> childTenderCollectionList = new ArrayList<>();/* 债权子记录回款记录 */
        String borrowCollectionIds = transfer.getBorrowCollectionIds();
        //生成子级债权回款记录，标注老债权回款已经转出
        Specification<BorrowCollection> bcs = null;
        if (transfer.getIsAll()) {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("status", 0)
                    .build();
        } else {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("id", borrowCollectionIds.split(","))
                    .eq("status", 0)
                    .build();
        }
        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);/* 债权转让原投资回款记录 */
        long transferInterest = borrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();/* 债权转让总利息 */
        Date repayAt = transfer.getRepayAt();/* 原借款下一期还款日期 */
        Date startAt = null;/* 计息开始时间 */
        if (parentBorrow.getRepayFashion() == 1) {
            startAt = DateHelper.subDays(repayAt, parentBorrow.getTimeLimit());
        } else if (parentBorrow.getRepayFashion() == 0 || parentBorrow.getRepayFashion() == 2) {
            startAt = DateHelper.subMonths(repayAt, 1);
        }

        for (int j = 0; j < childTenderList.size(); j++) {
            Tender childTender = childTenderList.get(j);/* 购买债权转让子投资记录 */
            //生成购买债权转让新的回款记录
            BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(
                    childTender.getValidMoney().doubleValue(),
                    transfer.getApr().doubleValue(),
                    transfer.getTimeLimit(),
                    startAt);
            Map<String, Object> rsMap = borrowCalculatorHelper.simpleCount(parentBorrow.getRepayFashion());
            List<Map<String, Object>> repayDetailList = (List<Map<String, Object>>) rsMap.get("repayDetailList");
            Preconditions.checkState(!CollectionUtils.isEmpty(repayDetailList), "生成用户回款计划开始: 计划生成为空");
            BorrowCollection borrowCollection;
            int startOrder = borrowCollectionList.get(0).getOrder();/* 获取开始转让期数,期数下标从0开始 */
            long sumCollectionInterest = 0l;/*总回款利息*/
            for (int i = 0; i < repayDetailList.size(); i++) {
                borrowCollection = new BorrowCollection();
                Map<String, Object> repayDetailMap = repayDetailList.get(i);
                long principal = NumberHelper.toLong(repayDetailMap.get("principal"));
                long interest = NumberHelper.toLong(repayDetailMap.get("interest"));
                Date collectionAt = DateHelper.stringToDate(StringHelper.toString(repayDetailMap.get("repayAt")));
                sumCollectionInterest += interest;

                //如果是理财计划借款不需要生成利息
                if (childTender.getType().intValue() == 1 || (parentBorrow.getIsFinance() && (childTender.getUserId().longValue() == 22002))) {
                    interest = 0;
                }

                borrowCollection.setTenderId(childTender.getId());
                borrowCollection.setStatus(0);
                borrowCollection.setOrder(startOrder++);
                borrowCollection.setUserId(childTender.getUserId());
                borrowCollection.setStartAt(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : startAt);
                borrowCollection.setStartAtYes(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : nowDate);
                borrowCollection.setCollectionAt(collectionAt);
                borrowCollection.setCollectionAtYes(collectionAt);
                borrowCollection.setCollectionMoney(principal + interest);
                borrowCollection.setPrincipal(principal);
                borrowCollection.setInterest(interest);
                borrowCollection.setCreatedAt(nowDate);
                borrowCollection.setUpdatedAt(nowDate);
                borrowCollection.setCollectionMoneyYes(0L);
                borrowCollection.setLateDays(0);
                borrowCollection.setLateInterest(0L);
                borrowCollection.setBorrowId(parentBorrow.getId());
                childTenderCollectionList.add(borrowCollection);

            }
            borrowCollectionService.save(childTenderCollectionList);
        }

        //更新转出投资记录回款状态
        borrowCollectionList.stream().forEach(bc -> {
            bc.setTransferFlag(1);
        });
        borrowCollectionService.save(borrowCollectionList);

        return childTenderCollectionList;
    }

    /**
     * 新增理财计划子级标的
     *
     * @param nowDate
     * @param transfer
     * @param parentTender
     * @param transferBuyLogList
     * @return
     */
    public List<Tender> addFinanceChildTender(Date nowDate, Transfer transfer, Tender parentTender, List<TransferBuyLog> transferBuyLogList) {
        //生成债权记录与回款记录
        List<Tender> childTenderList = new ArrayList<>();
        transferBuyLogList.stream().forEach(transferBuyLog -> {
            Tender childTender = new Tender();
            UserThirdAccount buyUserThirdAccount = userThirdAccountService.findByUserId(transferBuyLog.getUserId());

            childTender.setUserId(transferBuyLog.getUserId());
            childTender.setStatus(1);
            childTender.setType(transferBuyLog.getType());
            childTender.setBorrowId(transfer.getBorrowId());
            childTender.setSource(transferBuyLog.getSource());
            childTender.setIsAuto(transferBuyLog.getAuto());
            childTender.setAutoOrder(transferBuyLog.getAutoOrder());
            childTender.setMoney(transferBuyLog.getBuyMoney());
            childTender.setValidMoney(transferBuyLog.getPrincipal());
            childTender.setTransferFlag(0);
            childTender.setTUserId(transferBuyLog.getUserId());
            childTender.setParentId(parentTender.getId());
            childTender.setState(2);
            childTender.setFinanceBuyId(transferBuyLog.getFinanceBuyId());
            childTender.setTransferBuyId(transferBuyLog.getId());
            childTender.setAlreadyInterest(transferBuyLog.getAlreadyInterest());
            childTender.setThirdTenderOrderId(transferBuyLog.getThirdTransferOrderId());
            childTender.setAuthCode(transferBuyLog.getTransferAuthCode());
            childTender.setCreatedAt(nowDate);
            childTender.setUpdatedAt(nowDate);
            childTenderList.add(childTender);

            //更新购买信用标状态为成功购买
            transferBuyLog.setState(1);
            transferBuyLog.setUpdatedAt(new Date());
        });
        transferBuyLogService.save(transferBuyLogList);

        //如果债权转让本金未全部转出 默认发布者自己承接剩余部分
        /* 已转出本金 */
        long boughtPrincipal = 0l;
        for (TransferBuyLog transferBuyLog : transferBuyLogList) {
            boughtPrincipal += transferBuyLog.getPrincipal();
        }
        /* 剩余未转出本金 */
        long surplusPrincipal = transfer.getPrincipal() - boughtPrincipal;
        if (surplusPrincipal > 0) {
            Tender childTender = new Tender();
            childTender.setUserId(parentTender.getUserId());
            childTender.setStatus(1);
            childTender.setType(0);//普通投标
            childTender.setBorrowId(transfer.getBorrowId());
            childTender.setSource(0);
            childTender.setMoney(surplusPrincipal);
            childTender.setValidMoney(surplusPrincipal);
            childTender.setTransferFlag(0);
            childTender.setTUserId(parentTender.getUserId());
            childTender.setState(2);
            childTender.setAutoOrder(0);
            childTender.setParentId(parentTender.getId());
            childTender.setAlreadyInterest(0l);//无当期应计利息
            childTender.setThirdTenderOrderId(parentTender.getThirdTenderOrderId());
            childTender.setThirdTransferOrderId(parentTender.getThirdTransferOrderId());
            childTender.setAuthCode(parentTender.getAuthCode());
            childTender.setCreatedAt(nowDate);
            childTender.setUpdatedAt(nowDate);
            childTenderList.add(childTender);
        }
        //保存生成投标记录
        tenderService.save(childTenderList);
        //更新老债权为已转让 部分转让暂不记录  改为0
        parentTender.setTransferFlag(transfer.getIsAll() ? 2 : 0);
        parentTender.setUpdatedAt(nowDate);
        tenderService.save(parentTender);
        //更新债权转让为已转让
        transfer.setState(2);
        transfer.setUpdatedAt(new Date());
        transfer.setRecheckAt(new Date());
        transferService.save(transfer);
        return childTenderList;
    }

    /**
     * 债权转让复审
     *
     * @param transferId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseEntity<VoBaseResp> againVerifyTransfer(long transferId, String batchNo) throws Exception {
        Date nowDate = new Date();
        /*
        1.查询债权转让记录与购买记录
        2并且判断债权转让购买记录是否成功购买（在存管登记），
        3.生成tender记录，将购买债权转让信息对应加入tender
        4.生成新的borrowCollection记录,并添加新的待收
        5.将资金扣减
         */
        Transfer transfer = transferService.findByIdLock(transferId);/* 债权转让记录 */
        Preconditions.checkNotNull(transfer, "债权转让记录不存在!");
        Preconditions.checkState(transfer.getState() != 2, "债权状态为已转出状态!");
        Tender parentTender = tenderService.findById(transfer.getTenderId());/* 债权转让原投资记录 */
        Preconditions.checkNotNull(parentTender, "债权转让源投资记录不存在!tenderId:" + transfer.getTenderId());
        Borrow parentBorrow = borrowService.findById(parentTender.getBorrowId());
        Preconditions.checkNotNull(parentBorrow, "原始借款记录不存在!");
        Specification<TransferBuyLog> tbls = Specifications
                .<TransferBuyLog>and()
                .eq("transferId", transfer.getId())
                .eq("state", 0)
                .build();
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls);/* 购买债权转让记录 */
        Preconditions.checkState(!CollectionUtils.isEmpty(transferBuyLogList), "购买债权转让记录不存在!");
        long failure = transferBuyLogList
                .stream()
                .filter(transferBuyLog -> BooleanHelper.isFalse(transferBuyLog.getThirdTransferFlag())).count(); /* 登记即信存管失败条数 */
        if (failure > 0) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "存在未登记即信存管的购买" + failure + "失败记录"));
        }

        //部分转让id
        String borrowCollectionIds = transfer.getBorrowCollectionIds();
        Specification<BorrowCollection> bcs = null;
        if (transfer.getIsAll()) {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("status", 0)
                    .build();
        } else {
            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("tenderId", transfer.getTenderId())
                    .eq("id", borrowCollectionIds.split(","))
                    .eq("status", 0)
                    .build();
        }
        /* 债权转让原投资回款记录 */
        List<BorrowCollection> oldBorrowCollectionList = borrowCollectionService.findList(bcs);

        // 新增子级投标记录,更新老债权记录
        List<Tender> childTenderList = addChildTender(nowDate, transfer, parentTender, transferBuyLogList);

        // 生成子级债权回款记录，标注老债权回款已经转出
        List<BorrowCollection> childBorrowCollectionList = addChildTenderCollection(nowDate, transfer, parentBorrow, childTenderList, oldBorrowCollectionList);

        // 发放债权转让资金
        batchAssetChangeHelper.batchAssetChangeDeal(transferId, batchNo, BatchAssetChangeContants.BATCH_CREDIT_INVEST);

        // 发送投资成功站内信
        sendNoticsByBuyTransfer(transfer, childTenderList);

        // 用户投标信息和每日统计
        userTenderStatistic(childTenderList);

        //更新债权转让人用户缓存记录
        updateUserCacheByTransfer(parentBorrow, transfer, oldBorrowCollectionList);
        //更新债权转让人用户缓存
        updateUserAssetByTransfer(transfer, oldBorrowCollectionList);

        //更新债权转让人用户资产记录
        updateUserCacheByBuyTransfer(parentBorrow, childTenderList, childBorrowCollectionList);
        //更新购债权人用户资产记录
        updateUserAssetByBuyTransfer(childTenderList, transfer, childBorrowCollectionList);

        //更新全网网站统计
        updateStatisticByTransferReview(transfer);
        return ResponseEntity.ok(VoBaseResp.ok("债权转让复审成功!"));
    }

    /**
     * 更新债权转让人用户缓存
     *
     * @param transfer
     * @param oldBorrowCollectionList
     */
    private void updateUserAssetByTransfer(Transfer transfer, List<BorrowCollection> oldBorrowCollectionList) {
        /* 购买转让标 每期还款本金+利息 */
        long countCollectionMoney = oldBorrowCollectionList.stream().mapToLong(BorrowCollection::getCollectionMoney).sum();
        /* 购买转让标 每期还款利息 */
        long countInterest = oldBorrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();
        //减去债权转让人待收
        AssetChange assetChange = new AssetChange();
        assetChange.setType(AssetChangeTypeEnum.collectionSub);  //  减去债权转让人待收
        assetChange.setUserId(transfer.getUserId());
        assetChange.setMoney(countCollectionMoney);
        assetChange.setInterest(countInterest);
        assetChange.setRemark(String.format("转出债权转让[%s]借款,扣除待收", transfer.getTitle()));
        assetChange.setSourceId(transfer.getId());
        assetChange.setSeqNo(assetChangeProvider.getSeqNo());
        assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
        try {
            assetChangeProvider.commonAssetChange(assetChange);
        } catch (Exception e) {
            log.error(String.format("transferBizImpl updateUserAssetByTransfer 资金变动失败：%s", assetChange));
        }

    }

    /**
     * 更新购债权人用户资产记录
     *
     * @param childTenderList
     * @param childBorrowCollectionList
     * @//
     */
    private void updateUserAssetByBuyTransfer(List<Tender> childTenderList, Transfer transfer, List<BorrowCollection> childBorrowCollectionList) {
        Map<Long/* 投资id */, List<BorrowCollection>> childBorrowCollectionMaps = childBorrowCollectionList.stream()
                .collect(Collectors.groupingBy(BorrowCollection::getTenderId));
        childTenderList.stream().forEach(tender -> {
            List<BorrowCollection> borrowCollectionList = childBorrowCollectionMaps.get(tender.getId());
            long countCollectionMoney = borrowCollectionList.stream().mapToLong(BorrowCollection::getCollectionMoney).sum();/* 购买转让标 每期还款本金+利息 */
            long countInterest = borrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();/* 购买转让标 每期还款利息 */
            //增加债权转让购买人待收
            AssetChange assetChange = new AssetChange();
            assetChange.setType(AssetChangeTypeEnum.collectionAdd);  //  增加债权转让购买人待收
            assetChange.setUserId(tender.getUserId());
            assetChange.setMoney(countCollectionMoney);
            assetChange.setInterest(countInterest);
            assetChange.setRemark(String.format("购买债权转让[%s]借款,扣除待收", transfer.getTitle()));
            assetChange.setSourceId(tender.getId());
            assetChange.setSeqNo(assetChangeProvider.getSeqNo());
            assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
            try {
                assetChangeProvider.commonAssetChange(assetChange);
            } catch (Exception e) {
                log.error(String.format("transferBizImpl updateUserAssetByBuyTransfer 资金变动失败：%s", assetChange));
            }
        });
    }

    /**
     * 更新购买债权人用户缓存
     *
     * @param parentBorrow
     * @param childTenderList
     * @param childBorrowCollectionList
     */
    private void updateUserCacheByBuyTransfer(Borrow parentBorrow, List<Tender> childTenderList, List<BorrowCollection> childBorrowCollectionList) {
        Map<Long/* 投资id */, List<BorrowCollection>> childBorrowCollectionMaps = childBorrowCollectionList.stream()
                .collect(Collectors.groupingBy(BorrowCollection::getTenderId));
        childTenderList.stream().forEach(tender -> {
            List<BorrowCollection> borrowCollectionList = childBorrowCollectionMaps.get(tender.getId());
            UserCache userCache = userCacheService.findById(tender.getUserId());
            /* 购买转让标 每期还款利息 */
            long countInterest = borrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();
            if (parentBorrow.getType() == 0) {
                userCache.setTjWaitCollectionInterest(userCache.getTjWaitCollectionInterest() + countInterest);
                userCache.setTjWaitCollectionPrincipal(userCache.getTjWaitCollectionPrincipal() + tender.getValidMoney());
            } else if (parentBorrow.getType() == 4) {
                userCache.setQdWaitCollectionPrincipal(userCache.getQdWaitCollectionPrincipal() + tender.getValidMoney());
                userCache.setQdWaitCollectionInterest(userCache.getQdWaitCollectionInterest() + countInterest);
            }
            //更新待支出利息管理费
            if (parentBorrow.getRecheckAt().getTime() < DateHelper.stringToDate("2017-11-01 00:00:00").getTime() && ImmutableSet.of(0, 4).contains(parentBorrow.getType())) {
                userCache.setWaitExpenditureInterestManage(userCache.getWaitExpenditureInterestManage() + new Double(MoneyHelper.round(MoneyHelper.divide(countInterest, 0.1), 0)).longValue());
                userService.repairWaitExpenditureInterestManage();
            }
            userCacheService.save(userCache);
        });
    }


    /**
     * 发送投资成功站内信
     *
     * @param transfer
     * @param tenderList
     */
    private void sendNoticsByBuyTransfer(Transfer transfer, List<Tender> tenderList) {
        Gson gson = new Gson();
        log.info(String.format("发送投标成功站内信开始: %s", gson.toJson(tenderList)));
        Date nowDate = new Date();
        Set<Long> userIdSet = tenderList.stream().map(tender -> tender.getUserId()).collect(Collectors.toSet());
        for (Long userId : userIdSet) {
            Notices notices = new Notices();
            notices.setFromUserId(1L);
            notices.setUserId(userId);
            notices.setRead(false);
            notices.setName("投资的借款满标审核通过");
            notices.setContent("您所投资的债权转让借款[" + transfer.getTitle() + " 已满标审核通过");
            notices.setType("system");
            notices.setCreatedAt(nowDate);
            notices.setUpdatedAt(nowDate);
            //发送站内信
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_NOTICE);
            mqConfig.setTag(MqTagEnum.NOTICE_PUBLISH);
            Map<String, String> body = GSON.fromJson(GSON.toJson(notices), TypeTokenContants.MAP_TOKEN);
            mqConfig.setMsg(body);
            try {
                log.info(String.format("transferBizImpl sendNoticsByTender send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("transferBizImpl sendNoticsByTender send mq exception", e);
            }
        }
        log.info(String.format("发送投标成功站内信结束:  %s", gson.toJson(tenderList)));
    }

    /**
     * 用户投标统计
     *
     * @param tenderList
     */
    private void userTenderStatistic(List<Tender> tenderList) throws Exception {
        Gson gson = new Gson();
        for (Tender tender : tenderList) {
            log.info(String.format("投标统计: %s", gson.toJson(tender)));
            UserCache userCache = userCacheService.findById(tender.getUserId());
            IncrStatistic incrStatistic = new IncrStatistic();
            if ((!BooleanUtils.toBoolean(userCache.getTenderTransfer())) && (!BooleanUtils.toBoolean(userCache.getTenderTuijian()))
                    && (!BooleanUtils.toBoolean(userCache.getTenderJingzhi())) && (!BooleanUtils.toBoolean(userCache.getTenderMiao()))
                    && (!BooleanUtils.toBoolean(userCache.getTenderQudao()))) {
                incrStatistic.setTenderCount(1);
                incrStatistic.setTenderTotal(1);
            }

            incrStatistic.setTenderLzCount(1);
            incrStatistic.setTenderLzTotalCount(1);
            userCache.setTenderTransfer(tender.getBorrowId().intValue());

            userCacheService.save(userCache);
            if (!ObjectUtils.isEmpty(incrStatistic)) {
                incrStatisticBiz.caculate(incrStatistic);
            }
        }
    }

    /**
     * 更新债权转让人用户缓存记录
     *
     * @param transfer
     */
    private void updateUserCacheByTransfer(Borrow parentBorrow, Transfer transfer, List<BorrowCollection> childBorrowCollectionList) throws Exception {
        UserCache userCache = userCacheService.findById(transfer.getUserId());
        userCache.setUserId(userCache.getUserId());
        /*转出的总利息*/
        long countInterest = childBorrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();
        if (parentBorrow.getType() == 0) {
            userCache.setTjWaitCollectionPrincipal(userCache.getTjWaitCollectionPrincipal() - transfer.getPrincipal());
            userCache.setTjWaitCollectionInterest(userCache.getTjWaitCollectionInterest() - countInterest);
        } else if (parentBorrow.getType() == 4) {
            userCache.setQdWaitCollectionPrincipal(userCache.getQdWaitCollectionPrincipal() - transfer.getPrincipal());
            userCache.setQdWaitCollectionInterest(userCache.getQdWaitCollectionInterest() - countInterest);
        }
        //更新待支出利息管理费
        if (parentBorrow.getRecheckAt().getTime() < DateHelper.stringToDate("2017-11-01 00:00:00").getTime() && ImmutableSet.of(0, 4).contains(parentBorrow.getType())) {
            userCache.setWaitExpenditureInterestManage(userCache.getWaitExpenditureInterestManage() - new Double(MoneyHelper.round(MoneyHelper.divide(countInterest, 0.1), 0)).longValue());
            userService.repairWaitExpenditureInterestManage();
        }
        userCacheService.save(userCache);
    }


    /**
     * 更新网站统计
     *
     * @param transfer
     */
    private void updateStatisticByTransferReview(Transfer transfer) {
        //全站统计
        Statistic statistic = new Statistic();
        statistic.setLzBorrowTotal(transfer.getPrincipal());
        if (!ObjectUtils.isEmpty(statistic)) {
            try {
                statisticBiz.caculate(statistic);
            } catch (Throwable e) {
                log.error("transferBizImpl updateStatisticByTransferReview 异常:", e);
            }
        }
    }

    /**
     * 生成子级债权回款记录，标注老债权回款已经转出
     *
     * @param nowDate
     * @param transfer
     * @param parentBorrow
     * @param childTenderList
     */
    @Override
    public List<BorrowCollection> addChildTenderCollection(Date nowDate, Transfer transfer, Borrow parentBorrow, List<Tender> childTenderList, List<BorrowCollection> oldBorrowCollectionList) throws Exception {
        List<BorrowCollection> childTenderCollectionList = new ArrayList<>();/* 债权子记录回款记录 */
        //生成子级债权回款记录，标注老债权回款已经转出
        /* 债权转让总利息 */
        long transferInterest = oldBorrowCollectionList.stream().mapToLong(BorrowCollection::getInterest).sum();
        Date repayAt = transfer.getRepayAt();/* 原借款下一期还款日期 */
        Date startAt = null;/* 计息开始时间 */
        if (parentBorrow.getRepayFashion() == 1) {
            startAt = DateHelper.subDays(repayAt, parentBorrow.getTimeLimit());
        } else if (parentBorrow.getRepayFashion() == 0 || parentBorrow.getRepayFashion() == 2) {
            startAt = DateHelper.subMonths(repayAt, 1);
        }

        long sumCollectionInterest = 0;//总回款利息
        for (int j = 0; j < childTenderList.size(); j++) {
            Tender childTender = childTenderList.get(j);/* 购买债权转让子投资记录 */
            //生成购买债权转让新的回款记录
            BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(
                    childTender.getValidMoney().doubleValue(),
                    transfer.getApr().doubleValue(),
                    transfer.getTimeLimit(),
                    startAt);
            Map<String, Object> rsMap = borrowCalculatorHelper.simpleCount(parentBorrow.getRepayFashion());
            List<Map<String, Object>> repayDetailList = (List<Map<String, Object>>) rsMap.get("repayDetailList");
            Preconditions.checkState(!CollectionUtils.isEmpty(repayDetailList), "生成用户回款计划开始: 计划生成为空");
            BorrowCollection borrowCollection;
            long collectionMoney = 0;
            int startOrder = oldBorrowCollectionList.get(0).getOrder();/* 获取开始转让期数,期数下标从0开始 */
            for (int i = 0; i < repayDetailList.size(); i++) {
                borrowCollection = new BorrowCollection();
                Map<String, Object> repayDetailMap = repayDetailList.get(i);
                collectionMoney += new Double(NumberHelper.toDouble(repayDetailMap.get("repayMoney"))).longValue();
                long interest = new Double(NumberHelper.toDouble(repayDetailMap.get("interest"))).longValue();
                sumCollectionInterest += interest;
                //最后一个购买债权转让的最后一期回款，需要把转让溢出的利息补给新的回款记录
                //排除理财计划转让，因为理财计划没有回款利息
                if ((j == childTenderList.size() - 1) && (i == repayDetailList.size() - 1) && transfer.getType().intValue() != 1) {
                    /* 新的回款利息添加溢出的利息 */
                    long difference = transferInterest - sumCollectionInterest;
                    interest += difference;
                    collectionMoney += difference;
                }

                borrowCollection.setTenderId(childTender.getId());
                borrowCollection.setStatus(0);
                borrowCollection.setOrder(startOrder++);
                borrowCollection.setUserId(childTender.getUserId());
                borrowCollection.setStartAt(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : startAt);
                borrowCollection.setStartAtYes(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : nowDate);
                borrowCollection.setCollectionAt(DateHelper.stringToDate(StringHelper.toString(repayDetailMap.get("repayAt"))));
                borrowCollection.setCollectionAtYes(DateHelper.stringToDate(StringHelper.toString(repayDetailMap.get("repayAt"))));
                borrowCollection.setCollectionMoney(NumberHelper.toLong(repayDetailMap.get("repayMoney")));
                borrowCollection.setPrincipal(NumberHelper.toLong(repayDetailMap.get("principal")));
                borrowCollection.setInterest(interest);
                borrowCollection.setCreatedAt(nowDate);
                borrowCollection.setUpdatedAt(nowDate);
                borrowCollection.setCollectionMoneyYes(0L);
                borrowCollection.setLateInterest(0L);
                borrowCollection.setLateDays(0);
                borrowCollection.setBorrowId(parentBorrow.getId());
                childTenderCollectionList.add(borrowCollection);
            }
            borrowCollectionService.save(childTenderCollectionList);
        }

        //更新转出投资记录回款状态
        oldBorrowCollectionList.stream().forEach(bc -> {
            bc.setTransferFlag(1);
        });
        borrowCollectionService.save(oldBorrowCollectionList);

        return childTenderCollectionList;
    }

    /**
     * 新增子级标的
     *
     * @param nowDate
     * @param transfer
     * @param parentTender
     * @param transferBuyLogList
     * @return
     */
    public List<Tender> addChildTender(Date nowDate, Transfer transfer, Tender parentTender, List<TransferBuyLog> transferBuyLogList) {
        //生成债权记录与回款记录
        List<Tender> childTenderList = new ArrayList<>();
        transferBuyLogList.stream().forEach(transferBuyLog -> {
            Tender childTender = new Tender();
            UserThirdAccount buyUserThirdAccount = userThirdAccountService.findByUserId(transferBuyLog.getUserId());

            childTender.setUserId(transferBuyLog.getUserId());
            childTender.setStatus(1);
            childTender.setType(transferBuyLog.getType());
            childTender.setBorrowId(transfer.getBorrowId());
            childTender.setSource(transferBuyLog.getSource());
            childTender.setIsAuto(transferBuyLog.getAuto());
            childTender.setAutoOrder(transferBuyLog.getAutoOrder());
            childTender.setMoney(transferBuyLog.getBuyMoney());
            childTender.setValidMoney(transferBuyLog.getPrincipal());
            childTender.setFinanceBuyId(transferBuyLog.getFinanceBuyId());
            childTender.setTransferFlag(0);
            childTender.setTUserId(buyUserThirdAccount.getUserId());
            childTender.setState(2);
            childTender.setParentId(parentTender.getId());
            childTender.setTransferBuyId(transferBuyLog.getId());
            childTender.setAlreadyInterest(transferBuyLog.getAlreadyInterest());
            childTender.setThirdTenderOrderId(transferBuyLog.getThirdTransferOrderId());
            childTender.setAuthCode(transferBuyLog.getTransferAuthCode());
            childTender.setCreatedAt(nowDate);
            childTender.setUpdatedAt(nowDate);
            childTenderList.add(childTender);

            //更新购买信用标状态为成功购买
            transferBuyLog.setState(1);
            transferBuyLog.setUpdatedAt(new Date());
        });
        transferBuyLogService.save(transferBuyLogList);
        //保存生成投标记录
        tenderService.save(childTenderList);
        //更新老债权为已转让 部分转让暂不记录  改为0
        parentTender.setTransferFlag(transfer.getIsAll() ? 2 : 0);
        parentTender.setUpdatedAt(nowDate);
        tenderService.save(parentTender);
        //更新债权转让为已转让
        transfer.setState(2);
        transfer.setUpdatedAt(new Date());
        transfer.setRecheckAt(new Date());
        transferService.save(transfer);
        return childTenderList;
    }

    /**
     * 债权转让初审
     *
     * @param voPcFirstVerityTransfer
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> firstVerifyTransfer(VoPcFirstVerityTransfer voPcFirstVerityTransfer) throws Exception {
        String paramStr = voPcFirstVerityTransfer.getParamStr();
        if (!SecurityHelper.checkSign(voPcFirstVerityTransfer.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "债权转让初审 签名验证不通过!"));
        }

        Map<String, String> paramMap = new Gson().fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long transferId = NumberHelper.toLong(paramMap.get("transferId"));
        if (doFirstVerifyTransfer(transferId)) {
            return ResponseEntity.ok(VoBaseResp.ok("债权转让初审初审成功!"));
        } else {
            return ResponseEntity.
                    badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "债权转让初审初审失败,transferId：" + transferId));
        }
    }

    /**
     * 去债权转让初审
     *
     * @param transferId
     * @throws Exception
     */
    private boolean doFirstVerifyTransfer(long transferId) throws Exception {
        //更新债权转让状态
        Date nowDate = DateHelper.subSeconds(new Date(), 10);
        Transfer transfer = transferService.findById(transferId);
        transfer.setVerifyAt(nowDate);
        transfer.setState(1);
        Date releaseAt = transfer.getReleaseAt();
        transfer.setReleaseAt(ObjectUtils.isEmpty(releaseAt) ? nowDate : releaseAt);
        transferService.save(transfer);

        // 自动购买债权转让前提:
        // 1.标的年化率为 800 以上
        int apr = transfer.getApr();
        if (apr > 800) {
            transfer.setIsLock(true);
            transferService.save(transfer);  // 锁住债权转让,禁止手动购买

            //触发自动投标队列
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_TRANSFER);
            mqConfig.setTag(MqTagEnum.AUTO_TRANSFER);
            mqConfig.setSendTime(releaseAt);
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.MSG_TRANSFER_ID, StringHelper.toString(transferId), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
            mqConfig.setMsg(body);
            try {
                log.info(String.format("transferBizImpl doFirstVerifyTransfer send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
                return true;
            } catch (Throwable e) {
                log.error("transferBizImpl doFirstVerifyTransfer send mq exception", e);
                return false;
            }
        }
        return true;
    }

    /**
     * 购买债权转让
     * 1.判断投资人是否存管开户、并且签约
     * 2.判断债权转让剩余金额是否大于等于购买金额
     * 3.判断账户可用金额是否大于购入金额
     * 4.生成购买债权记录
     * 
     * @// TODO: 2017/12/19 大转盘活动
     * .每天单笔投资每满10,000元即可获得1次抽奖机会，按投资金额自动计算分配；
    4.累计投资金额每满100,000元，可另获赠1次抽奖机会，以此类推；
    活动期间新邀请好友投资金额可等同于自己投资送抽奖次数，规则同3、4条；
    6.本活动仅限车贷标、渠道标、流转标参与，其他标的均不参与该活动；
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseEntity<VoBaseResp> buyTransfer(VoBuyTransfer voBuyTransfer) throws Exception {
        String msg = null;
        String orderId = JixinHelper.getOrderId(JixinHelper.BALANCE_FREEZE_PREFIX);/* 购买债权转让冻结金额 orderid */
        long userId = voBuyTransfer.getUserId(); /*购买人id*/
        long transferId = voBuyTransfer.getTransferId(); /*债权转让记录id*/
        long buyMoney = voBuyTransfer.getBuyMoney().longValue(); /*购买债权转让金额*/
        boolean auto = voBuyTransfer.getAuto(); /* 是否是自动购买债权转让 */
        int autoOrder = voBuyTransfer.getAutoOrder(); /* 自动投标order编号 */

        UserThirdAccount buyUserThirdAccount = userThirdAccountService.findByUserId(userId);/*购买人存管信息*/
        if (!buyUserThirdAccount.getAutoTransferState().equals(1)) {  // 审核
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_CREDIT_TRNSFER, "请先签订自动债权转让协议！", VoBaseResp.class));
        }
        Transfer transfer = transferService.findByIdLock(transferId);/*债权转让记录*/
        Preconditions.checkNotNull(transfer, "债权转让记录不存在!");
        Asset asset = assetService.findByUserIdLock(userId);/* 购买人资产记录 */
        Preconditions.checkNotNull(asset, "购买人资产记录不存在!");

        //验证债权转让
        ImmutableMap<String, Object> verifyTransferMap = verifyTransfer(buyMoney, transfer);
        msg = StringHelper.toString(verifyTransferMap.get(MSG));
        if (!StringUtils.isEmpty(msg)) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, msg));
        }
        long leftMoney = NumberHelper.toLong(verifyTransferMap.get(LEFT_MONEY));
        long validMoney = new Double(MathHelper.min(leftMoney, buyMoney)).longValue();/* 可购债权金额  */
        long alreadyInterest = new Double(MoneyHelper.multiply(MoneyHelper.divide(validMoney, transfer.getTransferMoney()), transfer.getAlreadyInterest())).longValue();/* 应付给债权转让人的当期应计利息 */

        // 验证购买人账户
        ImmutableMap<String, Object> verifyBuyTransferUserMap = verifyBuyTransferUser(buyUserThirdAccount, asset, validMoney);
        msg = StringHelper.toString(verifyBuyTransferUserMap.get(MSG));
        if (!StringUtils.isEmpty(msg)) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, msg));
        }

        if (transfer.getUserId().intValue() == userId) {
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "不能购买自己的债权"));
        }

        //生成购买债权记录
        TransferBuyLog transferBuyLog = saveTransferAndTransferLog(userId, transferId, buyMoney, transfer, validMoney, alreadyInterest, auto, autoOrder);

        try {
            //更新购买人账户金额
            updateAssetByBuyUser(transferBuyLog, transfer, buyUserThirdAccount, orderId);

            //判断是否满标，满标触发债权转让复审
            if (transfer.getTransferMoney().intValue() <= transfer.getTransferMoneyYes().intValue()) {
                MqConfig mqConfig = new MqConfig();
                mqConfig.setQueue(MqQueueEnum.RABBITMQ_TRANSFER);
                mqConfig.setTag(MqTagEnum.AGAIN_VERIFY_TRANSFER);
                ImmutableMap<String, String> body = ImmutableMap
                        .of(MqConfig.MSG_TRANSFER_ID, StringHelper.toString(transferId), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
                mqConfig.setMsg(body);
                log.info(String.format("transferBizImpl buyTransfer send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            }
        } catch (Exception e) {
            String newOrderId = JixinHelper.getOrderId(JixinHelper.BALANCE_UNFREEZE_PREFIX);// 购买债权转让冻结金额 orderid
            //解除存管资金冻结
            BalanceUnfreezeReq balanceUnfreezeReq = new BalanceUnfreezeReq();
            balanceUnfreezeReq.setAccountId(buyUserThirdAccount.getAccountId());
            balanceUnfreezeReq.setTxAmount(StringHelper.formatDouble(transferBuyLog.getValidMoney() / 100.0, false));
            balanceUnfreezeReq.setChannel(ChannelContant.HTML);
            balanceUnfreezeReq.setOrderId(newOrderId);
            balanceUnfreezeReq.setOrgOrderId(orderId);
            BalanceUnfreezeResp balanceUnfreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_UN_FREEZE, balanceUnfreezeReq, BalanceUnfreezeResp.class);
            if ((ObjectUtils.isEmpty(balanceUnfreezeResp)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceUnfreezeResp.getRetCode()))) {
                throw new Exception("购买债权转让解冻资金失败：" + balanceUnfreezeResp.getRetMsg());
            }
        }

        return ResponseEntity.ok(VoBaseResp.ok("购买成功!"));
    }

    /**
     * 保存债权转让与购买债权转让记录
     *
     * @param userId
     * @param transferId
     * @param buyMoney
     * @param transfer
     * @param validMoney
     * @param alreadyInterest
     */
    private TransferBuyLog saveTransferAndTransferLog(long userId, long transferId, long buyMoney, Transfer transfer, long validMoney, long alreadyInterest, boolean auto, int autoOrder) {
        long buyPrincipal = validMoney - alreadyInterest;
        Specification<TransferBuyLog> tbls = Specifications
                .<TransferBuyLog>and()
                .in("state", 0, 1)
                .eq("transferId", transferId)
                .build();
        List<TransferBuyLog> transferBuyLogList = transferBuyLogService.findList(tbls);
        /*已购买金额*/
        long boughtPrincipal = transferBuyLogList.stream().mapToLong(TransferBuyLog::getPrincipal).sum();
        /*剩余可购买金额*/
        long leftPrincipal = transfer.getPrincipal() - boughtPrincipal;
        //如果检测到最后一个购买金额本金溢出，则重新计算本金与利息
        if (buyPrincipal > leftPrincipal) {
            buyPrincipal = leftPrincipal;
            alreadyInterest = validMoney - buyPrincipal;
        }
        //新增购买债权记录
        TransferBuyLog transferBuyLog = new TransferBuyLog();
        transferBuyLog.setUserId(userId);
        transferBuyLog.setType(0);
        transferBuyLog.setState(0);
        transferBuyLog.setAuto(auto);
        transferBuyLog.setBuyMoney(buyMoney);
        transferBuyLog.setValidMoney(validMoney);
        transferBuyLog.setPrincipal(buyPrincipal);
        transferBuyLog.setCreatedAt(new Date());
        transferBuyLog.setUpdatedAt(new Date());
        transferBuyLog.setDel(false);
        transferBuyLog.setAutoOrder(autoOrder);
        transferBuyLog.setTransferId(transferId);
        transferBuyLog.setAlreadyInterest(NumberHelper.toLong(alreadyInterest));
        transferBuyLog.setSource(0);

        transferBuyLogService.save(transferBuyLog);

        //更新债权抓让信息
        transfer.setTenderCount(NumberHelper.toInt(transfer.getTenderCount()) + 1);
        transfer.setTransferMoneyYes(transfer.getTransferMoneyYes() + validMoney);
        transfer.setUpdatedAt(new Date());
        transferService.save(transfer);
        return transferBuyLog;
    }

    /**
     * 更新购买人资产
     *
     * @param transferBuyLog
     * @param transfer
     */
    private void updateAssetByBuyUser(TransferBuyLog transferBuyLog, Transfer transfer, UserThirdAccount buyUserThirdAccount, String orderId) throws Exception {
        AssetChange assetChange = new AssetChange();
        assetChange.setSourceId(transferBuyLog.getId());
        assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
        assetChange.setMoney(transferBuyLog.getValidMoney());
        assetChange.setSeqNo(assetChangeProvider.getSeqNo());
        assetChange.setRemark(String.format("购买债权转让[%s], 成功冻结资金%s元", transfer.getTitle(), StringHelper.formatDouble(transferBuyLog.getValidMoney() / 100D, true)));
        assetChange.setType(AssetChangeTypeEnum.freeze);
        assetChange.setUserId(transferBuyLog.getUserId());
        assetChangeProvider.commonAssetChange(assetChange);
        //冻结购买债权转让人资金账户
        BalanceFreezeReq balanceFreezeReq = new BalanceFreezeReq();
        balanceFreezeReq.setAccountId(buyUserThirdAccount.getAccountId());
        balanceFreezeReq.setTxAmount(StringHelper.formatDouble(transferBuyLog.getValidMoney() / 100.0, false));
        balanceFreezeReq.setOrderId(orderId);
        balanceFreezeReq.setChannel(ChannelContant.HTML);
        BalanceFreezeResp balanceFreezeResp = jixinManager.send(JixinTxCodeEnum.BALANCE_FREEZE, balanceFreezeReq, BalanceFreezeResp.class);
        if ((ObjectUtils.isEmpty(balanceFreezeReq)) || (!JixinResultContants.SUCCESS.equalsIgnoreCase(balanceFreezeResp.getRetCode()))) {
            throw new Exception("即信批次还款冻结资金失败：" + balanceFreezeResp.getRetMsg());
        }
        //保存存管冻结orderId
        transferBuyLog.setFreezeOrderId(orderId);
        transferBuyLogService.save(transferBuyLog);
    }

    /**
     * 校验购买人账户
     *
     * @param buyUserThirdAccount
     * @param asset
     * @param validMoney
     * @return
     */
    private ImmutableMap<String, Object> verifyBuyTransferUser(UserThirdAccount buyUserThirdAccount, Asset asset, double validMoney) {
        String msg = "";
        if (validMoney > asset.getUseMoney()) {
            msg = "账户余额不足，请先充值或同步资金!";
        }

        // 查询存管系统资金
        BalanceQueryRequest balanceQueryRequest = new BalanceQueryRequest();
        balanceQueryRequest.setChannel(ChannelContant.HTML);
        balanceQueryRequest.setAccountId(buyUserThirdAccount.getAccountId());
        BalanceQueryResponse balanceQueryResponse = jixinManager.send(JixinTxCodeEnum.BALANCE_QUERY, balanceQueryRequest, BalanceQueryResponse.class);
        if ((ObjectUtils.isEmpty(balanceQueryResponse)) || !balanceQueryResponse.getRetCode().equals(JixinResultContants.SUCCESS)) {
            msg = "当前网络不稳定,请稍后重试!";
        }

        long availBal = MoneyHelper.yuanToFen(NumberHelper.toDouble(balanceQueryResponse.getAvailBal()));
        long useMoney = asset.getUseMoney().longValue();
        if (useMoney > availBal) {
            log.error(String.format("资金账户未同步:本地:%s 即信:%s", useMoney, availBal));
            msg = "资金账户未同步，请先在个人中心进行资金同步操作!";
        }
        return ImmutableMap.of(MSG, msg);
    }

    /**
     * 验证债权转让
     *
     * @param buyMoney
     * @param transfer
     * @return
     */
    private ImmutableMap<String, Object> verifyTransfer(double buyMoney, Transfer transfer) {
        String msg = "";
        if (transfer.getState() != 1) {
            msg = "您看到的债权转让消失啦!";
        }

        if (transfer.getTransferMoney() == transfer.getTransferMoneyYes()) {
            msg = "债权转出金额已购满!";
        }

        long leftMoney = transfer.getTransferMoney() - transfer.getTransferMoneyYes();/*债权转让剩余可购买金额*/
        double mayBuyMoney = MathHelper.min(leftMoney, transfer.getLowest());//获取剩余可购买金额
        if (buyMoney < mayBuyMoney) {
            msg = "购买金额小于最小购买金额";
        }
        ImmutableMap<String, Object> immutableMap = ImmutableMap.of(
                MSG, msg,
                LEFT_MONEY, leftMoney
        );

        return immutableMap;
    }


    /**
     * 新版债权转让
     *
     * @param voTransferTenderReq
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseEntity<VoBaseResp> newTransferTender(VoTransferTenderReq voTransferTenderReq) throws Exception {

        long tenderId = voTransferTenderReq.getTenderId();/* 转让债权id */
        long userId = voTransferTenderReq.getUserId();/* 转让人id */
        boolean isAll = voTransferTenderReq.getIsAll();/* 是否是部分转让 */
        String borrowCollectionIdsStr = voTransferTenderReq.getBorrowCollectionIds();/* 部分期数转让 回款id */
        List<String> borrowCollectionIds = null;

        Tender tender = tenderService.findById(tenderId);
        Preconditions.checkNotNull(tender, "立即转让: 查询用户投标记录为空!");
        Borrow borrow = borrowService.findByIdLock(tender.getBorrowId());
        Preconditions.checkNotNull(borrow, "立即转让: 查询用户投标标的信息为空!");

        // 前期债权转让检测
        ResponseEntity<VoBaseResp> transferConditionCheckResponse = transferConditionCheck(tender, borrow, userId);
        if (!transferConditionCheckResponse.getStatusCode().equals(HttpStatus.OK)) {
            return transferConditionCheckResponse;
        }

        // 计算（未还款）债权本金之和
        // 判断本金必须大于 1000元
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .eq("transferFlag", 0)
                .eq("status", 0)
                .eq("tenderId", tenderId)
                .build();
        if (!isAll) { //部分转让
            borrowCollectionIds = Arrays.asList(borrowCollectionIdsStr.split(","));

            bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("transferFlag", 0)
                    .eq("status", 0)
                    .eq("id", borrowCollectionIds.toArray())
                    .eq("tenderId", tenderId)
                    .build();
        }

        List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs, new Sort(Sort.Direction.ASC, "order"));
        Preconditions.checkState(!CollectionUtils.isEmpty(borrowCollectionList), "立即转让: 剩余还款不足3天，无法转让!");
        int waitTimeLimit = borrowCollectionList.size();  // 等待回款期数
        long leftCapital = borrowCollectionList.stream().mapToLong(borrowCollection -> borrowCollection.getPrincipal()).sum(); // 待回款本金
        if (leftCapital < (1000 * 100)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "可转本金必须大于1000元才能转让"));
        }

        //保存债权转让记录
        saveTransfer(tenderId, userId, tender, borrow, waitTimeLimit, leftCapital, borrowCollectionList, isAll, borrowCollectionIdsStr);
        return ResponseEntity.ok(VoBaseResp.ok("债权转让成功!"));
    }

    /**
     * 保存债权转让记录
     *
     * @param tenderId
     * @param userId
     * @param tender
     * @param borrow
     * @param waitTimeLimit
     * @param leftCapital
     * @param borrowCollectionList
     */
    private void saveTransfer(long tenderId, long userId, Tender tender, Borrow borrow, int waitTimeLimit, long leftCapital, List<BorrowCollection> borrowCollectionList, boolean isAll, String borrowCollectionIdsStr) {
        BorrowCollection firstBorrowCollection = borrowCollectionList.get(0);
        BorrowCollection lastBorrowCollection = borrowCollectionList.get(borrowCollectionList.size() - 1);
        //计算当期应计利息
        long interest = firstBorrowCollection.getInterest();/* 当期理论应计利息 */
        Date startAt = DateHelper.beginOfDate(ObjectUtils.isEmpty(firstBorrowCollection.getStartAt()) ? firstBorrowCollection.getStartAtYes() : firstBorrowCollection.getStartAt());//理论开始计息时间
        Date collectionAt = DateHelper.beginOfDate(firstBorrowCollection.getCollectionAt());//理论结束还款时间
        Date startAtYes = DateHelper.beginOfDate(firstBorrowCollection.getStartAtYes());//实际开始计息时间
        Date endAt = DateHelper.beginOfDate(new Date());//结束计息时间

        /* 当期应计利息 */
        double transferFeeRatio = MoneyHelper.divide(Math.max(DateHelper.diffInDays(endAt, startAtYes, false), 0),
                DateHelper.diffInDays(collectionAt, startAt, false));
        long alreadyInterest = Math.round(interest * transferFeeRatio);

        //新增债权转让记录
        Date nowDate = new Date();
        Transfer transfer = new Transfer();
        transfer.setUpdatedAt(nowDate);
        transfer.setType(0);
        transfer.setUserId(userId);
        transfer.setTransferMoney(leftCapital + alreadyInterest);
        transfer.setTransferMoneyYes(0l);
        transfer.setBorrowId(borrow.getId());
        transfer.setPrincipal(leftCapital);
        transfer.setStartOrder(firstBorrowCollection.getOrder());
        transfer.setEndOrder(lastBorrowCollection.getOrder());
        transfer.setAlreadyInterest(alreadyInterest);
        transfer.setApr(borrow.getApr());
        transfer.setCreatedAt(nowDate);
        transfer.setTimeLimit(waitTimeLimit);
        transfer.setLowest(1000 * 100L);
        transfer.setState(0);
        transfer.setTenderCount(0);
        transfer.setTenderId(tenderId);
        transfer.setIsAll(isAll);
        transfer.setBorrowCollectionIds(borrowCollectionIdsStr);
        transfer.setTitle(borrow.getName() + "转");
        transfer.setRepayAt(collectionAt);
        transferService.save(transfer);

        //更新投资记录
        tender.setTransferFlag(1);
        tender.setUpdatedAt(nowDate);
        tenderService.updateById(tender);

        try {
            //自动初审
            doFirstVerifyTransfer(transfer.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转让中
     *
     * @param voTransferReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewTransferOfWarpRes> tranferOfList(VoTransferReq voTransferReq) {
        try {
            Map<String, Object> resultMaps = transferService.transferOfList(voTransferReq);
            List<TransferOf> transferingList = (List<TransferOf>) resultMaps.get("transferOfList");
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            VoViewTransferOfWarpRes voViewTransferOfWarpRes = VoBaseResp.ok("查询成功", VoViewTransferOfWarpRes.class);
            voViewTransferOfWarpRes.setTransferOfs(transferingList);
            voViewTransferOfWarpRes.setTotalCount(totalCount);
            return ResponseEntity.ok(voViewTransferOfWarpRes);
        } catch (Throwable e) {
            log.info("TransferBizImpl tranferOfList query fail%S", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewTransferOfWarpRes.class));
        }
    }

    /**
     * 已转让
     *
     * @param voTransferReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewTransferedWarpRes> transferedlist(VoTransferReq voTransferReq) {
        try {
            Map<String, Object> resultMaps = transferService.transferedList(voTransferReq);
            List<Transfered> transfereds = (List<Transfered>) resultMaps.get("transferedList");
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            VoViewTransferedWarpRes voViewTransferOfWarpRes = VoBaseResp.ok("查询成功", VoViewTransferedWarpRes.class);
            voViewTransferOfWarpRes.setTransferedList(transfereds);
            voViewTransferOfWarpRes.setTotalCount(totalCount);
            return ResponseEntity.ok(voViewTransferOfWarpRes);
        } catch (Throwable e) {
            log.info("TransferBizImpl transferedlist query fail%S", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewTransferedWarpRes.class));
        }
    }

    /**
     * 可转让
     *
     * @param voTransferReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewTransferMayWarpRes> transferMayList(VoTransferReq voTransferReq) {
        try {
            Map<String, Object> resultMaps = transferService.transferMayList(voTransferReq);
            List<TransferMay> transferOfs = (List<TransferMay>) resultMaps.get("transferMayList");
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            VoViewTransferMayWarpRes voViewTransferOfWarpRes = VoBaseResp.ok("查询成功", VoViewTransferMayWarpRes.class);
            voViewTransferOfWarpRes.setMayList(transferOfs);
            voViewTransferOfWarpRes.setTotalCount(totalCount);
            return ResponseEntity.ok(voViewTransferOfWarpRes);
        } catch (Throwable e) {
            log.info("TransferBizImpl transferMayList query fail%S", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewTransferMayWarpRes.class));
        }
    }

    /**
     * 已购买
     *
     * @param voTransferReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewTransferBuyWarpRes> tranferBuyList(VoTransferReq voTransferReq) {
        try {
            Map<String, Object> resultMaps = transferService.transferBuyList(voTransferReq);
            List<TransferBuy> transferOfs = (List<TransferBuy>) resultMaps.get("transferBuys");
            Integer totalCount = Integer.valueOf(resultMaps.get("totalCount").toString());
            VoViewTransferBuyWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewTransferBuyWarpRes.class);
            warpRes.setTransferBuys(transferOfs);
            warpRes.setTotalCount(totalCount);
            return ResponseEntity.ok(warpRes);
        } catch (Exception e) {
            log.info("TransferBizImpl transferMayList query fail%S", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewTransferBuyWarpRes.class));
        }
    }

    /**
     * 债券购买记录列表
     *
     * @param transferUserListReq
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBorrowTenderUserWarpListRes> transferUserList(VoTransferUserListReq transferUserListReq) {
        VoBorrowTenderUserWarpListRes warpListRes = VoBaseResp.ok("查询成功", VoBorrowTenderUserWarpListRes.class);
        //购买记录
        Specification<TransferBuyLog> specification1 = Specifications.<TransferBuyLog>and()
                .in("state", Lists.newArrayList(TransferBuyLogContants.SUCCESS, TransferBuyLogContants.BUYING).toArray())
                .eq("transferId", transferUserListReq.getTransferId())
                .build();
        List<TransferBuyLog> transferBuyLogs = transferBuyLogService.findList(specification1,
                new PageRequest(transferUserListReq.getPageIndex(),
                        transferUserListReq.getPageSize(),
                        new Sort(Sort.Direction.DESC, "id")));
        if (CollectionUtils.isEmpty(transferBuyLogs)) {
            return ResponseEntity.ok(warpListRes);
        }
        //投标用户
        Set<Long> userIds = transferBuyLogs.stream().map(m -> m.getUserId()).collect(Collectors.toSet());
        List<Users> usersList = usersRepository.findByIdIn(new ArrayList<>(userIds));
        Map<Long, Users> usersMap = usersList.stream().collect(Collectors.toMap(Users::getId, Function.identity()));
        //装配结果集
        List<VoBorrowTenderUserRes> tenderUserResList = Lists.newArrayList();
        Users sendUser = usersRepository.findOne(transferUserListReq.getUserId());
        //获取当前用户类型
        String userType = "";
        if (!ObjectUtils.isEmpty(sendUser)) {
            userType = sendUser.getType();
        }

        if (StringUtils.isEmpty(userType)) {
            userType = "";
        }
        String finalUserType = userType;
        transferBuyLogs.forEach(p -> {
            VoBorrowTenderUserRes transfer = new VoBorrowTenderUserRes();
            transfer.setDate(DateHelper.dateToString(p.getCreatedAt()));
            transfer.setValidMoney(StringHelper.formatMon(p.getValidMoney() / 100D));
            transfer.setType(p.getAuto() == true ? "自动" : "手动");
            Users user = usersMap.get(p.getUserId());
            //如果当前用户是管理员或者是投资者本人 用户名可见
            transfer.setUserName(finalUserType.equals("manager")
                    ? user.getPhone()
                    : UserHelper.hideChar(user.getPhone(), UserHelper.PHONE_NUM));
            tenderUserResList.add(transfer);
        });
        warpListRes.setVoBorrowTenderUser(tenderUserResList);
        return ResponseEntity.ok(warpListRes);
    }


    /**
     * 债权装让前期检测
     * 1. 当期债权是否已经发生转让行为
     * 2. 当前待还是否为官方标的
     * 3. 保证只能同时发生一个债权转让
     *
     * @param tender
     * @param borrow
     * @return
     */
    private ResponseEntity<VoBaseResp> transferConditionCheck(Tender tender, Borrow borrow, long userId) {
        if (borrow.getIsNovice()) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "新手标暂不支持转让，敬请期待!"));
        }
        if (userId != tender.getUserId()) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "操作失败: 非债权投资人不能转让!"));
        }

        if (tender.getTransferFlag() != 0 && tender.getTransferFlag() != 3) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "操作失败: 你已经出让债权了!"));
        }

        if ((tender.getStatus() != 1)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前系统出现异常, 麻烦通知平台客服人员!"));
        }

        if ((borrow.getType() != 0 && borrow.getType() != 4)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "投资非官方借款暂不支持债权转让!"));
        }

        if ((borrow.getStatus() != 3)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前债权不符合转让规则!"));
        }


        Specification<Borrow> borrowSpecification = Specifications
                .<Borrow>and()
                .eq("userId", tender.getUserId())
                .in("status", 0, 1)
                .build();
        long tranferingNum = borrowService.count(borrowSpecification);
        if (tranferingNum > 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "您已经有一个进行中的借款标"));
        }

        //判断当期是否有提交处理中回调
        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrow.getId())
                .eq("status", 0)
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);
        Preconditions.checkState(!CollectionUtils.isEmpty(borrowRepaymentList), "还款记录不存在!");
        List<Long> borrowRepaymentIds = borrowRepaymentList.stream().map(BorrowRepayment::getId).collect(Collectors.toList());
        Specification<ThirdBatchLog> tbls = Specifications
                .<ThirdBatchLog>and()
                .in("sourceId", borrowRepaymentIds.toArray())
                .in("type", ThirdBatchLogContants.BATCH_REPAY, ThirdBatchLogContants.BATCH_REPAY_ALL)
                .in("state", 0, 1)
                .build();
        long count = thirdBatchLogService.count(tbls);
        if (count > 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "这笔投资回款处理中，暂时无法债权转让操作!"));
        }

        //判断是否已有至少一期回款已回款
        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .eq("tenderId", tender.getId())
                .eq("status", 1)
                .build();
        count = borrowCollectionService.count(bcs);
        if (count < 1) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "债权至少要一期已回款,才能转让!"));
        }

        //至少剩余2天回款
        Date flagAt = new Date();
        flagAt = DateHelper.beginOfDate(flagAt);
        flagAt = DateHelper.subDays(flagAt, 2);

        bcs = Specifications
                .<BorrowCollection>and()
                .eq("tenderId", tender.getId())
                .eq("transferFlag", 0)
                .eq("status", 0)
                .predicate(new LeSpecification<>("collectionAt", new DataObject(flagAt)))
                .build();
        count = borrowCollectionService.count(bcs);
        if (count > 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "转让债权离还款日必须大于2天,才能转让!"));
        }

        //判断用户存管信息
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(tender.getUserId());
        if (!userThirdAccount.getAutoTransferState().equals(1)) {  // 审核
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_CREDIT_TRNSFER, "请先签订自动债权转让协议！", VoBaseResp.class));
        }
        return ThirdAccountHelper.allConditionCheck(userThirdAccount);
    }

    /**
     * 获取立即转让详情
     *
     * @param tenderId 投标记录Id
     * @return
     */
    @Override
    public ResponseEntity<VoGoTenderInfo> goTenderInfo(Long tenderId, Long userId) {
        Tender tender = tenderService.findById(tenderId);
        Preconditions.checkNotNull(tender, "");
        Preconditions.checkArgument(userId.equals(tender.getUserId()), "获取立即转让详情: 非法操作!");

        Specification<BorrowCollection> bcs = Specifications
                .<BorrowCollection>and()
                .eq("tenderId", tenderId)
                .eq("status", 0)
                .build();
        List<BorrowCollection> borrowCollections = borrowCollectionService.findList(bcs, new Sort(Sort.Direction.ASC, "id"));
        Preconditions.checkState(!CollectionUtils.isEmpty(borrowCollections), "获取立即转让详情: 还款计划查询失败!");
        borrowCollections = borrowCollections.stream().filter(w -> w.getStatus() == BorrowCollectionContants.STATUS_NO).collect(Collectors.toList());
        BorrowCollection borrowCollection = borrowCollections.get(0);
        Borrow borrow = borrowService.findById(tender.getBorrowId());
        Preconditions.checkState(!CollectionUtils.isEmpty(borrowCollections), "获取立即转让详情: 获取投资的标的信息失败!");
        String repayFashionStr = "";
        switch (borrow.getRepayFashion()) {
            case BorrowContants.REPAY_FASHION_AYFQ_NUM:
                repayFashionStr = "按月分期";
                break;
            case BorrowContants.REPAY_FASHION_YCBX_NUM:
                repayFashionStr = "一次性还本付息";
                break;
            case BorrowContants.REPAY_FASHION_XXHB_NUM:
                repayFashionStr = "先息后本";
                break;
            default:
        }
        long money = borrowCollections.stream().mapToLong(borrowCollectionItem -> borrowCollectionItem.getPrincipal()).sum(); // 待汇款本金
        //转让费用
        // 0.4% + 0.08% * (剩余期限-1)  （费率最高上限为1.28%）
        double rate = 0.004 + 0.0008 * (borrowCollections.size() - 1);
        rate = Math.min(rate, 0.0128);
        Double fee = money * rate;  // 费用

        int day = DateHelper.diffInDays(borrowCollection.getCollectionAt(), new Date(), false);
        day = day < 0 ? 0 : day;
        VoGoTenderInfo voGoTenderInfo = VoGoTenderInfo.ok("查询成功!", VoGoTenderInfo.class);
        voGoTenderInfo.setTenderId(tender.getId());
        voGoTenderInfo.setApr(StringHelper.formatDouble(borrow.getApr(), 100.0, false));
        voGoTenderInfo.setBorrowName(borrow.getName());
        voGoTenderInfo.setNextRepaymentDate(DateHelper.dateToString(borrowCollection.getCollectionAt(), DateHelper.DATE_FORMAT_YMD));
        voGoTenderInfo.setSurplusDate(String.valueOf(day));
        voGoTenderInfo.setRepayFashionStr(repayFashionStr);
        voGoTenderInfo.setTimeLimit(String.valueOf(borrowCollections.size()) + "个月");
        voGoTenderInfo.setMoney(StringHelper.formatDouble(money, 100.0, true));
        voGoTenderInfo.setFee(StringHelper.formatDouble(fee, 100.0, true));
        return ResponseEntity.ok(voGoTenderInfo);
    }

    @Override
    public List<VoViewBorrowList> findTransferList(VoBorrowListReq voBorrowListReq) {
        Map<String, Object> resultMaps = commonQuery(voBorrowListReq);
        List<Transfer> transferList = (List<Transfer>) resultMaps.get("transfers");
        if (CollectionUtils.isEmpty(transferList)) {
            return Lists.newArrayList();
        }
        Set<Long> borrowIds = transferList
                .stream()
                .map(transfer -> transfer.getBorrowId()).collect(Collectors.toSet());

        Specification<Borrow> bs = Specifications
                .<Borrow>and()
                .in("id", borrowIds.toArray())
                .build();

        List<Borrow> borrowList = borrowService.findList(bs);
        if (CollectionUtils.isEmpty(borrowList)) {
            return Lists.newArrayList();
        }

        Map<Long, Borrow> borrowRef = borrowList
                .stream()
                .collect(Collectors.toMap(Borrow::getId, Function.identity()));
        Set<Long> userIds = transferList
                .stream()
                .map(transfer -> transfer.getUserId()).collect(Collectors.toSet());
        Specification<Users> us = Specifications
                .<Users>and()
                .in("id", userIds.toArray())
                .build();

        List<Users> userLists = userService.findList(us);
        Map<Long, Users> userRef = userLists
                .stream()
                .collect(Collectors.toMap(Users::getId, Function.identity()));


        List<VoViewBorrowList> voViewBorrowLists = new ArrayList<>(transferList.size());

        VoViewBorrowList voViewBorrowList = null;
        Borrow borrow = null;
        for (Transfer item : transferList) {
            voViewBorrowList = new VoViewBorrowList();
            borrow = borrowRef.get(item.getBorrowId());
            voViewBorrowList.setId(item.getId());  // ID
            voViewBorrowList.setMoney(StringHelper.formatMon(item.getTransferMoney() / 100d) + MoneyConstans.RMB);
            voViewBorrowList.setIsContinued(borrow.getIsContinued());
            voViewBorrowList.setLockStatus(borrow.getIsLock());
            voViewBorrowList.setIsImpawn(borrow.getIsImpawn());
            voViewBorrowList.setApr(StringHelper.formatMon(item.getApr() / 100d) + MoneyConstans.PERCENT);  //转换率
            voViewBorrowList.setName(item.getTitle());
            voViewBorrowList.setMoneyYes(StringHelper.formatMon(item.getPrincipal() / 100d) + MoneyConstans.RMB);
            voViewBorrowList.setIsNovice(borrow.getIsNovice());
            voViewBorrowList.setIsMortgage(borrow.getIsMortgage());
            voViewBorrowList.setIsPassWord(false);
            voViewBorrowList.setTimeLimit(item.getTimeLimit() + BorrowContants.MONTH);
            voViewBorrowList.setRecheckAt(!StringUtils.isEmpty(item.getRecheckAt())
                    ? DateHelper.dateToString(item.getRecheckAt())
                    : "");
            voViewBorrowList.setSuccessAt(!StringUtils.isEmpty(item.getSuccessAt())
                    ? DateHelper.dateToString(item.getSuccessAt())
                    : "");
            //1.待发布 2.还款中 3.招标中 4.已完成 5.已过期 6.待复审

            //待发布时间
            voViewBorrowList.setSurplusSecond(0L);
            //进度
            voViewBorrowList.setSpend(0d);

            //过期时间 第二天晚上9点
            Date endAt = DateHelper.subHours(DateHelper.endOfDate(DateHelper.addDays(item.getReleaseAt(), 1)), 3);

            //转让中
            if (item.getState() == TransferContants.TRANSFERIND) {
                if (item.getTransferMoneyYes() / item.getTransferMoney() == 1) {
                    //待复审
                    voViewBorrowList.setStatus(6);
                } else {
                    //招标中
                    if (endAt.getTime() > System.currentTimeMillis()) {
                        voViewBorrowList.setStatus(3);
                    } else {
                        voViewBorrowList.setStatus(5);
                    }
                }
            } else if (item.getState() == TransferContants.TRANSFERED) {
                //已完成
                voViewBorrowList.setStatus(4);
            }
            double spend = NumberHelper.floorDouble((item.getTransferMoneyYes().doubleValue() / item.getTransferMoney()) * 100, 2);
            voViewBorrowList.setRecheckAt(StringUtils.isEmpty(item.getRecheckAt()) ? "" : DateHelper.dateToString(item.getRecheckAt()));
            voViewBorrowList.setSpend(spend);
            Users user = userRef.get(item.getUserId());
            voViewBorrowList.setUserName(!StringUtils.isEmpty(user.getUsername()) ? user.getUsername() : user.getPhone());
            voViewBorrowList.setType(5);
            voViewBorrowList.setIsFlow(true);
            voViewBorrowList.setIsConversion(borrow.getIsConversion());
            voViewBorrowList.setReleaseAt(DateHelper.dateToString(item.getReleaseAt()));
            voViewBorrowList.setRepayFashion(BorrowContants.REPAY_FASHION_AYFQ_NUM);
            voViewBorrowList.setIsVouch(borrow.getIsVouch());
            voViewBorrowList.setTenderCount(item.getTenderCount());
            voViewBorrowList.setAvatar(user.getAvatarPath());
            voViewBorrowLists.add(voViewBorrowList);
        }
        return voViewBorrowLists;
    }


    @Override
    public ResponseEntity<VoPcBorrowList> pcFindTransferList(VoBorrowListReq voBorrowListReq) {
        VoPcBorrowList voPcBorrowList = VoBaseResp.ok("查询成功", VoPcBorrowList.class);
        Map<String, Object> resultMaps = commonQuery(voBorrowListReq);
        List<Transfer> transferList = (List<Transfer>) resultMaps.get("transfers");
        if (CollectionUtils.isEmpty(transferList)) {
            return ResponseEntity.ok(voPcBorrowList);
        }
        voPcBorrowList.setTotalCount(Long.valueOf(resultMaps.get("totalCount").toString()).intValue());
        voPcBorrowList.setBorrowLists(commonHandel(transferList));
        return ResponseEntity.ok(voPcBorrowList);
    }

    @Override
    public List<VoViewBorrowList> commonHandel(List<Transfer> transferList) {
        Set<Long> borrowIds = transferList
                .stream()
                .map(transfer -> transfer.getBorrowId()).collect(Collectors.toSet());
        Specification<Borrow> bs = Specifications
                .<Borrow>and()
                .in("id", borrowIds.toArray())
                .build();

        List<Borrow> borrowList = borrowService.findList(bs);


        Map<Long, Borrow> borrowRef = borrowList
                .stream()
                .collect(Collectors.toMap(Borrow::getId, Function.identity()));
        Set<Long> userIds = transferList
                .stream()
                .map(transfer -> transfer.getUserId()).collect(Collectors.toSet());
        Specification<Users> us = Specifications
                .<Users>and()
                .in("id", userIds.toArray())
                .build();

        List<Users> userLists = userService.findList(us);
        Map<Long, Users> userRef = userLists
                .stream()
                .collect(Collectors.toMap(Users::getId, Function.identity()));

        List<VoViewBorrowList> transfers = Lists.newArrayList();

        transferList.forEach(transfer -> {
            VoViewBorrowList item = new VoViewBorrowList();

            Borrow borrow = borrowRef.get(transfer.getBorrowId());
            Users users = userRef.get(transfer.getUserId());

            item.setApr(StringHelper.formatMon(transfer.getApr() / 100D) + MoneyConstans.PERCENT);
            item.setAvatar(users.getAvatarPath());
            item.setId(transfer.getId());
            item.setMoneyYes(StringHelper.formatMon(transfer.getTransferMoneyYes() / 100D));
            item.setMoney(StringHelper.formatMon(transfer.getTransferMoney() / 100D));
            item.setIsConversion(borrow.getIsConversion());
            item.setName(transfer.getTitle());
            item.setIsFlow(true);
            item.setIsImpawn(borrow.getIsImpawn());
            item.setIsMortgage(borrow.getIsMortgage());
            item.setIsNovice(borrow.getIsNovice());
            item.setIsContinued(borrow.getIsContinued());
            item.setIsPassWord(StringUtils.isEmpty(borrow.getPassword()) ? false : true);
            item.setLockStatus(false);
            item.setIsVouch(borrow.getIsVouch());
            item.setReleaseAt(DateHelper.dateToString(transfer.getReleaseAt()));
            item.setTimeLimit(transfer.getTimeLimit() + BorrowContants.MONTH);
            item.setTenderCount(transfer.getTenderCount());
            item.setUserName(StringUtils.isEmpty(users.getUsername())
                    ? UserHelper.hideChar(users.getPhone(), UserHelper.PHONE_NUM)
                    : UserHelper.hideChar(users.getUsername(), UserHelper.USERNAME_NUM));
            item.setAvatar(StringUtils.isEmpty(users.getAvatarPath())
                    ? imageDomain + "/images/user/default_avatar.jpg"
                    : users.getAvatarPath());
            item.setType(5);
            item.setSuccessAt(StringUtils.isEmpty(transfer.getSuccessAt())
                    ? ""
                    : DateHelper.dateToString(transfer.getSuccessAt()));
            item.setRecheckAt(StringUtils.isEmpty(transfer.getRecheckAt())
                    ? ""
                    : DateHelper.dateToString(transfer.getRecheckAt()));
            double spend = NumberHelper.floorDouble((transfer.getTransferMoneyYes().doubleValue() / transfer.getTransferMoney()) * 100, 2);
            item.setSpend(spend);

            Date endAt = DateHelper.subHours(DateHelper.endOfDate(DateHelper.addDays(transfer.getReleaseAt(), 1)), 3);

            //1.待发布 2.还款中 3.招标中 4.已完成 5.已过期 6.待复审
            //进度
            Integer status = transfer.getState();
            //转让中
            if (status == TransferContants.TRANSFERIND) {
                if (transfer.getTransferMoneyYes() / transfer.getTransferMoney() == 1) {
                    //待审核
                    item.setStatus(6);
                } else {
                    //招标中
                    if (endAt.getTime() > new Date().getTime()) {
                        item.setStatus(3);
                    } else {
                        item.setStatus(5);
                    }
                }
            } else if (status == TransferContants.TRANSFERED) {
                //已完成
                item.setStatus(4);
            }
            item.setRepayFashion(0);
            item.setSurplusSecond(0L);
            transfers.add(item);
        });
        return transfers;
    }

    private Map<String, Object> commonQuery(VoBorrowListReq voBorrowListReq) {
        Map<String, Object> resultMaps = Maps.newHashMap();
/*        Specification<Transfer> ts = Specifications
                .<Transfer>and()
                .eq("state", TransferContants.TRANSFERIND)
                .eq("type",0)
                .predicate(Specifications.or().eq("state", TransferContants.TRANSFERED).le("apr", 1500).build())
                .build();*/
        Pageable pageable = new PageRequest(voBorrowListReq.getPageIndex(),
                voBorrowListReq.getPageSize(),
                new Sort(Sort.Direction.DESC,
                        "id"));
        Page<Transfer> transferPage = transferRepository.findByStateIsOrStateIsAndAprThanLee(pageable);
        resultMaps.put("totalCount", transferPage.getTotalElements());
        resultMaps.put("transfers", transferPage.getContent());
        return resultMaps;
    }

    @Override
    public ResponseEntity<BorrowInfoRes> transferInfo(Long transferId) {
        Transfer transfer = transferService.findById(transferId);
        if (ObjectUtils.isEmpty(transfer)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "债权转让信息为空", BorrowInfoRes.class));
        }

        Long borrowId = transfer.getBorrowId();
        Borrow borrow = borrowService.findById(borrowId);
        if (ObjectUtils.isEmpty(borrow)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "债权转让 原始标的信息为空", BorrowInfoRes.class));
        }

        BorrowInfoRes borrowInfoRes = VoBaseResp.ok("查询成功", BorrowInfoRes.class);
        borrowInfoRes.setApr(StringHelper.formatMon(borrow.getApr() / 100d));
        borrowInfoRes.setLowest(StringHelper.formatMon(transfer.getLowest() / 100d));
        long surplusMoney = transfer.getTransferMoney() - transfer.getTransferMoneyYes();
        borrowInfoRes.setViewSurplusMoney(StringHelper.formatMon(surplusMoney / 100D));
        borrowInfoRes.setHideSurplusMoney(surplusMoney);

        if (borrow.getType().equals(com.gofobao.framework.borrow.contants.BorrowContants.REPAY_FASHION_ONCE)) {
            borrowInfoRes.setTimeLimit(transfer.getTimeLimit() + com.gofobao.framework.borrow.contants.BorrowContants.DAY);
        } else {
            borrowInfoRes.setTimeLimit(transfer.getTimeLimit() + com.gofobao.framework.borrow.contants.BorrowContants.MONTH);
        }

        double principal = 10000D * 100;
        double apr = NumberHelper.toDouble(StringHelper.toString(transfer.getApr()));
        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(
                principal,
                apr,
                transfer.getTimeLimit(),
                transfer.getRecheckAt());
        Map<String, Object> calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
        Integer earnings = NumberHelper.toInt(calculatorMap.get("earnings"));
        borrowInfoRes.setEarnings(StringHelper.formatMon(earnings / 100d) + MoneyConstans.RMB);
        if (transfer.getTenderCount() == 0) {
            Specification<TransferBuyLog> transferBuyLogSpecification = Specifications
                    .<TransferBuyLog>and()
                    .eq("transferId", transfer.getId())
                    .eq("state", 1).build();
            long count = transferBuyLogService.count(transferBuyLogSpecification);
            borrowInfoRes.setTenderCount(count + com.gofobao.framework.borrow.contants.BorrowContants.TIME);
        } else {
            borrowInfoRes.setTenderCount(transfer.getTenderCount() + com.gofobao.framework.borrow.contants.BorrowContants.TIME);
        }

        borrowInfoRes.setSpend(StringHelper.formatMon(NumberHelper.floorDouble(transfer.getTransferMoneyYes() / transfer.getTransferMoney().doubleValue(), 2) * 100));
        borrowInfoRes.setMoney(StringHelper.formatMon(transfer.getTransferMoney() / 100d));
        borrowInfoRes.setRepayFashion(borrow.getRepayFashion());

        //结束时间
        Date endAt = DateHelper.subHours(DateHelper.endOfDate(DateHelper.addDays(transfer.getReleaseAt(), 1)), 3);
        borrowInfoRes.setEndAt(DateHelper.dateToString(endAt, DateHelper.DATE_FORMAT_YMDHMS));
        //进度
        borrowInfoRes.setSurplusSecond(-1L);
        //0.待审核 1.转让中 2.已转让 3.审核未通过 4.已取消',
        Integer status = transfer.getState();
        if (status == 1) {//招标中
            if ((transfer.getTransferMoneyYes() / transfer.getTransferMoney() == 1) && !ObjectUtils.isEmpty(transfer.getSuccessAt())) {
                //复审中
                borrowInfoRes.setStatus(6);
                borrowInfoRes.setPeriodHour(transfer.getSuccessAt().getTime() - transfer.getReleaseAt().getTime());
                //已过期
            } else if (endAt.getTime() < System.currentTimeMillis()) {
                borrowInfoRes.setStatus(5);
            } else {
                //招标中
                borrowInfoRes.setStatus(3);
                borrowInfoRes.setPeriodSurplusHour(endAt.getTime() - System.currentTimeMillis());
            }
        } else {
            borrowInfoRes.setStatus(4);
            borrowInfoRes.setPeriodHour(transfer.getSuccessAt().getTime() - transfer.getReleaseAt().getTime());
        }
        borrowInfoRes.setReleaseAt(DateHelper.dateToString(transfer.getReleaseAt()));
        borrowInfoRes.setBorrowId(borrowId);
        borrowInfoRes.setTenderId(transfer.getTenderId());
        borrowInfoRes.setTransferId(transfer.getId());
        borrowInfoRes.setType(5);
        borrowInfoRes.setPassWord(false);
        Users users = userService.findById(borrow.getUserId());
        borrowInfoRes.setUserName(!StringUtils.isEmpty(users.getUsername())
                ? users.getUsername() :
                users.getPhone());
        borrowInfoRes.setIsNovice(borrow.getIsNovice());
        borrowInfoRes.setSuccessAt(StringUtils.isEmpty(transfer.getSuccessAt())
                ? ""
                : DateHelper.dateToString(borrow.getSuccessAt()));
        borrowInfoRes.setRecheckAt(!StringUtils.isEmpty(transfer.getRecheckAt())
                ? DateHelper.dateToString(transfer.getRecheckAt())
                : ""
        );
        borrowInfoRes.setBorrowName(borrow.getName());
        borrowInfoRes.setIsConversion(borrow.getIsConversion());
        borrowInfoRes.setIsNovice(borrow.getIsNovice());
        borrowInfoRes.setIsContinued(borrow.getIsContinued());
        borrowInfoRes.setIsImpawn(borrow.getIsImpawn());
        borrowInfoRes.setIsMortgage(borrow.getIsMortgage());
        borrowInfoRes.setIsVouch(borrow.getIsVouch());
        borrowInfoRes.setHideLowMoney(borrow.getLowest());
        borrowInfoRes.setIsFlow(true);
        borrowInfoRes.setAvatar(StringUtils.isEmpty(users.getAvatarPath())
                ? imageDomain + "/images/user/default_avatar.jpg"
                : users.getAvatarPath());
        borrowInfoRes.setLockStatus(transfer.getIsLock());
        return ResponseEntity.ok(borrowInfoRes);
    }

    @Override
    public void cancelTransferByTenderId(Long id) throws Exception {
        // 获取转让信息
        Specification<Transfer> ts = Specifications
                .<Transfer>and()
                .eq("tenderId", id)
                .in("state", 0, 1)
                .build();
        List<Transfer> list = transferService.findList(ts);
        Preconditions.checkState(!CollectionUtils.isEmpty(list), "取消债权转让: 查询转让记录为空");
        Transfer transfer = list.get(0);

        // 获取投标记录
        Specification<TransferBuyLog> tbs = Specifications
                .<TransferBuyLog>and()
                .eq("transferId", transfer.getId())
                .eq("state", 0)
                .build();

        List<TransferBuyLog> transferBuyLogs = transferBuyLogService.findList(tbs);


        Date nowDate = new Date();
        // 取消借款
        if (!CollectionUtils.isEmpty(transferBuyLogs)) {
            for (TransferBuyLog item : transferBuyLogs) {
                item.setState(3);
                item.setUpdatedAt(nowDate);
                AssetChange assetChange = new AssetChange();
                assetChange.setSourceId(item.getId());
                assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
                assetChange.setMoney(item.getValidMoney());
                assetChange.setSeqNo(assetChangeProvider.getSeqNo());
                assetChange.setRemark(String.format("你购买债权转让[%s]已被取消, 成功解冻资金%s元",
                        transfer.getTitle(),
                        StringHelper.formatDouble(item.getValidMoney() / 100D,
                                true)));
                assetChange.setType(AssetChangeTypeEnum.unfreeze);
                assetChange.setUserId(item.getUserId());
                assetChangeProvider.commonAssetChange(assetChange);
            }

            // 发送站内信通知
            Set<Long> userIds = transferBuyLogs
                    .stream()
                    .map(item -> item.getUserId()).collect(Collectors.toSet());
            Notices notices;
            for (Long userId : userIds) {
                notices = new Notices();
                notices.setFromUserId(1L);
                notices.setUserId(userId);
                notices.setRead(false);
                notices.setName("债权转让取消通知");
                notices.setContent(String.format("你购买的债权[%s]已被系统取消", transfer.getTitle()));
                notices.setType("system");
                notices.setCreatedAt(nowDate);
                notices.setUpdatedAt(nowDate);
                MqConfig mqConfig = new MqConfig();
                mqConfig.setQueue(MqQueueEnum.RABBITMQ_NOTICE);
                mqConfig.setTag(MqTagEnum.NOTICE_PUBLISH);
                Map<String, String> body = GSON.fromJson(GSON.toJson(notices), TypeTokenContants.MAP_TOKEN);
                mqConfig.setMsg(body);
                try {
                    log.info(String.format("borrowBizImpl cancelBorrow send mq %s", GSON.toJson(body)));
                    mqHelper.convertAndSend(mqConfig);
                } catch (Throwable e) {
                    log.error("borrowBizImpl cancelBorrow send mq exception", e);
                }
            }
        }

        // 取消债权转让
        transfer.setState(4);
        transfer.setUpdatedAt(nowDate);
        transferService.save(transfer);

        // 更新投标记录状态
        Tender tender = tenderService.findById(id);
        tender.setTransferFlag(0);
        tender.setUpdatedAt(nowDate);
        tenderService.save(tender);
    }

    /**
     * 投标合同
     *
     * @param tenderId
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> contract(Long tenderId, Long userId) {
        try {
            return transferService.transferContract(tenderId, userId);
        } catch (Exception e) {
            return null;
        }
    }


}
