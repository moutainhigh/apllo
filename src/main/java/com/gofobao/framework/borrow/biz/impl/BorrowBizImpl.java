package com.gofobao.framework.borrow.biz.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.api.contants.ChannelContant;
import com.gofobao.framework.api.contants.DesLineFlagContant;
import com.gofobao.framework.api.contants.JixinResultContants;
import com.gofobao.framework.api.helper.JixinManager;
import com.gofobao.framework.api.helper.JixinTxCodeEnum;
import com.gofobao.framework.api.model.trustee_pay_query.TrusteePayQueryReq;
import com.gofobao.framework.api.model.trustee_pay_query.TrusteePayQueryResp;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayRequest;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayResponse;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.service.AssetService;
import com.gofobao.framework.borrow.biz.BorrowBiz;
import com.gofobao.framework.borrow.biz.BorrowThirdBiz;
import com.gofobao.framework.borrow.contants.BorrowContants;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.borrow.vo.request.*;
import com.gofobao.framework.borrow.vo.response.*;
import com.gofobao.framework.collection.entity.BorrowCollection;
import com.gofobao.framework.collection.service.BorrowCollectionService;
import com.gofobao.framework.common.capital.CapitalChangeEntity;
import com.gofobao.framework.common.capital.CapitalChangeEnum;
import com.gofobao.framework.common.constans.JixinContants;
import com.gofobao.framework.common.constans.MoneyConstans;
import com.gofobao.framework.common.constans.TypeTokenContants;
import com.gofobao.framework.common.rabbitmq.MqConfig;
import com.gofobao.framework.common.rabbitmq.MqHelper;
import com.gofobao.framework.common.rabbitmq.MqQueueEnum;
import com.gofobao.framework.common.rabbitmq.MqTagEnum;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.*;
import com.gofobao.framework.helper.project.*;
import com.gofobao.framework.lend.entity.Lend;
import com.gofobao.framework.lend.service.LendService;
import com.gofobao.framework.listener.providers.BorrowProvider;
import com.gofobao.framework.member.entity.UserCache;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.service.UserCacheService;
import com.gofobao.framework.member.service.UserService;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.member.vo.response.VoHtmlResp;
import com.gofobao.framework.repayment.biz.RepaymentBiz;
import com.gofobao.framework.repayment.entity.BorrowRepayment;
import com.gofobao.framework.repayment.service.BorrowRepaymentService;
import com.gofobao.framework.repayment.vo.request.VoRepayReq;
import com.gofobao.framework.system.biz.IncrStatisticBiz;
import com.gofobao.framework.system.biz.StatisticBiz;
import com.gofobao.framework.system.entity.*;
import com.gofobao.framework.system.service.DictItemServcie;
import com.gofobao.framework.system.service.DictValueService;
import com.gofobao.framework.tender.biz.TenderBiz;
import com.gofobao.framework.tender.biz.TenderThirdBiz;
import com.gofobao.framework.tender.entity.AutoTender;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.service.AutoTenderService;
import com.gofobao.framework.tender.service.TenderService;
import com.gofobao.framework.tender.vo.request.VoCancelThirdTenderReq;
import com.gofobao.framework.tender.vo.request.VoCreateTenderReq;
import com.gofobao.framework.tender.vo.response.VoAutoTenderInfo;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Zeke on 2017/5/26.
 */
@Service
@Slf4j
public class BorrowBizImpl implements BorrowBiz {

    static final Gson GSON = new Gson();

    @Autowired
    private UserCacheService userCacheService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private AutoTenderService autoTenderService;
    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private MqHelper mqHelper;
    @Autowired
    private TenderService tenderService;
    @Autowired
    private CapitalChangeHelper capitalChangeHelper;
    @Autowired
    private BorrowCollectionService borrowCollectionService;
    @Autowired
    private BorrowRepaymentService borrowRepaymentService;
    @Autowired
    private RepaymentBiz repaymentBiz;
    @Autowired
    private BorrowProvider borrowProvider;
    @Autowired
    private BorrowThirdBiz borrowThirdBiz;
    @Autowired
    private IncrStatisticBiz incrStatisticBiz;
    @Autowired
    private UserService userService;
    @Autowired
    private StatisticBiz statisticBiz;
    @Autowired
    private ThymeleafHelper thymeleafHelper;
    @Autowired
    private TenderThirdBiz tenderThirdBiz;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LendService lendService;
    @Autowired
    private TenderBiz tenderBiz;

    @Autowired
    JixinManager jixinManager;


    @Autowired
    private DictItemServcie dictItemServcie;

    @Autowired
    private DictValueService dictValueService;

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

    @Value("${gofobao.imageDomain}")
    private String imageDomain;


    /**
     * 理财首页标列表
     *
     * @param voBorrowListReq
     * @return
     */
    @Override
    public ResponseEntity<VoViewBorrowListWarpRes> findAll(VoBorrowListReq voBorrowListReq) {
        try {
            List<VoViewBorrowList> borrowLists = borrowService.findAll(voBorrowListReq);
            VoViewBorrowListWarpRes listWarpRes = VoBaseResp.ok("查询成功", VoViewBorrowListWarpRes.class);
            listWarpRes.setVoViewBorrowLists(borrowLists);
            return ResponseEntity.ok(listWarpRes);
        } catch (Throwable e) {
            log.info("BorrowBizImpl findAll fail%s", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewBorrowListWarpRes.class));
        }
    }

    @Override
    public ResponseEntity<VoPcBorrowList> pcFindAll(VoBorrowListReq voBorrowListReq) {

        try {
            VoPcBorrowList borrowLists = borrowService.pcFindAll(voBorrowListReq);
            VoPcBorrowList listWarpRes = VoBaseResp.ok("查询成功", VoPcBorrowList.class);
            listWarpRes.setBorrowLists(borrowLists.getBorrowLists());
            listWarpRes.setTotalCount(borrowLists.getTotalCount());
            listWarpRes.setPageIndex(borrowLists.getPageIndex());
            listWarpRes.setPageSize(borrowLists.getPageSize());
            return ResponseEntity.ok(listWarpRes);
        } catch (Throwable e) {
            log.info("BorrowBizImpl findAll fail%s", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoPcBorrowList.class));
        }
    }


    /**
     * 标信息
     *
     * @param borrowId
     * @return
     */
    @Override
    public ResponseEntity<BorrowInfoRes> info(Long borrowId) {
        Borrow borrow = borrowService.findByBorrowId(borrowId);
        if (ObjectUtils.isEmpty(borrow)) {
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "非法查询",
                            BorrowInfoRes.class));
        }

        BorrowInfoRes borrowInfoRes = VoBaseResp.ok("", BorrowInfoRes.class);
        try {
            borrowInfoRes.setApr(StringHelper.formatMon(borrow.getApr() / 100d));
            borrowInfoRes.setLowest(StringHelper.formatMon(borrow.getLowest() / 100d));
            Integer surplusMoney = borrow.getMoney() - borrow.getMoneyYes();
            borrowInfoRes.setViewSurplusMoney(StringHelper.formatMon(surplusMoney / 100D));
            borrowInfoRes.setHideSurplusMoney(surplusMoney);
            if (borrow.getType() == BorrowContants.REPAY_FASHION_ONCE) {
                borrowInfoRes.setTimeLimit(borrow.getTimeLimit() + BorrowContants.DAY);
            } else {
                borrowInfoRes.setTimeLimit(borrow.getTimeLimit() + BorrowContants.MONTH);
            }
            double principal = 10000D * 100;
            double apr = NumberHelper.toDouble(StringHelper.toString(borrow.getApr()));
            BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(principal, apr, borrow.getTimeLimit(), borrow.getSuccessAt());
            Map<String, Object> calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
            Integer earnings = NumberHelper.toInt(calculatorMap.get("earnings"));
            borrowInfoRes.setEarnings(StringHelper.formatMon(earnings / 100d) + MoneyConstans.RMB);
            borrowInfoRes.setTenderCount(borrow.getTenderCount() + BorrowContants.TIME);
            borrowInfoRes.setMoney(StringHelper.formatMon(borrow.getMoney() / 100d));
            borrowInfoRes.setRepayFashion(borrow.getRepayFashion());
            borrowInfoRes.setSpend(Double.parseDouble(StringHelper.formatMon(borrow.getMoneyYes() / borrow.getMoney().doubleValue())));
            //结束时间
            Date endAt = DateHelper.addDays(DateHelper.beginOfDate(borrow.getReleaseAt()), borrow.getValidDay() + 1);
            borrowInfoRes.setEndAt(DateHelper.dateToString(endAt, DateHelper.DATE_FORMAT_YMDHMS));
            //进度
            borrowInfoRes.setSurplusSecond(-1L);
            //1.待发布 2.还款中 3.招标中 4.已完成 5.其它
            Integer status = borrow.getStatus();
            Date nowDate = new Date();
            Date releaseAt = borrow.getReleaseAt();  //发布时间

            if (status == BorrowContants.BIDDING) {//招标中
                //待发布
                if (releaseAt.getTime() >= nowDate.getTime()) {
                    status = 1;
                    borrowInfoRes.setSurplusSecond((releaseAt.getTime() - nowDate.getTime()) / 1000 + 5);
                } else if (nowDate.getTime() > endAt.getTime()) {  //当前时间大于招标有效时间
                    status = 5; //已过期
                } else {
                    status = 3; //招标中
                    //  进度
                    borrowInfoRes.setSpend(Double.parseDouble(StringHelper.formatMon(borrow.getMoneyYes().doubleValue() / borrow.getMoney())));
                }
            } else if (!ObjectUtils.isEmpty(borrow.getSuccessAt()) && !ObjectUtils.isEmpty(borrow.getCloseAt())) {   //满标时间 结清
                status = 4; //已完成
            } else if (status == BorrowContants.PASS && ObjectUtils.isEmpty(borrow.getCloseAt())) {
                status = 2; //还款中
            }
            borrowInfoRes.setType(borrow.getType());
            if (!StringUtils.isEmpty(borrow.getTenderId())) {
                borrowInfoRes.setType(5);
            }

            borrowInfoRes.setPassWord(StringUtils.isEmpty(borrow.getPassword()) ? false : true);
            Users users = userService.findById(borrow.getUserId());
            borrowInfoRes.setUserName(!StringUtils.isEmpty(users.getUsername()) ? users.getUsername() : users.getPhone());
            borrowInfoRes.setIsNovice(borrow.getIsNovice());
            borrowInfoRes.setStatus(status);
            borrowInfoRes.setSuccessAt(StringUtils.isEmpty(borrow.getSuccessAt()) ? "" : DateHelper.dateToString(borrow.getSuccessAt()));
            borrowInfoRes.setBorrowName(borrow.getName());
            borrowInfoRes.setIsConversion(borrow.getIsConversion());
            borrowInfoRes.setIsNovice(borrow.getIsNovice());
            borrowInfoRes.setIsContinued(borrow.getIsContinued());
            borrowInfoRes.setIsImpawn(borrow.getIsImpawn());
            borrowInfoRes.setIsMortgage(borrow.getIsMortgage());
            borrowInfoRes.setIsVouch(borrow.getIsVouch());
            borrowInfoRes.setIsFlow(StringUtils.isEmpty(borrow.getTenderId()) ? false : true);
            borrowInfoRes.setAvatar(imageDomain + "/data/images/avatar/" + borrow.getUserId() + "_avatar_small.jpg");
            borrowInfoRes.setReleaseAt(status != 1 ? DateHelper.dateToString(borrow.getReleaseAt()) : "");
            borrowInfoRes.setLockStatus(borrow.getIsLock());
            return ResponseEntity.ok(borrowInfoRes);
        } catch (Throwable e) {
            log.info("BorrowBizImpl detail fail%s", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            BorrowInfoRes.class));
        }

    }

    /**
     * 标简介
     *
     * @param borrowId
     * @return
     */
    @Override
    public ResponseEntity<VoViewVoBorrowDescWarpRes> desc(Long borrowId) {
        try {
            VoViewVoBorrowDescWarpRes borrowDescWarpRes = VoBaseResp.ok("查询成功", VoViewVoBorrowDescWarpRes.class);
            VoBorrowDescRes voBorrowDescRes = borrowService.desc(borrowId);
            borrowDescWarpRes.setVoBorrowDescRes(voBorrowDescRes);
            return ResponseEntity.ok(borrowDescWarpRes);
        } catch (Throwable e) {
            log.info("BorrowBizImpl desc fail%s", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewVoBorrowDescWarpRes.class));
        }
    }

    /**
     * pc:招标中统计
     *
     * @param
     * @return
     */
    @Override
    public ResponseEntity<VoViewBorrowStatisticsWarpRes> statistics() {
        try {
            VoViewBorrowStatisticsWarpRes warpRes = VoBaseResp.ok("查询成功", VoViewBorrowStatisticsWarpRes.class);
            List<BorrowStatistics> voBorrowDescRes = borrowService.statistics();
            warpRes.setStatisticsList(voBorrowDescRes);
            return ResponseEntity.ok(warpRes);
        } catch (Throwable e) {
            log.info("BorrowBizImpl desc fail%s", e);
            return ResponseEntity.badRequest()
                    .body(VoBaseResp.error(
                            VoBaseResp.ERROR,
                            "查询失败",
                            VoViewBorrowStatisticsWarpRes.class));
        }
    }

    /**
     * 标合同
     *
     * @param borrowId
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> contract(Long borrowId, Long userId) {
        try {
            return borrowService.contract(borrowId, userId);
        } catch (Throwable e) {
            log.info("BorrowBizImpl contract error", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> pcContract(Long borrowId, Long userId) {
        try {
            return borrowService.pcContract(borrowId, userId);
        } catch (Throwable e) {
            log.info("BorrowBizImpl pcContract error", e);
            return null;
        }
    }

    /**
     * 新增借款
     *
     * @param voAddNetWorthBorrow
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> addNetWorth(VoAddNetWorthBorrow voAddNetWorthBorrow) throws Exception {
        Long userId = voAddNetWorthBorrow.getUserId();
        String releaseAtStr = voAddNetWorthBorrow.getReleaseAt();
        Integer money = (int) voAddNetWorthBorrow.getMoney();
        boolean closeAuto = voAddNetWorthBorrow.isCloseAuto();

        Asset asset = assetService.findByUserIdLock(userId);
        if (ObjectUtils.isEmpty(asset)) {
            log.info("新增借款：用户asset未被查询得到。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "系统开小差了，请稍候重试！"));
        }

        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        if (ObjectUtils.isEmpty(userThirdAccount)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR_OPEN_ACCOUNT, "你还没有开通江西银行存管，请前往开通！", VoAutoTenderInfo.class));
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


        Date releaseAt = DateHelper.stringToDate(releaseAtStr, DateHelper.DATE_FORMAT_YMDHMS);
        if (releaseAt.getTime() > DateHelper.addDays(new Date(), 1).getTime()) {
            log.info("新增借款：发布时间必须在24小时内。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "发布时间必须在24小时内!"));
        }

        UserCache userCache = userCacheService.findById(userId);
        if (ObjectUtils.isEmpty(userCache)) {
            log.info("新增借款：用户usercache未被查询得到。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "系统开小差了，请稍候重试！"));
        }

        double totalMoney = (asset.getUseMoney() + userCache.getWaitCollectionPrincipal()) * 0.8 - asset.getPayment();
        if (totalMoney < money) {
            log.info("新增借款：借款金额大于净值额度。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款金额大于净值额度!"));
        }

        long count = borrowService.countByUserIdAndStatusIn(userId, Arrays.asList(0, 1));
        if (count > 0) {
            log.info("新增借款：您已经有一个进行中的借款标。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "您已经有一个进行中的借款标!"));
        }

        if (closeAuto) { //关闭用户自动投标
            AutoTender saveAutoTender = new AutoTender();
            saveAutoTender.setStatus(false);
            saveAutoTender.setUpdatedAt(new Date());

            AutoTender condAutoTender = new AutoTender();
            condAutoTender.setUserId(userId);
            Example<AutoTender> autoTenderExample = Example.of(condAutoTender);

            if (!autoTenderService.updateByExample(saveAutoTender, autoTenderExample)) {
                log.info("新增借款：自动投标关闭失败。");
                return ResponseEntity
                        .badRequest()
                        .body(VoBaseResp.error(VoBaseResp.ERROR, "自动投标关闭失败!"));
            }
        }

        Long borrowId = null;
        try {
            borrowId = insertBorrow(voAddNetWorthBorrow, userId);  // 插入标
        } catch (Throwable e) {

            log.error("新增借款异常：", e);
            throw new Exception(e);
        }

        if (borrowId <= 0) {
            log.info("新增借款：净值标插入失败。");
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "净值标插入失败!"));
        }

        //初审
        MqConfig mqConfig = new MqConfig();
        mqConfig.setQueue(MqQueueEnum.RABBITMQ_BORROW);
        mqConfig.setTag(MqTagEnum.FIRST_VERIFY);
        ImmutableMap<String, String> body = ImmutableMap
                .of(MqConfig.MSG_BORROW_ID, StringHelper.toString(borrowId), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
        mqConfig.setMsg(body);
        boolean mqState = false;
        try {
            log.info(String.format("borrowBizImpl firstVerify send mq %s", GSON.toJson(body)));
            mqState = mqHelper.convertAndSend(mqConfig);
        } catch (Throwable e) {
            log.error("borrowBizImpl firstVerify send mq exception", e);
        }

        if (!mqState) {
            return ResponseEntity.ok(VoBaseResp.ok("发布净值借款失败!"));
        }

        return ResponseEntity.ok(VoBaseResp.ok("发布净值借款成功!"));
    }

    private long insertBorrow(VoAddNetWorthBorrow voAddNetWorthBorrow, Long userId) throws Exception {
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        Preconditions.checkNotNull(userThirdAccount, "借款人未开户!");

        Borrow borrow = new Borrow();
        borrow.setType(BorrowContants.JING_ZHI); // 净值标
        borrow.setUserId(userId);
        borrow.setTUserId(userThirdAccount.getId());
        borrow.setUse(0);
        borrow.setStatus(0);
        borrow.setIsLock(false);
        borrow.setIsImpawn(false);
        borrow.setIsContinued(false);
        borrow.setIsConversion(false);
        borrow.setIsNovice(false);
        borrow.setIsVouch(false);
        borrow.setIsMortgage(false);
        borrow.setName(voAddNetWorthBorrow.getName());
        borrow.setMoney((int) voAddNetWorthBorrow.getMoney());
        borrow.setRepayFashion(1);
        borrow.setTimeLimit(voAddNetWorthBorrow.getTimeLimit());
        borrow.setApr(voAddNetWorthBorrow.getApr());
        borrow.setLowest(50 * 100);
        borrow.setMost(0);
        borrow.setMostAuto(0);
        borrow.setValidDay(voAddNetWorthBorrow.getValidDay());
        borrow.setAward(0);
        borrow.setAwardType(0);
        String releaseAt = voAddNetWorthBorrow.getReleaseAt();
        if (!ObjectUtils.isEmpty(releaseAt)) {
            borrow.setReleaseAt(DateHelper.stringToDate(releaseAt, "yyyy-MM-dd HH:mm:ss"));
        }
        borrow.setDescription("");
        borrow.setPassword("");
        borrow.setMoneyYes(0);
        borrow.setTenderCount(0);
        borrow.setCreatedAt(new Date());
        borrow.setUpdatedAt(new Date());
        boolean rs = borrowService.insert(borrow);
        if (rs) {
            return borrow.getId();
        } else {
            return 0;
        }
    }

    /**
     * 取消借款
     *
     * @param voCancelBorrow
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> cancelBorrow(VoCancelBorrow voCancelBorrow) throws Exception {
        Long borrowId = voCancelBorrow.getBorrowId();
        Long userId = voCancelBorrow.getUserId();
        Date nowDate = new Date();

        Borrow borrow = borrowService.findByIdLock(borrowId);
        Preconditions.checkNotNull(borrow, "借记录不存在!");

        if (ObjectUtils.isEmpty(borrow) || ObjectUtils.isEmpty(userId)
                || (borrow.getStatus() != 0 && borrow.getStatus() != 1)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款状态已发生更改!"));
        }

        boolean bool = false;//债权转让默认不过期
        if (!ObjectUtils.isEmpty(borrow.getReleaseAt())) {
            Date limitDate = DateHelper.addDays(DateHelper.beginOfDate(borrow.getReleaseAt()), borrow.getValidDay() + 1);
            bool = limitDate.getTime() < nowDate.getTime();
        }

        if (((borrow.getStatus() == 1) && (bool))
                || (StringHelper.toString(borrow.getUserId()).equals(StringHelper.toString(voCancelBorrow.getUserId())))) {//只有借款标过期或者本人才能取消借款
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "只有借款标过期或者本人才能取消借款!"));
        }

        Specification<Tender> borrowSpecification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrowId)
                .build();

        List<Tender> tenderList = tenderService.findList(borrowSpecification);
        Set<Long> userIdSet = tenderList.stream().map(p -> p.getUserId()).collect(Collectors.toSet());   // 投标的UserID

        // ======================================
        //  更改投资记录标识, 并且解冻投资资金
        // ======================================
        VoCancelThirdTenderReq voCancelThirdTenderReq = null;
        for (Tender tender : tenderList) {
            tender.setId(tender.getId());
            tender.setStatus(2); // 取消状态
            tender.setUpdatedAt(nowDate);
            tenderService.save(tender);

            //==================================================================
            //取消即信投资人投标记录
            //==================================================================
            if (!ObjectUtils.isEmpty(tender.getThirdTenderOrderId())) {
                voCancelThirdTenderReq = new VoCancelThirdTenderReq();
                voCancelThirdTenderReq.setTenderId(tender.getId());
                ResponseEntity<VoBaseResp> resp = tenderThirdBiz.cancelThirdTender(voCancelThirdTenderReq);
                if (!resp.getStatusCode().equals(HttpStatus.OK)) {
                    throw new Exception("borrowBizImpl cancelBorrow:" + resp.getBody().getState().getMsg());
                }
            }

            CapitalChangeEntity entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Unfrozen);
            entity.setUserId(tender.getUserId());
            entity.setMoney(tender.getValidMoney());
            entity.setRemark("借款 [" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "] 招标失败解除冻结资金。");
            capitalChangeHelper.capitalChange(entity);
        }

        //==================================================================
        //即信取消标的
        //==================================================================
        cancelThirdBorrow(borrow);

        // ======================================
        //  发送站内信
        // ======================================
        Notices notices;
        String content = String.format("你所投资的借款[ %s ]在 %s 已取消", BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()), DateHelper.nextDate(nowDate));
        for (Long toUserId : userIdSet) {
            notices = new Notices();
            notices.setFromUserId(1L);
            notices.setUserId(toUserId);
            notices.setRead(false);
            notices.setName("投资的借款失败");
            notices.setContent(content);
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

        // 债权转让标识取消
        assertAndDoTransfer(borrow);

        //更新借款
        borrow.setStatus(5);
        borrow.setUpdatedAt(nowDate);
        borrowService.updateById(borrow);
        return ResponseEntity.ok(VoBaseResp.ok("取消借款成功!"));
    }

    /**
     * 自动甄别该标是否属于债权转让. 是:自动取消债权转让标识
     *
     * @param borrow
     * @throws Exception
     */
    public void assertAndDoTransfer(Borrow borrow) throws Exception {
        Long tenderId = borrow.getTenderId();
        if ((borrow.getType() == 0) && (!ObjectUtils.isEmpty(tenderId)) && (tenderId > 0)) {
            Tender tender = tenderService.findById(tenderId);
            tender.setTransferFlag(0);
            tender.setUpdatedAt(new Date());
            tenderService.updateById(tender);
        }
    }


    /**
     * pc取消借款
     *
     * @param voPcCancelThirdBorrow
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcCancelBorrow(VoPcCancelThirdBorrow voPcCancelThirdBorrow) throws Exception {
        Date nowDate = new Date();
        String paramStr = voPcCancelThirdBorrow.getParamStr();
        if (!SecurityHelper.checkSign(voPcCancelThirdBorrow.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc取消借款 签名验证不通过!"));
        }

        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long borrowId = NumberHelper.toLong(paramMap.get("borrowId"));

        Borrow borrow = borrowService.findByIdLock(borrowId);
        if (ObjectUtils.isEmpty(borrow)
                || (borrow.getStatus() != 0 && borrow.getStatus() != 1)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款状态已发生更改!"));
        }

        if (borrow.getMoneyYes()/borrow.getMoney() == 1){
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "满标后标的不可以撤销!"));
        }

        Specification<Tender> borrowSpecification = Specifications
                .<Tender>and()
                .eq("status", 1)
                .eq("borrowId", borrowId)
                .build();

        List<Tender> tenderList = tenderService.findList(borrowSpecification);
        Set<Long> userIdSet = tenderList.stream().map(p -> p.getUserId()).collect(Collectors.toSet());   // 投标的UserID

        // ======================================
        //  更改投资记录标识, 并且解冻投资资金
        // ======================================
        VoCancelThirdTenderReq voCancelThirdTenderReq = null;
        for (Tender tender : tenderList) {
            tender.setUpdatedAt(nowDate);
            tender.setStatus(2);
            tenderService.save(tender);

            //==================================================================
            //取消即信投资人投标记录
            //==================================================================
            if (!ObjectUtils.isEmpty(tender.getThirdTenderOrderId())) {
                voCancelThirdTenderReq = new VoCancelThirdTenderReq();
                voCancelThirdTenderReq.setTenderId(tender.getId());
                ResponseEntity<VoBaseResp> resp = tenderThirdBiz.cancelThirdTender(voCancelThirdTenderReq);
                if (!resp.getStatusCode().equals(HttpStatus.OK)) {
                    throw new Exception("borrowBizImpl pcCancelBorrow:" + resp.getBody().getState().getMsg());
                }
            }

            CapitalChangeEntity entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Unfrozen);
            entity.setUserId(tender.getUserId());
            entity.setMoney(tender.getValidMoney());
            entity.setRemark("借款 [" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "] 招标失败解除冻结资金。");
            capitalChangeHelper.capitalChange(entity);
        }

        //==================================================================
        //即信取消标的
        //==================================================================
        cancelThirdBorrow(borrow);

        // ======================================
        //  发送站内信
        // ======================================
        Notices notices;
        String content = String.format("你所投资的借款[ %s ]在 %s 已取消", BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()), DateHelper.nextDate(nowDate));
        for (Long toUserId : userIdSet) {
            notices = new Notices();
            notices.setFromUserId(1L);
            notices.setUserId(toUserId);
            notices.setRead(false);
            notices.setName("投资的借款失败");
            notices.setContent(content);
            notices.setType("system");
            notices.setCreatedAt(nowDate);
            notices.setUpdatedAt(nowDate);
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_NOTICE);
            mqConfig.setTag(MqTagEnum.NOTICE_PUBLISH);
            Map<String, String> body = GSON.fromJson(GSON.toJson(notices), TypeTokenContants.MAP_TOKEN);
            mqConfig.setMsg(body);
            try {
                log.info(String.format("borrowBizImpl pcCancelBorrow send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
            } catch (Throwable e) {
                log.error("borrowBizImpl pcCancelBorrow send mq exception", e);
            }
        }

        // 债权转让标识取消
        assertAndDoTransfer(borrow);

        //更新借款
        borrow.setStatus(5);
        borrow.setUpdatedAt(nowDate);
        borrowService.updateById(borrow);
        return ResponseEntity.ok(VoBaseResp.ok("取消借款成功!"));
    }

    /**
     * 取消第三方标的
     *
     * @param borrow
     */
    private void cancelThirdBorrow(Borrow borrow) throws Exception {
        //================================即信取消标的==================================
        String productId = borrow.getProductId();
        if (!StringUtils.isEmpty(productId)) {

            Map<String, Object> map = jdbcTemplate.queryForMap("select count(id) as count from gfb_borrow_tender where borrow_id = " + borrow.getId() + " and third_tender_order_id is not null AND third_tender_cancel_order_id is NULL ");
            if (NumberHelper.toInt(map.get("count")) <= 0) {
                VoCancelThirdBorrow voCancelThirdBorrow = new VoCancelThirdBorrow();
                voCancelThirdBorrow.setProductId(productId);
                voCancelThirdBorrow.setUserId(borrow.getUserId());
                voCancelThirdBorrow.setRaiseDate(DateHelper.dateToString(borrow.getReleaseAt(), DateHelper.DATE_FORMAT_YMD_NUM));
                voCancelThirdBorrow.setAcqRes(StringHelper.toString(borrow.getId()));
                ResponseEntity<VoBaseResp> resp = borrowThirdBiz.cancelThirdBorrow(voCancelThirdBorrow);
                if (!resp.getStatusCode().equals(HttpStatus.OK)) {
                    throw new Exception("borrowBizImpl cancelThirdBorrow:" + resp.getBody().getState().getMsg());
                }
            } else {
                log.error("当前标定中还存在未取消投标申请记录");
                throw new Exception("borrowBizImpl cancelThirdBorrow: 当前标定中还存在未取消投标申请记录");
            }
        }
    }

    /**
     * 非转让标复审
     *
     * @param borrow
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean notTransferBorrowAgainVerify(Borrow borrow) throws Exception {

        if ((ObjectUtils.isEmpty(borrow)) || (borrow.getStatus() != 1)
                || (!StringHelper.toString(borrow.getMoney()).equals(StringHelper.toString(borrow.getMoneyYes())))) {
            return false;
        }

        Date nowDate = new Date();
        // 生成还款记录
        disposeBorrowRepay(borrow, nowDate);
        //生成回款记录
        boolean generateState = disposeBorrowCollection(borrow, nowDate);
        if (!generateState) {
            return false;
        }

        // 复审事件
        //如果是流转标则扣除 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
        updateUserCacheByBorrowReview(borrow);
        //更新网站统计
        updateStatisticByBorrowReview(borrow);
        //借款成功发送通知短信
        smsNoticeByBorrowReview(borrow);
        //发送借款协议
        sendBorrowProtocol(borrow);
        return true;
    }

    /**
     * 生成还款记录
     *
     * @param borrow
     * @param nowDate
     */
    private void disposeBorrowRepay(Borrow borrow, Date nowDate) {
        // 调用利息计算器得出借款每期应还信息
        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(NumberHelper.toDouble(StringHelper.toString(borrow.getMoney())),
                NumberHelper.toDouble(StringHelper.toString(borrow.getApr())), borrow.getTimeLimit(), borrow.getSuccessAt());
        Map<String, Object> rsMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
        List<Map<String, Object>> repayDetailList = (List<Map<String, Object>>) rsMap.get("repayDetailList");
        BorrowRepayment borrowRepayment = null;
        for (int i = 0; i < repayDetailList.size(); i++) {
            borrowRepayment = new BorrowRepayment();
            Map<String, Object> repayDetailMap = repayDetailList.get(i);
            borrowRepayment.setBorrowId(borrow.getId());
            borrowRepayment.setStatus(0);
            borrowRepayment.setOrder(i);
            borrowRepayment.setRepayAt(DateHelper.stringToDate(StringHelper.toString(repayDetailMap.get("repayAt"))));
            borrowRepayment.setRepayMoney(new Double(NumberHelper.toDouble(repayDetailMap.get("repayMoney"))).intValue());
            borrowRepayment.setPrincipal(new Double(NumberHelper.toDouble(repayDetailMap.get("principal"))).intValue());
            borrowRepayment.setInterest(new Double(NumberHelper.toDouble(repayDetailMap.get("interest"))).intValue());
            borrowRepayment.setRepayMoneyYes(0);
            borrowRepayment.setCreatedAt(nowDate);
            borrowRepayment.setUpdatedAt(nowDate);
            borrowRepayment.setAdvanceMoneyYes(0);
            borrowRepayment.setLateDays(0);
            borrowRepayment.setLateInterest(0);
            borrowRepayment.setUserId(borrow.getUserId());
            borrowRepaymentService.save(borrowRepayment);
        }
    }

    /**
     * 转让标复审
     *
     * @return
     */
    @Transactional(rollbackFor = Throwable.class)
    public boolean transferBorrowAgainVerify(Borrow borrow) throws Exception {
        boolean bool = false;
        do {
            if ((ObjectUtils.isEmpty(borrow)) || (borrow.getStatus() != 1)
                    || (!StringHelper.toString(borrow.getMoney()).equals(StringHelper.toString(borrow.getMoneyYes())))) {
                break;
            }
            Long tenderId = borrow.getTenderId();
            List<BorrowCollection> transferedBorrowCollections = null;

            //============================更新转让标识=============================
            BorrowCollection borrowCollection = new BorrowCollection();
            borrowCollection.setTransferFlag(1);
            Specification<BorrowCollection> bcs = Specifications.<BorrowCollection>and()
                    .eq("tenderId", tenderId)
                    .eq("status", 0)
                    .build();
            borrowCollectionService.updateBySpecification(borrowCollection, bcs);

            Tender tender = tenderService.findById(tenderId);
            tender.setId(tenderId);
            tender.setTransferFlag(2);
            tenderService.updateById(tender);
            //======================================================================
            //扣除转让待收
            bcs = Specifications.<BorrowCollection>and()
                    .eq("tenderId", tenderId)
                    .eq("status", 0)
                    .eq("transferFlag", 1)
                    .build();

            transferedBorrowCollections = borrowCollectionService.findList(bcs, new Sort(Sort.Direction.ASC, "order"));

            Integer collectionMoney = 0;
            Integer collectionInterest = 0;
            for (BorrowCollection temp : transferedBorrowCollections) {
                collectionMoney += temp.getCollectionMoney();
                collectionInterest += temp.getInterest();
            }

            //更新资产记录
            CapitalChangeEntity entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.CollectionLower);
            entity.setUserId(borrow.getUserId());
            entity.setMoney(collectionMoney);
            entity.setInterest(collectionInterest);
            entity.setRemark("债权转让成功，扣除待收资金");
            capitalChangeHelper.capitalChange(entity);

            //生成回款记录
            bool = disposeBorrowCollection(borrow, transferedBorrowCollections.get(0).getStartAt());

            // 复审事件
            //如果是流转标则扣除 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
            updateUserCacheByBorrowReview(borrow);
            //更新网站统计
            updateStatisticByBorrowReview(borrow);
            //借款成功发送通知短信
            smsNoticeByBorrowReview(borrow);
            //发送借款协议
            sendBorrowProtocol(borrow);
        } while (false);
        return bool;
    }

    /**
     * 处理借款回款
     *
     * @param borrow
     * @param borrowDate
     * @return
     * @throws Exception
     */
    private boolean disposeBorrowCollection(Borrow borrow, Date borrowDate) throws Exception {
        Date nowDate = new Date();
        long borrowId = borrow.getId();
        Integer repayMoney = 0;
        Integer repayInterest = 0;
        Integer borrowType = borrow.getType();

        //投标用户id集合
        Set<Integer> tenderUserIds = new HashSet<>();
        CapitalChangeEntity entity = null;

        //查询当前借款的所有 状态为1的 tender记录
        Specification<Tender> ts = Specifications.<Tender>and()
                .eq("borrowId", borrowId)
                .eq("status", 1)
                .build();
        List<Tender> tenderList = tenderService.findList(ts);
        if (CollectionUtils.isEmpty(tenderList)) {
            return false;
        }

        for (Tender tender : tenderList) {
            BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(
                    NumberHelper.toDouble(StringHelper.toString(tender.getValidMoney())),
                    NumberHelper.toDouble(StringHelper.toString(borrow.getApr())), borrow.getTimeLimit(), borrowDate);
            Map<String, Object> rsMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
            List<Map<String, Object>> repayDetailList = (List<Map<String, Object>>) rsMap.get("repayDetailList");

            BorrowCollection borrowCollection = null;
            int collectionMoney = 0;
            int collectionInterest = 0;
            for (int i = 0; i < repayDetailList.size(); i++) {
                borrowCollection = new BorrowCollection();
                Map<String, Object> repayDetailMap = repayDetailList.get(i);
                collectionMoney += new Double(NumberHelper.toDouble(repayDetailMap.get("repayMoney"))).intValue();
                collectionInterest += new Double(NumberHelper.toDouble(repayDetailMap.get("interest"))).intValue();
                borrowCollection.setTenderId(tender.getId());
                borrowCollection.setStatus(0);
                borrowCollection.setOrder(i);
                borrowCollection.setUserId(tender.getUserId());
                borrowCollection.setStartAt(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : borrowDate);
                borrowCollection.setStartAtYes(i > 0 ? DateHelper.stringToDate(StringHelper.toString(repayDetailList.get(i - 1).get("repayAt"))) : nowDate);
                borrowCollection.setCollectionAt(DateHelper.stringToDate(StringHelper.toString(repayDetailMap.get("repayAt"))));
                borrowCollection.setCollectionMoney(new Double(NumberHelper.toDouble(repayDetailMap.get("repayMoney"))).intValue());
                borrowCollection.setPrincipal(new Double(NumberHelper.toDouble(repayDetailMap.get("principal"))).intValue());
                borrowCollection.setInterest(new Double(NumberHelper.toDouble(repayDetailMap.get("interest"))).intValue());
                borrowCollection.setCreatedAt(nowDate);
                borrowCollection.setUpdatedAt(nowDate);
                borrowCollection.setCollectionMoneyYes(0);
                borrowCollection.setLateDays(0);
                borrowCollection.setLateInterest(0);
                borrowCollection.setBorrowId(borrowId);
                borrowCollectionService.insert(borrowCollection);
            }

            //扣除冻结
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Tender);
            entity.setUserId(tender.getUserId());
            entity.setToUserId(borrow.getUserId());
            entity.setMoney(tender.getValidMoney());
            entity.setRemark("成功投资[" + BorrowHelper.getBorrowLink(borrowId, borrow.getName()) + "]");
            capitalChangeHelper.capitalChange(entity);

            //添加待收
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.CollectionAdd);
            entity.setUserId(tender.getUserId());
            entity.setToUserId(borrow.getUserId());
            entity.setMoney(collectionMoney);
            entity.setInterest(collectionInterest);
            entity.setRemark("添加待收金额");
            capitalChangeHelper.capitalChange(entity);

            //添加奖励
            if (borrow.getAwardType() > 0) {
                UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(tender.getUserId());

                int money = (int) MathHelper.myRound((tender.getValidMoney().doubleValue() / borrow.getMoney().doubleValue()) * borrow.getAward(), 2);
                if (borrow.getAwardType() == 2) {
                    money = (int) MathHelper.myRound(tender.getValidMoney().doubleValue() * borrow.getAward() / 100, 2);
                }

                String remark = "借款标‘" + borrow.getName() + "’的奖励";

                //查询红包账户
                DictValue dictValue = jixinCache.get(JixinContants.RED_PACKET_USER_ID);
                UserThirdAccount redPacketAccount = userThirdAccountService.findByUserId(NumberHelper.toLong(dictValue.getValue03()));

                //通过红包的形式发送奖励
                VoucherPayRequest voucherPayRequest = new VoucherPayRequest();
                voucherPayRequest.setAccountId(redPacketAccount.getAccountId());
                voucherPayRequest.setTxAmount(StringHelper.formatDouble(money, 100, false));
                voucherPayRequest.setForAccountId(userThirdAccount.getAccountId());
                voucherPayRequest.setDesLineFlag(DesLineFlagContant.TURE);
                voucherPayRequest.setChannel(ChannelContant.HTML);
                voucherPayRequest.setDesLine(remark);
                VoucherPayResponse response = jixinManager.send(JixinTxCodeEnum.SEND_RED_PACKET, voucherPayRequest, VoucherPayResponse.class);
                if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
                    String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
                    throw new Exception("发放投资奖励异常：" + msg);
                }

                entity = new CapitalChangeEntity();
                entity.setType(CapitalChangeEnum.Award);
                entity.setUserId(tender.getUserId());
                entity.setToUserId(borrow.getUserId());
                entity.setMoney(money);
                entity.setRemark(remark);
                capitalChangeHelper.capitalChange(entity);
            }

            if (!tenderUserIds.contains(tender.getUserId())) {
                Notices notices = new Notices();
                notices.setFromUserId(1L);
                notices.setUserId(tender.getUserId());
                notices.setRead(false);
                notices.setName("投资的借款满标审核通过");
                notices.setContent("您所投资的借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]在 " + DateHelper.dateToString(nowDate) + " 已满标审核通过");
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
                    log.info(String.format("borrowProvider doAgainVerify send mq %s", GSON.toJson(body)));
                    mqHelper.convertAndSend(mqConfig);
                } catch (Throwable e) {
                    log.error("borrowProvider doAgainVerify send mq exception", e);
                }

                //更新投标状态 为还款中
                tender.setState(2);
                tenderService.updateById(tender);
            }

            //触发投标成功事件
            //=============================================================
            //投资车贷标成功添加 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
            //更新 投过相应标种 标识
            //=============================================================
            updateUserCacheByTenderSuccess(tender, borrow, repayDetailList);
        }

        //借款入账
        //判断借款是否是受托支付：如果是受托支付则把款给收款人
        entity = new CapitalChangeEntity();
        entity.setType(CapitalChangeEnum.Borrow);
        entity.setUserId(ObjectUtils.isEmpty(borrow.getTakeUserId()) ? borrow.getUserId() : borrow.getTakeUserId());
        entity.setMoney(borrow.getMoney());
        entity.setRemark("通过[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]借到的款");

        if (borrow.getType() == 2) {
            entity.setAsset("add@noUseMoney");
        }
        capitalChangeHelper.capitalChange(entity);

        //扣除奖励
        Integer awardType = borrow.getAwardType();
        if (!ObjectUtils.isEmpty(awardType)) {
            entity = new CapitalChangeEntity();

            if (borrow.getType() == 2) {
                entity.setAsset("sub@noUseMoney");
            }
            int tempMoney = borrow.getAward();
            if (borrow.getAwardType() == 2) {
                tempMoney = borrow.getMoney() * borrow.getAward();
            }
            entity.setType(CapitalChangeEnum.Fee);
            entity.setUserId(borrow.getUserId());
            entity.setMoney(tempMoney);
            entity.setRemark("扣除借款标[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]的奖励");
            capitalChangeHelper.capitalChange(entity);
        }

        if ((borrow.getType() == 0) && (!ObjectUtils.isEmpty(borrow.getTenderId())) && (borrow.getTenderId() > 0)) { //转让管理费
            double transferFeeRate = Math.min(0.004 + 0.0008 * (borrow.getTotalOrder() - 1), 0.0128);

            //转让管理费
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Fee);
            entity.setUserId(borrow.getUserId());
            entity.setMoney((int) (borrow.getMoney() * transferFeeRate));
            entity.setRemark("扣除借款标[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]的转让管理费");
            capitalChangeHelper.capitalChange(entity);

        } else {
            //添加待还
            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.PaymentAdd);
            entity.setUserId(borrow.getUserId());
            entity.setMoney(repayMoney);
            entity.setInterest(repayInterest);
            entity.setRemark("添加待还金额");
            capitalChangeHelper.capitalChange(entity);
        }

        //净值账户管理费
        if (borrowType == 1) {
            double manageFeeRate = 0.0012;
            double fee = 0;
            if (borrow.getRepayFashion() == 1) {
                fee = MathHelper.myRound(borrow.getMoney() * manageFeeRate / 30 * borrow.getTimeLimit(), 2);
            } else {
                fee = MathHelper.myRound(borrow.getMoney() * manageFeeRate * borrow.getTimeLimit(), 2);
            }

            entity = new CapitalChangeEntity();
            entity.setType(CapitalChangeEnum.Manager);
            entity.setUserId(borrow.getUserId());
            entity.setMoney((int) fee);
            entity.setRemark("扣除借款标[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]的管理费");
            capitalChangeHelper.capitalChange(entity);
        }

        borrow.setStatus(3);
        borrow.setSuccessAt(nowDate);
        borrowService.updateById(borrow);
        return true;
    }

    /**
     * 投资车贷标成功添加 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
     * 更新 投过相应标种 标识
     *
     * @param tender
     * @param borrow
     * @param repayDetailList
     */
    private Map<String, Object> updateUserCacheByTenderSuccess(Tender tender, Borrow borrow, List<Map<String, Object>> repayDetailList) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        Users user = userService.findById(tender.getUserId());
        UserCache userCache = userCacheService.findById(tender.getUserId());
        log.debug("-------updateUserCacheByTenderSuccess---" + GSON.toJson(borrow) + "-------");
        log.debug("------------------");
        log.debug("-------updateUserCacheByTenderSuccess---" + GSON.toJson(tender) + "-------");
        if ((!borrow.isTransfer())
                && (!userCache.getTenderTuijian()) && (!userCache.getTenderQudao())) {
            //首次投资推荐标满2000元赠送流量
            Set<Integer> tempSet = new HashSet<>();
            tempSet.add(3);
            tempSet.add(5);
            tempSet.add(7);
            if ((!tempSet.contains(tender.getSource())) && tender.getValidMoney() >= 2000 * 100) {

            } else if ((user.getSource() == 5) && (tender.getValidMoney() >= 1000 * 100)) {

                MqConfig mqConfig = new MqConfig();
                mqConfig.setQueue(MqQueueEnum.RABBITMQ_ACTIVITY);
                mqConfig.setTag(MqTagEnum.GIVE_COUPON);
                ImmutableMap<String, String> body = ImmutableMap
                        .of(MqConfig.MSG_TENDER_ID, StringHelper.toString(tender.getId()), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
                mqConfig.setMsg(body);
                boolean mqState = false;
                try {
                    log.info(String.format("borrowBizImpl firstVerify send mq %s", GSON.toJson(body)));
                    mqState = mqHelper.convertAndSend(mqConfig);
                } catch (Throwable e) {
                    log.error("borrowBizImpl firstVerify send mq exception", e);
                }
                if (!mqState) {
                    log.error("赠送流量券失败!");
                }
            }
        }

        Integer countInterest = 0;
        for (int i = 0; i < repayDetailList.size(); i++) {
            Map<String, Object> repayDetailMap = repayDetailList.get(i);
            countInterest += new Double(NumberHelper.toDouble(repayDetailMap.get("interest"))).intValue();
        }

        UserCache tempUserCache = new UserCache();
        if (borrow.getType() == 0) {
            tempUserCache.setTjWaitCollectionPrincipal(userCache.getTjWaitCollectionPrincipal() + tender.getValidMoney());
            tempUserCache.setTjWaitCollectionInterest(userCache.getTjWaitCollectionInterest() + countInterest);
        }

        if (borrow.getType() == 4) {
            tempUserCache.setQdWaitCollectionPrincipal(userCache.getQdWaitCollectionPrincipal() + tender.getValidMoney());
            tempUserCache.setQdWaitCollectionInterest(userCache.getQdWaitCollectionInterest() + countInterest);
        }

        try {
            IncrStatistic incrStatistic = new IncrStatistic();
            if ((!userCache.getTenderTransfer()) && (!userCache.getTenderTuijian()) && (!userCache.getTenderJingzhi()) && (!userCache.getTenderMiao()) && (!userCache.getTenderQudao())) {
                incrStatistic.setTenderCount(1);
                incrStatistic.setTenderTotal(1);
            }

            if (borrow.isTransfer() && (!userCache.getTenderTransfer())) {
                tempUserCache.setTenderTransfer(true);
                incrStatistic.setTenderLzCount(1);
                incrStatistic.setTenderLzTotalCount(1);
            } else if ((borrow.getType() == 0) && (!userCache.getTenderTuijian())) {
                tempUserCache.setTenderTuijian(true);
                incrStatistic.setTenderTjCount(1);
                incrStatistic.setTenderTjTotalCount(1);
            } else if ((borrow.getType() == 1) && (!userCache.getTenderJingzhi())) {
                tempUserCache.setTenderJingzhi(true);
                incrStatistic.setTenderJzCount(1);
                incrStatistic.setTenderJzTotalCount(1);
            } else if ((borrow.getType() == 2) && (!userCache.getTenderMiao())) {
                tempUserCache.setTenderMiao(true);
                incrStatistic.setTenderMiaoCount(1);
                incrStatistic.setTenderMiaoTotalCount(1);
            } else if ((borrow.getType() == 4) && (!userCache.getTenderQudao())) {
                tempUserCache.setTenderQudao(true);
                incrStatistic.setTenderQdCount(1);
                incrStatistic.setTenderQdTotalCount(1);
            }
            if (!ObjectUtils.isEmpty(incrStatistic)) {
                incrStatisticBiz.caculate(incrStatistic);
            }
        } catch (Throwable e) {
            log.error(String.format("投标成功统计错误：%s", e.getMessage()));
        }

        //======================================
        // 老用户投标红包
        //======================================


        //======================================
        // 推荐用户投资红包
        //======================================

        return resultMap;
    }

    /**
     * 检查提前结清参数
     *
     * @param voRepayAll
     * @return
     */
    public ResponseEntity<VoBaseResp> checkRepayAll(VoRepayAll voRepayAll) {
        Long borrowId = voRepayAll.getBorrowId();
        Borrow borrow = borrowService.findByIdLock(borrowId);
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
        return null;
    }

    /**
     * 提前结清
     *
     * @param voRepayAll
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> repayAll(VoRepayAll voRepayAll) {

        ResponseEntity resp = checkRepayAll(voRepayAll);
        if (!ObjectUtils.isEmpty(resp)) {
            return resp;
        }

        Long borrowId = voRepayAll.getBorrowId();
        Borrow borrow = borrowService.findByIdLock(borrowId);
        Asset borrowAsset = assetService.findByUserId(borrow.getUserId());
        Preconditions.checkNotNull(borrowAsset, "借款人资产记录不存在!");

        int repaymentTotal = 0;
        List<VoRepayReq> voRepayReqList = new ArrayList<>();
        int penalty = 0;
        int lateInterest = 0;
        int lateDays = 0;
        int overPrincipal = 0;
        Date startAt = null;
        Date endAt = null;
        BorrowRepayment borrowRepayment = null;
        double interestPercent = 0;
        VoRepayReq voRepayReq = null;
        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrowId)
                .eq("status", 0)
                .build();
        List<BorrowRepayment> borrowRepaymentList = borrowRepaymentService.findList(brs);

        for (int i = 0; i < borrowRepaymentList.size(); i++) {
            borrowRepayment = borrowRepaymentList.get(i);

            if (borrowRepayment.getOrder() == 0) {
                startAt = DateHelper.beginOfDate(borrow.getSuccessAt());
            } else {
                startAt = DateHelper.beginOfDate(borrowRepaymentList.get(i - 1).getRepayAt());
            }
            endAt = DateHelper.beginOfDate(borrowRepayment.getRepayAt());

            //以结清第一期的14天利息作为违约金
            if (penalty == 0) {
                penalty = borrowRepayment.getInterest() / DateHelper.diffInDays(endAt, startAt, false) * 14;
            }

            Date nowStartDate = DateHelper.beginOfDate(new Date());
            if (nowStartDate.getTime() <= startAt.getTime()) {
                interestPercent = 0;
            } else {
                interestPercent = MathHelper.min(DateHelper.diffInDays(nowStartDate, startAt, false) / DateHelper.diffInDays(endAt, startAt, false), 1);
            }

            lateDays = DateHelper.diffInDays(nowStartDate, endAt, false);
            if (interestPercent == 1 && lateDays > 0) {
                for (int j = i; j < borrowRepaymentList.size(); j++) {
                    overPrincipal += borrowRepaymentList.get(j).getPrincipal();
                }
                lateInterest = new Double(overPrincipal * 0.004 * lateDays).intValue();
            } else {
                lateInterest = 0;
            }
            repaymentTotal += borrowRepayment.getPrincipal() + borrowRepayment.getInterest() * interestPercent + lateInterest;
            voRepayReq = new VoRepayReq();
            voRepayReq.setInterestPercent(interestPercent);
            voRepayReq.setRepaymentId(borrowRepayment.getId());
            voRepayReq.setUserId(borrowRepayment.getUserId());
            voRepayReq.setIsUserOpen(false);
            voRepayReqList.add(voRepayReq);
        }

        int repayMoney = repaymentTotal + penalty;
        if (borrowAsset.getUseMoney() < (repayMoney)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "结清总共需要还款 " + repayMoney + " 元，您的账户余额不足，请先充值!！"));
        }

        for (VoRepayReq tempVoRepayReq : voRepayReqList) {
            try {
                repaymentBiz.repayDeal(tempVoRepayReq);
            } catch (Throwable e) {
                log.error("提前结清异常：", e);
            }
        }

        if (penalty > 0) {
            CapitalChangeEntity entity = new CapitalChangeEntity();
            entity.setUserId(borrow.getUserId());
            entity.setType(CapitalChangeEnum.Fee);
            entity.setMoney(penalty);
            entity.setRemark("扣除提前结清的违约金");
            try {
                capitalChangeHelper.capitalChange(entity);
                receivedPenalty(borrow, penalty);
            } catch (Throwable e) {
                log.error("BorrowBizImpl 异常:", e);
            }
        }

        return ResponseEntity.ok(VoBaseResp.ok("提前结清成功!"));
    }

    /**
     * 提前结清给投资者违约金
     *
     * @param borrow
     * @param penalty
     */
    private void receivedPenalty(Borrow borrow, int penalty) throws Exception {
        Date nowDate = new Date();
        List<Long> collectionUserIds = new ArrayList<>();
        Specification<Tender> ts = Specifications
                .<Tender>and()
                .eq("status", 1)
                .build();
        Pageable pageable = null;
        List<Tender> tenderList = null;
        int pageNum = 0;
        int pageSize = 10;
        int tempPenalty = 0;
        Borrow tempBorrow = null;
        long tenderUserId = 0;
        UserThirdAccount tenderUserThirdAccount = null;
        do {
            pageable = new PageRequest(pageNum++, pageSize, new Sort(Sort.Direction.ASC));
            tenderList = tenderService.findList(ts, pageable);
            for (Tender tender : tenderList) {
                tenderUserId = tender.getUserId();
                tempPenalty = (int) MathHelper.myRound(tender.getValidMoney().doubleValue() / borrow.getMoney().doubleValue() * penalty, 0);
                if (tender.getTransferFlag() == 2) { //已转让
                    Specification<Borrow> bs = Specifications
                            .<Borrow>and()
                            .eq("tenderId", tender.getId())
                            .eq("status", 3)
                            .build();
                    List<Borrow> borrowList = borrowService.findList(bs);
                    receivedPenalty(borrowList.get(0), tempPenalty);
                    continue;
                }

                //查询红包账户
                DictValue dictValue = jixinCache.get(JixinContants.RED_PACKET_USER_ID);
                UserThirdAccount redPacketAccount = userThirdAccountService.findByUserId(NumberHelper.toLong(dictValue.getValue03()));

                tenderUserThirdAccount = userThirdAccountService.findByUserId(tenderUserId);
                //调用即信发送红包接口
                VoucherPayRequest voucherPayRequest = new VoucherPayRequest();
                voucherPayRequest.setAccountId(redPacketAccount.getAccountId());
                voucherPayRequest.setTxAmount(StringHelper.formatDouble(tempPenalty, 100, false));
                voucherPayRequest.setForAccountId(tenderUserThirdAccount.getAccountId());
                voucherPayRequest.setDesLineFlag(DesLineFlagContant.TURE);
                voucherPayRequest.setDesLine("借款'" + borrow.getName() + "'的违约金");
                voucherPayRequest.setChannel(ChannelContant.HTML);
                VoucherPayResponse response = jixinManager.send(JixinTxCodeEnum.SEND_RED_PACKET, voucherPayRequest, VoucherPayResponse.class);
                if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
                    String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
                    throw new Exception(msg);
                }

                CapitalChangeEntity entity = new CapitalChangeEntity();
                entity.setUserId(tenderUserId);
                entity.setType(CapitalChangeEnum.IncomeOther);
                entity.setMoney(tempPenalty);
                entity.setRemark("收到借款用户提前结清的违约金");
                capitalChangeHelper.capitalChange(entity);

                if (!collectionUserIds.contains(tenderUserId)) {
                    collectionUserIds.add(tenderUserId);
                    Notices notices = new Notices();
                    notices.setFromUserId(1L);
                    notices.setUserId(tenderUserId);
                    notices.setRead(false);
                    notices.setName("违约金");
                    notices.setContent("客户在" + DateHelper.dateToString(new Date()) + "已将借款[" + BorrowHelper.getBorrowLink(borrow.getId(), borrow.getName()) + "]]提前结清，收到" + tempPenalty + "元违约金");
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
                        log.info(String.format("borrowProvider doAgainVerify send mq %s", GSON.toJson(body)));
                        mqHelper.convertAndSend(mqConfig);
                    } catch (Throwable e) {
                        log.error("borrowProvider doAgainVerify send mq exception", e);
                    }
                }
            }
        } while (tenderList.size() < 10);
    }

    /**
     * 请求复审
     */
    public ResponseEntity<VoBaseResp> doAgainVerify(VoDoAgainVerifyReq voDoAgainVerifyReq) {
        String paramStr = voDoAgainVerifyReq.getParamStr();
        if (!SecurityHelper.checkSign(voDoAgainVerifyReq.getSign(), paramStr)) {
            log.error("BorrowBizImpl doAgainVerify error：签名校验不通过");
        }

        Map<String, String> paramMap = GSON.fromJson(paramStr, new TypeToken<Map<String, String>>() {
        }.getType());
        boolean flag = false;
        try {
            flag = borrowProvider.doAgainVerify(paramMap);
        } catch (Throwable e) {
            log.error("PC 复审异常", e);
        }
        return ResponseEntity.ok(VoBaseResp.ok(StringHelper.toString(flag)));
    }

    /**
     * 登记官方借款（车贷标、渠道标）
     *
     * @param voRegisterOfficialBorrow
     * @param request
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoHtmlResp> registerOfficialBorrow(VoRegisterOfficialBorrow voRegisterOfficialBorrow, HttpServletRequest request) {
        String paramStr = voRegisterOfficialBorrow.getParamStr();
        if (!SecurityHelper.checkSign(voRegisterOfficialBorrow.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc 登记官方借款 签名验证不通过", VoHtmlResp.class));
        }

        Map<String, String> paramMap = GSON.fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long borrowId = NumberHelper.toLong(paramMap.get("borrowId"));
        Borrow borrow = borrowService.findById(borrowId);

        Long userId = borrow.getUserId();
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        if (ObjectUtils.isEmpty(userThirdAccount)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款人未开通存管账户!", VoHtmlResp.class));
        }

        if (!userThirdAccount.getPasswordState().equals(1)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "借款人还未初始化银行交易密码!", VoHtmlResp.class));
        }

        Preconditions.checkNotNull(borrow, "借款不存在!");
        if (borrow.getStatus() != 0) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc 登记官方借款 该标已初审", VoHtmlResp.class));
        }

        ResponseEntity<VoBaseResp> resp = null;
        //检查标的是否登记
        if (StringUtils.isEmpty(borrow.getProductId())) {
            //即信标的登记
            VoCreateThirdBorrowReq voCreateThirdBorrowReq = new VoCreateThirdBorrowReq();
            voCreateThirdBorrowReq.setBorrowId(borrowId);
            voCreateThirdBorrowReq.setEntrustFlag(true);
            resp = borrowThirdBiz.createThirdBorrow(voCreateThirdBorrowReq);
            if (resp.getBody().getState().getCode() == VoBaseResp.ERROR) { //创建状态为失败时返回错误提示
                return ResponseEntity
                        .badRequest()
                        .body(VoHtmlResp.error(VoHtmlResp.ERROR, resp.getBody().getState().getMsg(), VoHtmlResp.class));
            }
        }

        //受托支付
        if (!ObjectUtils.isEmpty(borrow.getTakeUserId())) {
            VoThirdTrusteePayReq voThirdTrusteePayReq = new VoThirdTrusteePayReq();
            voThirdTrusteePayReq.setBorrowId(borrowId);
            return borrowThirdBiz.thirdTrusteePay(voThirdTrusteePayReq, request);
        } else {
            return ResponseEntity.ok(VoBaseResp.ok("初审成功", VoHtmlResp.class));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean doTrusteePay(Long borrowId) {
        Borrow borrow = borrowService.findByIdLock(borrowId);
        String productId = borrow.getProductId();
        Preconditions.checkNotNull(productId, "受托支付记录查询, 当前标的为登记");
        Long userId = borrow.getUserId();
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);

        TrusteePayQueryReq trusteePayQueryReq = new TrusteePayQueryReq();
        trusteePayQueryReq.setChannel(ChannelContant.HTML);
        trusteePayQueryReq.setAccountId(userThirdAccount.getAccountId());
        trusteePayQueryReq.setProductId(productId);
        TrusteePayQueryResp trusteePayQueryResp = jixinManager.send(JixinTxCodeEnum.TRUSTEE_PAY_QUERY, trusteePayQueryReq, TrusteePayQueryResp.class);
        if ((ObjectUtils.isEmpty(trusteePayQueryResp))
                || !(JixinResultContants.SUCCESS.equals(trusteePayQueryResp.getRetCode()))) {
            return false;
        }

        if (!trusteePayQueryResp.getState().equals("1")) {
            return false;
        }

        // 确认后初审
        MqConfig mqConfig = new MqConfig();
        mqConfig.setQueue(MqQueueEnum.RABBITMQ_BORROW);
        mqConfig.setTag(MqTagEnum.FIRST_VERIFY);
        ImmutableMap<String, String> body = ImmutableMap
                .of(MqConfig.MSG_BORROW_ID, StringHelper.toString(borrowId), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
        mqConfig.setMsg(body);
        try {
            log.info(String.format("borrowBizImpl firstVerify send mq %s", GSON.toJson(body)));
            mqHelper.convertAndSend(mqConfig);
        } catch (Throwable e) {
            log.error("borrowBizImpl firstVerify send mq exception", e);
        }
        return true;
    }

    /**
     * 发送借款协议
     *
     * @param borrow
     */
    public void sendBorrowProtocol(Borrow borrow) {
        List<Tender> tenderList = null;
        Users borrowUser = null;
        List<Users> tenderUserList = null;
        Map<String, Object> borrowMap = null;
        List<Map<String, Object>> tenderMapList = null;
        Map<String, Object> calculatorMap = null;
        String content = null;
        String username = null;

        if (!ObjectUtils.isEmpty(borrow)) {

            //查询借款信息

            borrowMap = GSON.fromJson(GSON.toJson(borrow), new com.google.common.reflect.TypeToken<Map<String, Object>>() {
            }.getType());
            borrowUser = userService.findById(borrow.getUserId());
            username = borrowUser.getUsername();

            borrowMap.put("username", org.apache.commons.lang3.StringUtils.isEmpty(username) ? borrowUser.getPhone() : username);
            borrowMap.put("cardId", UserHelper.hideChar(borrowUser.getCardId(), UserHelper.CARD_ID_NUM));

            if (!ObjectUtils.isEmpty(borrow.getSuccessAt())) { //判断是否存在满标时间
                boolean successAtBool = DateHelper.getMonth(DateHelper.addMonths(borrow.getSuccessAt(), borrow.getTimeLimit())) % 12
                        !=
                        (DateHelper.getMonth(borrow.getSuccessAt()) + borrow.getTimeLimit()) % 12;

                String borrowExpireAtStr = null;
                String monthAsReimbursement = null;//月截止还款日
                if (borrow.getRepayFashion() == 1) {
                    borrowExpireAtStr = DateHelper.dateToString(DateHelper.addDays(borrow.getSuccessAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
                    monthAsReimbursement = borrowExpireAtStr;
                } else {
                    if (successAtBool) {
                        borrowExpireAtStr = DateHelper.dateToString(DateHelper.subDays(DateHelper.addDays(DateHelper.setDays(borrow.getSuccessAt(), borrow.getTimeLimit()), 1), 1), "yyyy-MM-dd HH:mm:ss");
                    } else {
                        borrowExpireAtStr = DateHelper.dateToString(DateHelper.addMonths(borrow.getSuccessAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
                    }
                    monthAsReimbursement = "每月" + DateHelper.getDay(borrow.getSuccessAt()) + "日";
                }
                borrowMap.put("borrowExpireAtStr", borrowExpireAtStr);
                borrowMap.put("monthAsReimbursement", monthAsReimbursement);
            }


            //使用当前借款计算利息信息
            BorrowCalculatorHelper borrowCalculatorHelper = null;

            //查询投标信息
            Specification<Tender> ts = Specifications
                    .<Tender>and()
                    .eq("borrowId", borrow.getId())
                    .build();

            tenderList = tenderService.findList(ts);

            if (!CollectionUtils.isEmpty(tenderList)) {
                List<Long> tenderUserIds = new ArrayList<>();

                tenderMapList = GSON.fromJson(GSON.toJson(tenderList), new com.google.common.reflect.TypeToken<List<Map<String, Object>>>() {
                }.getType());

                for (Tender tempTender : tenderList) {
                    tenderUserIds.add(tempTender.getUserId());
                }

                Specification<Users> us = Specifications
                        .<Users>and()
                        .in("id", tenderUserIds.toArray())
                        .build();

                tenderUserList = userService.findList(us);

                List<Map<String, Object>> tempTenderMapList = null;
                Map<String, String> msgMap = new HashMap<>();
                Users tenderUser = null;
                for (Map<String, Object> tempTenderMap : tenderMapList) {
                    tempTenderMapList = new ArrayList<>();

                    for (Users tempTenderUser : tenderUserList) {
                        if (NumberHelper.toInt(tempTenderMap.get("userId")) == NumberHelper.toInt(tempTenderUser.getId())) {
                            tenderUser = tempTenderUser;
                            break;
                        }
                    }

                    if (ObjectUtils.isEmpty(tenderUser.getEmail())) {
                        continue;
                    }

                    borrowCalculatorHelper = new BorrowCalculatorHelper(NumberHelper.toDouble(tempTenderMap.get("validMoney")), new Double(borrow.getApr()), borrow.getTimeLimit(), null);
                    calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
                    tempTenderMap.put("calculatorMap", calculatorMap);

                    username = tenderUser.getUsername();
                    tempTenderMap.put("username", org.apache.commons.lang3.StringUtils.isEmpty(username) ? tenderUser.getPhone() : username);

                    tempTenderMapList.add(tempTenderMap);

                    //使用thymeleaf模版引擎渲染 借款合同html
                    Map<String, Object> templateMap = new HashMap<>();
                    templateMap.put("borrowMap", borrowMap);
                    templateMap.put("tenderMapList", tempTenderMapList);
                    templateMap.put("calculatorMap", calculatorMap);
                    content = thymeleafHelper.build("borrowProtocol", templateMap);

                    // 使用消息队列发送邮件
                    MqConfig config = new MqConfig();
                    config.setQueue(MqQueueEnum.RABBITMQ_EMAIL);
                    config.setTag(MqTagEnum.SEND_BORROW_PROTOCOL_EMAIL);
                    ImmutableMap<String, String> body = ImmutableMap
                            .of(MqConfig.EMAIL, tenderUser.getEmail(),
                                    MqConfig.IP, "127.0.0.1",
                                    "subject", "广富宝金服借款协议",
                                    "content", content);
                    config.setMsg(body);
                    mqHelper.convertAndSend(config);

                }
            }
        }
    }

    /**
     * 借款成功发送通知短信
     *
     * @param borrow
     * @throws Exception
     */
    private void smsNoticeByBorrowReview(Borrow borrow) throws Exception {

        Users user = userService.findById(borrow.getUserId());

        if ((borrow.getType() == 1) && (!ObjectUtils.isEmpty(borrow.getLendId())) && ((borrow.getApr() / 100) > 1)
                && ((borrow.getRepayFashion() != 1) || (borrow.getTimeLimit() > 1))) {
            String phone = user.getPhone();

            if ((ObjectUtils.isEmpty(phone))) {

                long fee = 0;
                if (borrow.getRepayFashion() == 1) {
                    fee = Math.round(borrow.getMoney() * 0.12 / 30 * borrow.getTimeLimit());
                } else {
                    fee = Math.round(borrow.getMoney() * 0.12 * borrow.getTimeLimit());
                }

                // 使用消息队列发送短信
                MqConfig config = new MqConfig();
                config.setQueue(MqQueueEnum.RABBITMQ_SMS);
                config.setTag(MqTagEnum.SMS_BORROW_SUCCESS);
                ImmutableMap<String, String> body = ImmutableMap
                        .of(MqConfig.PHONE, phone,
                                MqConfig.IP, "127.0.0.1",
                                "money", StringHelper.formatDouble(borrow.getMoney(), 100.0, true),
                                "fee", StringHelper.formatDouble(fee, 100.0, true),
                                "id", StringHelper.toString(borrow.getId()));
                config.setMsg(body);

                mqHelper.convertAndSend(config);
            }
        }
    }

    /**
     * 如果是流转标则扣除 自身车贷标待收本金 和 推荐人的邀请用户车贷标总待收本金
     *
     * @param borrow
     */
    private void updateUserCacheByBorrowReview(Borrow borrow) throws Exception {
        UserCache userCache = userCacheService.findById(borrow.getUserId());

        if (borrow.isTransfer()) {
            Specification<BorrowCollection> bcs = Specifications
                    .<BorrowCollection>and()
                    .eq("status", 0)
                    .eq("tenderId", borrow.getTenderId())
                    .build();

            List<BorrowCollection> borrowCollectionList = borrowCollectionService.findList(bcs);
            if (CollectionUtils.isEmpty(borrowCollectionList)) {
                return;
            }

            Integer countInterest = 0;
            for (BorrowCollection borrowCollection : borrowCollectionList) {
                countInterest += borrowCollection.getInterest();
            }

            userCache.setUserId(userCache.getUserId());
            if (borrow.getType() == 0) {
                userCache.setTjWaitCollectionPrincipal(userCache.getTjWaitCollectionPrincipal() - borrow.getMoney());
                userCache.setTjWaitCollectionInterest(userCache.getTjWaitCollectionInterest() - countInterest);
            } else if (borrow.getType() == 4) {
                userCache.setQdWaitCollectionPrincipal(userCache.getQdWaitCollectionPrincipal() - borrow.getMoney());
                userCache.setQdWaitCollectionInterest(userCache.getQdWaitCollectionInterest() - countInterest);
            }
            userCacheService.save(userCache);
        }
    }

    /**
     * 更新网站统计
     *
     * @param borrow
     */
    private void updateStatisticByBorrowReview(Borrow borrow) {
        Date nowDate = new Date();

        Specification<BorrowRepayment> brs = Specifications
                .<BorrowRepayment>and()
                .eq("borrowId", borrow.getId())
                .build();

        List<BorrowRepayment> repaymentList = borrowRepaymentService.findList(brs);
        if (CollectionUtils.isEmpty(repaymentList)) {//查询当前借款 还款记录
            return;
        }

        Integer repayMoney = 0;
        Integer principal = 0;
        for (BorrowRepayment borrowRepayment : repaymentList) {
            repayMoney += borrowRepayment.getRepayMoney();
            principal += borrowRepayment.getPrincipal();
        }

        //全站统计
        Statistic statistic = new Statistic();
        Integer borrowMoney = borrow.getMoney();

        statistic.setBorrowItems(1L);
        statistic.setBorrowTotal((long) borrowMoney);
        statistic.setWaitRepayTotal((long) repayMoney);

        if (borrow.isTransfer()) {
            statistic.setLzBorrowTotal((long) borrowMoney);
        } else if (borrow.getType() == 0) {//0：车贷标；1：净值标；2：秒标；4：渠道标；
            statistic.setTjBorrowTotal((long) borrowMoney);
            statistic.setTjWaitRepayPrincipalTotal((long) principal);
            statistic.setTjWaitRepayTotal((long) repayMoney);
        } else if (borrow.getType() == 1) {
            statistic.setJzBorrowTotal((long) borrowMoney);
            statistic.setJzWaitRepayPrincipalTotal((long) principal);
            statistic.setJzWaitRepayTotal((long) repayMoney);
        } else if (borrow.getType() == 2) {
            statistic.setMbBorrowTotal((long) borrowMoney);
        } else if (borrow.getType() == 4) {
            statistic.setQdBorrowTotal((long) borrowMoney);
            statistic.setQdWaitRepayPrincipalTotal((long) principal);
            statistic.setQdWaitRepayTotal((long) repayMoney);
        }
        if (!ObjectUtils.isEmpty(statistic)) {
            try {
                statisticBiz.caculate(statistic);
            } catch (Throwable e) {
                log.error("borrowProvider updateStatisticByBorrowReview 异常:", e);
            }
        }
    }

    /**
     * 初审
     *
     * @param borrowId
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean doFirstVerify(Long borrowId) throws Exception {

        log.info(String.format("触发标的初审: %s", borrowId));
        Borrow borrow = borrowService.findByIdLock(borrowId);
        if ((ObjectUtils.isEmpty(borrow)) || (borrow.getStatus() != 0)) {
            return false;
        }

        if (!ObjectUtils.isEmpty(borrow.getLendId())) {
            return verifyLendBorrow(borrow);      //有草出借初审
        } else {
            return verifyStandardBorrow(borrow);  //标准标的初审
        }

    }


    /**
     * 车贷标、净值标、渠道标、转让标初审
     *
     * @return
     */

    private boolean verifyStandardBorrow(Borrow borrow) {
        Date nowDate = DateHelper.subSeconds(new Date(), 10);
        borrow.setStatus(1);
        borrow.setVerifyAt(nowDate);
        Date releaseAt = borrow.getReleaseAt();
        borrow.setReleaseAt(ObjectUtils.isEmpty(releaseAt) ? nowDate : releaseAt);   // 处理不填写发布时间的请款
        borrowService.updateById(borrow);    //更新借款状态

        // 自动投标前提:
        // 1.没有设置标密码
        // 2.车贷标, 渠道标, 流转表
        // 3.标的年化率为 800 以上
        Integer borrowType = borrow.getType();
        ImmutableList<Integer> autoTenderBorrowType = ImmutableList.of(0, 1, 4);
        if ((ObjectUtils.isEmpty(borrow.getPassword()))
                && (autoTenderBorrowType.contains(borrowType)) && borrow.getApr() > 800) {
            borrow.setIsLock(true);
            borrowService.updateById(borrow);  // 锁住标的,禁止手动投标
            if (borrow.getIsNovice()) {   // 对于新手标直接延迟8点后推送
                Date noviceBorrowStandeReaseAt = DateHelper.addHours(DateHelper.beginOfDate(new Date()), 20);  // 新手标 能进行制动的时间
                releaseAt = DateHelper.max(noviceBorrowStandeReaseAt, releaseAt);
            }

            //触发自动投标队列
            MqConfig mqConfig = new MqConfig();
            mqConfig.setQueue(MqQueueEnum.RABBITMQ_AUTO_TENDER);
            mqConfig.setTag(MqTagEnum.AUTO_TENDER);
            mqConfig.setSendTime(releaseAt);
            ImmutableMap<String, String> body = ImmutableMap
                    .of(MqConfig.MSG_BORROW_ID, StringHelper.toString(borrow.getId()), MqConfig.MSG_TIME, DateHelper.dateToString(new Date()));
            mqConfig.setMsg(body);
            try {
                log.info(String.format("borrowProvider autoTender send mq %s", GSON.toJson(body)));
                mqHelper.convertAndSend(mqConfig);
                return true;
            } catch (Throwable e) {
                log.error("borrowProvider autoTender send mq exception", e);
                return false;
            }
        }
        return true;
    }

    /**
     * 摘草 生成借款 初审
     *
     * @param borrow
     * @return
     * @throws Exception
     */
    private boolean verifyLendBorrow(Borrow borrow) throws Exception {
        Date nowDate = DateHelper.subSeconds(new Date(), 10);
        borrow.setStatus(1);  //更新借款状态
        borrow.setVerifyAt(nowDate);
        Date releaseAt = borrow.getReleaseAt();
        borrow.setReleaseAt(ObjectUtils.isEmpty(releaseAt) ? nowDate : releaseAt);
        borrowService.save(borrow);   // 更改标的为可投标状态
        Long lendId = borrow.getLendId();

        Lend lend = lendService.findById(lendId);
        VoCreateTenderReq voCreateTenderReq = new VoCreateTenderReq();
        voCreateTenderReq.setUserId(lend.getUserId());
        voCreateTenderReq.setBorrowId(borrow.getId());
        voCreateTenderReq.setTenderMoney(MathHelper.myRound(borrow.getMoney() / 100.0, 2));
        ResponseEntity<VoBaseResp> response = tenderBiz.createTender(voCreateTenderReq);
        return response.getStatusCode().equals(HttpStatus.OK);
    }

    /**
     * pc初审
     *
     * @param voPcDoFirstVerity
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> pcFirstVerify(VoPcDoFirstVerity voPcDoFirstVerity) throws Exception {
        String paramStr = voPcDoFirstVerity.getParamStr();
        if (!SecurityHelper.checkSign(voPcDoFirstVerity.getSign(), paramStr)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "pc去初审 签名验证不通过!"));
        }

        Map<String, String> paramMap = new Gson().fromJson(paramStr, TypeTokenContants.MAP_ALL_STRING_TOKEN);
        Long borrowId = NumberHelper.toLong(paramMap.get("borrowId"));
        if (doFirstVerify(borrowId)) {
            return ResponseEntity.ok(VoBaseResp.ok("初审成功!"));
        } else {
            return ResponseEntity.
                    badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "初审失败!"));
        }
    }
}
