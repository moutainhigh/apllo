package com.gofobao.framework.borrow.service.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.borrow.contants.BorrowContants;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.repository.BorrowRepository;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.borrow.vo.request.VoBorrowListReq;
import com.gofobao.framework.borrow.vo.response.*;
import com.gofobao.framework.common.constans.MoneyConstans;
import com.gofobao.framework.helper.*;
import com.gofobao.framework.helper.project.BorrowCalculatorHelper;
import com.gofobao.framework.helper.project.UserHelper;
import com.gofobao.framework.member.entity.UserAttachment;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.repository.UserAttachmentRepository;
import com.gofobao.framework.member.repository.UsersRepository;
import com.gofobao.framework.tender.contants.TenderConstans;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.repository.TenderRepository;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gofobao.framework.helper.project.UserHelper.*;
import static java.util.stream.Collectors.groupingBy;


/**
 * Created by admin on 2017/5/17.
 */
@Component
@Slf4j
@SuppressWarnings("all")
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private BorrowRepository borrowRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserAttachmentRepository userAttachmentRepository;

    @Autowired
    private UsersRepository usersRepository;


    @Autowired
    private TenderRepository tenderRepository;

    @Autowired
    private ThymeleafHelper thymeleafHelper;

    @Value("${gofobao.imageDomain}")
    private String imageDomain;

    /**
     * 首页标列表
     *
     * @param voBorrowListReq
     * @return
     */
    @Override
    public List<VoViewBorrowList> findAll(VoBorrowListReq voBorrowListReq) {

        Integer type = voBorrowListReq.getType();
        List<Integer> typeArray = Arrays.asList(-1, 1, 2, 0, 4, 5);
        Boolean flag = typeArray.contains(type);
        if (!flag) {
            return Collections.EMPTY_LIST;
        }
        if (type == -1) {
            type = null;
        }
        //过滤掉 发标待审 初审不通过；复审不通过 已取消
        List statusArray = Lists.newArrayList(
                new Integer(BorrowContants.CANCEL),
                new Integer(BorrowContants.NO_PASS),
                new Integer(BorrowContants.RECHECK_NO_PASS),
                new Integer(BorrowContants.PENDING));

        StringBuilder pageSb = new StringBuilder(" SELECT b FROM Borrow b WHERE 1=1 ");
        StringBuilder countSb = new StringBuilder(" SELECT COUNT(id) FROM Borrow b WHERE 1=1 ");
        StringBuilder condtionSql = new StringBuilder("");

        // 条件
        if (type != null) {  // 全部

            if (type == 5) {
                condtionSql.append(" AND b.tenderId is not null ");
            } else {
                condtionSql.append(" AND b.type=" + type);
            }

        }
        condtionSql.append(" AND b.verifyAt IS Not NULL AND b.status NOT IN(:statusArray)");
        // 排序
        if ("-1".equals(type)) {   // 全部
            condtionSql.append(" ORDER BY FIELD(b.type,0, 4, 1, 2),(b.moneyYes / b.money) DESC, b.id DESC");
        } else {
            if (ObjectUtils.isEmpty(BorrowContants.INDEX_TYPE_CE_DAI)) {
                condtionSql.append(" ORDER BY b.status ASC,(b.moneyYes / b.money) DESC, b.successAt DESC,b.id DESC");
            } else {
                condtionSql.append(" ORDER BY b.status, b.successAt DESC, b.id DESC");
            }
        }

        Query pageQuery = entityManager.createQuery(pageSb.append(condtionSql).toString(), Borrow.class);
        pageQuery.setParameter("statusArray", statusArray);
        int firstResult = voBorrowListReq.getPageIndex() * voBorrowListReq.getPageSize();
        List<Borrow> borrowLists = pageQuery
                .setFirstResult(firstResult)
                .setMaxResults(voBorrowListReq.getPageSize())
                .getResultList();

        if (CollectionUtils.isEmpty(borrowLists)) {
            return Collections.EMPTY_LIST;
        }

        return commonHandle(borrowLists, voBorrowListReq);
    }


    private List<VoViewBorrowList> commonHandle(List<Borrow> borrowLists, VoBorrowListReq voBorrowListReq) {
        List<VoViewBorrowList> listResList = new ArrayList<>(voBorrowListReq.getPageSize());
        Set<Long> userIdArray = borrowLists.stream().map(p -> p.getUserId()).collect(Collectors.toSet());
        List<Users> usersList = usersRepository.findByIdIn(new ArrayList(userIdArray));
        Map<Long, Users> usersMap = usersList.stream().collect(Collectors.toMap(Users::getId, Function.identity()));
        borrowLists.stream().forEach(m -> {
            VoViewBorrowList item = new VoViewBorrowList();
            item.setId(m.getId());
            item.setMoney(StringHelper.formatMon(m.getMoney() / 100d) + MoneyConstans.RMB);
            item.setIsContinued(m.getIsContinued());
            item.setLockStatus(m.getIsLock());
            item.setIsImpawn(m.getIsImpawn());
            item.setApr(StringHelper.formatMon(m.getApr() / 100d) + MoneyConstans.PERCENT);
            item.setName(m.getName());
            item.setMoneyYes(StringHelper.formatMon(m.getMoneyYes() / 100d) + MoneyConstans.RMB);
            item.setIsNovice(m.getIsNovice());
            item.setIsMortgage(m.getIsMortgage());
            if (m.getType() == BorrowContants.REPAY_FASHION_ONCE) {
                item.setTimeLimit(m.getTimeLimit() + BorrowContants.DAY);
            } else {
                item.setTimeLimit(m.getTimeLimit() + BorrowContants.MONTH);
            }

            //待发布时间
            item.setSurplusSecond(-1L);
            //进度
            item.setSpend(0d);

            //1.待发布 2.还款中 3.招标中 4.已完成 5.其它
            Integer status = m.getStatus();
            Date nowDate = new Date(System.currentTimeMillis());
            Date releaseAt = m.getReleaseAt();  //发布时间

            if (status == BorrowContants.BIDDING) {//招标中
                Integer validDay = m.getValidDay();
                Date endAt = DateHelper.addDays(DateHelper.beginOfDate(m.getReleaseAt()), validDay + 1);
                //待发布
                if (releaseAt.getTime() >=nowDate.getTime()) {
                    status = 1;
                    item.setSurplusSecond((releaseAt.getTime() - nowDate.getTime()) + 5);
                } else if (nowDate.getTime() >= endAt.getTime()) {  //当前时间大于招标有效时间
                    status = 5; //已过期
                } else {
                    try {
                        System.out.println(JacksonHelper.obj2json(m));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    status = 3; //招标中
                    //  进度
                    item.setSpend(Double.parseDouble(StringHelper.formatMon(m.getMoneyYes().doubleValue() / m.getMoney())));
                }
            } else if (!ObjectUtils.isEmpty(m.getSuccessAt()) && !ObjectUtils.isEmpty(m.getCloseAt())) {   //满标时间 结清
                status = 4; //已完成
            } else if (status == BorrowContants.PASS && ObjectUtils.isEmpty(m.getCloseAt())) {
                status = 2; //还款中
            }

            Long userId = m.getUserId();
            Users user = usersMap.get(userId);
            item.setUserName(!StringUtils.isEmpty(user.getUsername()) ? user.getUsername() : user.getPhone());
            item.setType(m.getType());
            if (!StringUtils.isEmpty(m.getTenderId()) && m.getTenderId() > 0) {
                item.setIsFlow(true);
                item.setType(5);
            } else {
                item.setIsFlow(false);
            }
            item.setReleaseAt(DateHelper.dateToString(m.getReleaseAt()));
            item.setStatus(status);
            item.setRepayFashion(m.getRepayFashion());
            item.setIsContinued(m.getIsContinued());
            item.setIsConversion(m.getIsConversion());
            item.setIsVouch(m.getIsVouch());
            item.setTenderCount(m.getTenderCount());
            item.setAvatar(imageDomain + "/data/images/avatar/" + userId + "_avatar_small.jpg");
            //   item.setPageCount(count.intValue());
            listResList.add(item);
        });

        Optional<List<VoViewBorrowList>> result = Optional.empty();
        return result.ofNullable(listResList).orElse(Collections.emptyList());

    }


    /**
     * pc:首页标列表
     *
     * @param voBorrowListReq
     * @return
     */
    @Override
    public VoPcBorrowList pcFindAll(VoBorrowListReq voBorrowListReq) {

        Integer type = voBorrowListReq.getType();
        List<Integer> typeArray = Arrays.asList(-1, 1, 2, 0, 4, 5);
        Boolean flag = typeArray.contains(type);
        if (!flag) {
            return null;
        }
        if (type == -1) {
            type = null;
        }
        //过滤掉 发标待审 初审不通过；复审不通过 已取消
        List statusArray = Lists.newArrayList(
                new Integer(BorrowContants.CANCEL),
                new Integer(BorrowContants.NO_PASS),
                new Integer(BorrowContants.RECHECK_NO_PASS),
                new Integer(BorrowContants.PENDING));

        StringBuilder pageSb = new StringBuilder(" SELECT b FROM Borrow b WHERE 1=1 ");
        StringBuilder countSb = new StringBuilder(" SELECT COUNT(id) FROM Borrow b WHERE 1=1 ");
        StringBuilder condtionSql = new StringBuilder("");

        // 条件
        if (type != null) {  // 全部

            if (type == 5) {
                condtionSql.append(" AND b.tenderId is not null ");
            } else {
                condtionSql.append(" AND b.type=" + type);
            }

        }
        condtionSql.append(" AND b.verifyAt IS Not NULL AND b.status NOT IN(:statusArray)");
        // 排序
        if ("-1".equals(type)) {   // 全部
            condtionSql.append(" ORDER BY FIELD(b.type,0, 4, 1, 2),(b.moneyYes / b.money) DESC, b.id DESC");
        } else {
            if (ObjectUtils.isEmpty(BorrowContants.INDEX_TYPE_CE_DAI)) {
                condtionSql.append(" ORDER BY b.status ASC,(b.moneyYes / b.money) DESC, b.successAt DESC,b.id DESC");
            } else {
                condtionSql.append(" ORDER BY b.status, b.successAt DESC, b.id DESC");
            }
        }
        //分页
        Query pageQuery = entityManager.createQuery(pageSb.append(condtionSql).toString(), Borrow.class);
        pageQuery.setParameter("statusArray", statusArray);
        List<Borrow> borrowLists = pageQuery
                .setFirstResult(voBorrowListReq.getPageIndex() * voBorrowListReq.getPageSize())
                .setMaxResults(voBorrowListReq.getPageSize())
                .getResultList();

        if (CollectionUtils.isEmpty(borrowLists)) {
            return null;
        }

        List<VoViewBorrowList> borrowListList = commonHandle(borrowLists, voBorrowListReq);
        VoPcBorrowList warpRes = new VoPcBorrowList();
        warpRes.setBorrowLists(borrowListList);
        warpRes.setPageIndex(voBorrowListReq.getPageIndex() + 1);
        warpRes.setPageSize(voBorrowListReq.getPageSize());
        //总记录数
        Query countQuery = entityManager.createQuery(countSb.append(condtionSql).toString(), Long.class);
        countQuery.setParameter("statusArray", statusArray);
        Long count = (Long) countQuery.getSingleResult();
        warpRes.setTotalCount(count.intValue());
        return warpRes;

    }


    /**
     * 标详情
     *
     * @param borrowId
     * @return
     */
    @Override
    public Borrow findByBorrowId(Long borrowId) {
        Borrow borrow = borrowRepository.findOne(new Long(borrowId));
        return borrow;
    }

    /**
     * 标简介
     *
     * @param borrowId
     * @return
     */
    @Override
    public VoBorrowDescRes desc(Long borrowId) {
        VoBorrowDescRes voViewBorrowDescRes = new VoBorrowDescRes();
        Borrow borrow = borrowRepository.findOne(borrowId);
        if (ObjectUtils.isEmpty(borrow)) {
            return voViewBorrowDescRes;
        }
        Long userId = borrow.getUserId();
        voViewBorrowDescRes.setBorrowDesc(borrow.getDescription());
        List<UserAttachment> attachmentList = userAttachmentRepository.findByUserId(userId);
        if (!CollectionUtils.isEmpty(attachmentList)) {
            List<UserAttachmentRes> attachmentRes = Lists.newArrayList();
            attachmentList.stream().forEach(p -> {
                UserAttachmentRes attachment = new UserAttachmentRes();
                attachment.setFilepath(imageDomain + p.getFilepath());
                attachment.setName(p.getName());
                attachmentRes.add(attachment);
            });
            voViewBorrowDescRes.setUserAttachmentRes(attachmentRes);
        }
        return voViewBorrowDescRes;
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
        Borrow borrow = borrowRepository.findOne(borrowId);
        if (ObjectUtils.isEmpty(borrow)) {
            return null;
        }
        //发标用户
        Long borrowUserId = borrow.getUserId();
        Users users = usersRepository.findOne(borrowUserId);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(borrow);
        Map<String, Object> borrowMap = gson.fromJson(jsonStr, new TypeToken<Map<String, Object>>() {
        }.getType());

        borrowMap.put("username", StringUtils.isEmpty(users.getUsername()) ? users.getPhone() : users.getUsername());
        borrowMap.put("cardId", UserHelper.hideChar(users.getCardId(), CARD_ID_NUM));
        borrowMap.put("id", borrow.getId());
        borrowMap.put("money", StringHelper.formatMon(borrow.getMoney() / 100d));
        borrowMap.put("timeLimit", borrow.getTimeLimit() + "");
        borrowMap.put("apr", StringHelper.formatMon(borrow.getApr() / 100d));
        borrowMap.put("successAt", StringUtils.isEmpty(borrow.getSuccessAt()) ? null : DateHelper.dateToString(borrow.getSuccessAt()));
        borrowMap.put("endAt", DateHelper.dateToString(DateHelper.addDays(borrow.getReleaseAt(), borrow.getValidDay())));


        if (!ObjectUtils.isEmpty(borrow.getSuccessAt())) { //判断是否满标
            boolean successAtBool = DateHelper.getMonth(DateHelper.addMonths(borrow.getSuccessAt(), borrow.getTimeLimit())) % 12
                    !=
                    (DateHelper.getMonth(borrow.getSuccessAt()) + borrow.getTimeLimit()) % 12;

            String borrowExpireAtStr = null;
            String monthAsReimbursement = null;//月截止还款日
            if (borrow.getRepayFashion() == 1) {
                borrowExpireAtStr = DateHelper.dateToString(DateHelper.addDays(borrow.getSuccessAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
                monthAsReimbursement = borrowExpireAtStr;
            } else {
                monthAsReimbursement = "每月" + DateHelper.getDay(borrow.getSuccessAt()) + "日";

                if (successAtBool) {
                    borrowExpireAtStr = DateHelper.dateToString(DateHelper.subDays(DateHelper.addDays(DateHelper.setDays(borrow.getSuccessAt(), borrow.getTimeLimit()), 1), 1), "yyyy-MM-dd HH:mm:ss");
                } else {
                    borrowExpireAtStr = DateHelper.dateToString(DateHelper.addMonths(borrow.getSuccessAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
                }
            }
            borrowMap.put("borrowExpireAtStr", borrowExpireAtStr);
            borrowMap.put("monthAsReimbursement", monthAsReimbursement);
        }

        //使用当前借款息信计算利息
        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(new Double(borrow.getMoney()), new Double(borrow.getApr()), borrow.getTimeLimit(), null);
        Map calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());

        //查询投标信息


        List<Tender> borrowTenderList = new ArrayList<>();
        if (!StringUtils.isEmpty(userId) || userId > 0) {  //当前不是访客
            Specification specification;
            if (!borrowUserId.equals(userId)) {  //当前用户是否 发标用户
                specification = Specifications.<Tender>and()
                        .eq("userId", userId)
                        .eq("borrowId", borrowId)
                        .build();
            } else {
                specification = Specifications.<Tender>and()
                        .eq("borrowId", borrowId)
                        .build();
            }
            borrowTenderList = tenderRepository.findAll(specification);
        }
        List<Map<String, Object>> tenderMapList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(borrowTenderList)) {
            tenderMapList = gson.fromJson(
                    gson.toJson(borrowTenderList),
                    new TypeToken<List<Object>>() {
                    }.getType());

            List<Long> tenderUserList = borrowTenderList.stream().map(m -> m.getUserId()).collect(Collectors.toList());
            List<Users> usersList = usersRepository.findByIdIn(tenderUserList);
            Map<Long, Users> userMap = usersList.stream().collect(Collectors.toMap(Users::getId, Function.identity()));

            for (Map<String, Object> tempTenderMap : tenderMapList) {
                Long tempUserId = new Double(tempTenderMap.get("userId").toString()).longValue();
                Users usersTemp = userMap.get(tempUserId);
                tempTenderMap.put("username", UserHelper.hideChar(StringUtils.isEmpty(usersTemp.getUsername()) ? usersTemp.getPhone() : usersTemp.getUsername(), USERNAME_NUM));
                borrowCalculatorHelper = new BorrowCalculatorHelper(NumberHelper.toDouble(tempTenderMap.get("validMoney")), new Double(borrow.getApr()), borrow.getTimeLimit(), null);
                calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
                calculatorMap.put("earnings", StringHelper.formatMon(Double.parseDouble(calculatorMap.get("earnings").toString()) / 100));
                calculatorMap.put("eachRepay", StringHelper.formatMon(Double.parseDouble(calculatorMap.get("eachRepay").toString()) / 100));
                calculatorMap.put("repayTotal", StringHelper.formatMon(Double.parseDouble(calculatorMap.get("repayTotal").toString()) / 100));
                calculatorMap.put("repayDetailList", calculatorMap.get("repayDetailList"));

                tempTenderMap.put("calculatorMap", calculatorMap);
            }
        }

        //使用thymeleaf模版引擎渲染 借款合同html
        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("borrowMap", borrowMap);
        templateMap.put("tenderMapList", tenderMapList);
        templateMap.put("calculatorMap", calculatorMap);
        return templateMap;
    }

    /**
     * pc:招标统计
     *
     * @param
     * @return
     */
    @Override
    public List<BorrowStatistics> statistics() {
        List<BorrowStatistics> borrowStatisticss = Lists.newArrayList();
        StringBuilder sql = new StringBuilder("SELECT b  FROM Borrow AS b where 1=1 " +
                "AND  " +
                "b.status=3 " +
                "AND " +
                "b.successAt is null ");
        TypedQuery query1 = entityManager.createQuery(sql + " and b.tenderId is null", Borrow.class);
        BorrowStatistics borrowStatistics = new BorrowStatistics();
        Integer cheDai = 0;
        Integer jingZhi = 0;
        Integer quDao = 0;
        Integer miaoBiao = 0;
        Integer sum1 = 0;
        List<Borrow> borrowList = query1.getResultList();
        Date nowDate = new Date();
        if (!CollectionUtils.isEmpty(borrowList)) {
            List<Borrow> tempBorrowList = borrowList.stream().filter(p -> nowDate.getTime() < DateHelper.addDays(p.getReleaseAt(), p.getValidDay()).getTime()).collect(Collectors.toList());
            sum1 = tempBorrowList.size();
            Map<Integer, List<Borrow>> borrowMaps = tempBorrowList.stream().collect(groupingBy(Borrow::getType));
            borrowList = borrowMaps.get(BorrowContants.CE_DAI);
            cheDai = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.JING_ZHI);
            jingZhi = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.QU_DAO);
            quDao = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.MIAO_BIAO);
            miaoBiao = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
        }
        TypedQuery query2 = entityManager.createQuery(sql + " and b.tenderId IS NOT NULL", Borrow.class);
        List<Borrow> liuZhuanBorrow = query2.getResultList();
        List<Borrow> borrowList1 = liuZhuanBorrow.stream().filter(p -> nowDate.getTime() < DateHelper.addDays(p.getReleaseAt(), p.getValidDay()).getTime()).collect(Collectors.toList());
        Integer liuZhuanCount = borrowList1.size();
        borrowStatistics.setJingZhi(jingZhi);
        borrowStatistics.setCheDai(cheDai);
        borrowStatistics.setMiaoBiao(miaoBiao);
        borrowStatistics.setQuDao(quDao);
        borrowStatistics.setLiuZhuan(liuZhuanCount);
        borrowStatistics.setSum(sum1 + liuZhuanCount);
        borrowStatisticss.add(borrowStatistics);
        return borrowStatisticss;
    }

    @Override
    public Map<String, Object> pcContract(Long borrowId, Long userId) {

        Map<String, Object> resultMap = new HashMap<>();
        Borrow borrow = borrowRepository.findOne(borrowId);
        Long borrowUserId = borrow.getUserId();
        Users users = usersRepository.findOne(borrowUserId);
        String userName = StringUtils.isEmpty(users.getUsername()) ? UserHelper.hideChar(users.getPhone(), USERNAME_NUM) : UserHelper.hideChar(users.getUsername(), PHONE_NUM);

        BorrowerInfo borrowerInfo = new BorrowerInfo();
        borrowerInfo.setName(userName);
        borrowerInfo.setIdCard(UserHelper.hideChar(users.getCardId(), CARD_ID_NUM));
        borrowerInfo.setBorrowId(borrowId);
        resultMap.put("borrowInfo", borrowerInfo);

        RepaymentBasisInfo repaymentBasisInfo = new RepaymentBasisInfo();
        repaymentBasisInfo.setApr(StringHelper.formatMon(borrow.getApr() / 100d) + "%");
        repaymentBasisInfo.setMoney(StringHelper.formatMon(borrow.getMoney() / 100d));
        String timeLimit = borrow.getTimeLimit() + "";
        if (borrow.getRepayFashion() == 1) {
            timeLimit += BorrowContants.DAY;
        } else {
            timeLimit += BorrowContants.MONTH;
        }

        repaymentBasisInfo.setTimeLimit(timeLimit);
        repaymentBasisInfo.setRepayAt(DateHelper.dateToString(borrow.getReleaseAt()));
        repaymentBasisInfo.setEndAt(DateHelper.dateToString(DateHelper.addDays(borrow.getReleaseAt(), borrow.getValidDay())));

        //使用当前借款息信计算利息
        Integer borrowMoney = 0;
        if (!StringUtils.isEmpty(borrow.getSuccessAt())) {
            borrowMoney = borrow.getMoneyYes();
        } else {
            borrowMoney = borrow.getMoney();
        }
        String repaymentAt;
        if (borrow.getRepayFashion() == 1) {
            repaymentAt = DateHelper.dateToString(DateHelper.addDays(borrow.getSuccessAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
        } else {
            repaymentAt = "每月" + DateHelper.getDay(borrow.getSuccessAt()) + "日";
        }
        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(new Double(borrowMoney), new Double(borrow.getApr()), borrow.getTimeLimit(), borrow.getSuccessAt());
        Map calculatorMap = borrowCalculatorHelper.simpleCount(borrow.getRepayFashion());
        Double eachRepay = new Double(calculatorMap.get("eachRepay").toString());
        repaymentBasisInfo.setMonthAsReimbursement(StringHelper.formatMon(eachRepay / 100));
        repaymentBasisInfo.setMonthRepaymentAt(repaymentAt);
        resultMap.put("repaymentBasisInfo", repaymentBasisInfo);

        Specification specification;
        List<Tender> tenderList = Lists.newArrayList();
        if (userId > 0) {
            if (borrowUserId.equals(userId)) { //当前是发标用户
                specification = Specifications.<Tender>and()
                        .eq("borrowId", borrowId)
                        .eq("status", TenderConstans.SUCCESS)
                        .build();
            } else {
                specification = Specifications.<Tender>and()
                        .eq("borrowId", borrowId)
                        .eq("userId", userId)
                        .eq("status", TenderConstans.SUCCESS)
                        .build();
            }
            tenderList = tenderRepository.findAll(specification);
        }
        List<Lender> lenders = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(tenderList)) {
            List<Long> userIdArray = tenderList.stream().map(p -> p.getUserId()).collect(Collectors.toList());
            List<Users> usersList = usersRepository.findByIdIn(userIdArray);

            Map<Long, Users> usersMap = usersList.stream().collect(Collectors.toMap(Users::getId, Function.identity()));
            String temp = timeLimit;
            tenderList.stream().forEach(p -> {
                Lender lender = new Lender();
                lender.setTimeLimit(temp);
                lender.setMoney(StringHelper.formatMon(p.getValidMoney() / 100d));
                Users user = usersMap.get(p.getUserId());
                lender.setName(StringUtils.isEmpty(user.getUsername()) ? user.getPhone() : user.getUsername());
                BorrowCalculatorHelper userCalculator = new BorrowCalculatorHelper(new Double(p.getValidMoney()), new Double(borrow.getApr()), borrow.getTimeLimit(), borrow.getSuccessAt());
                Map userCalculatorMap = userCalculator.simpleCount(borrow.getRepayFashion());
                Double eachRepay1 = new Double(userCalculatorMap.get("eachRepay").toString());
                lender.setMonthAsReimbursement(StringHelper.formatMon(eachRepay1 / 100));
                lenders.add(lender);
            });
        }
        resultMap.put("lenders", CollectionUtils.isEmpty(lenders) ? "" : lenders);
        return resultMap;
    }

    public long countByUserIdAndStatusIn(Long userId, List<Integer> statusList) {
        return borrowRepository.countByUserIdAndStatusIn(userId, statusList);
    }

    public boolean insert(Borrow borrow) {
        if (ObjectUtils.isEmpty(borrow)) {
            return false;
        }
        borrow.setId(null);
        return !ObjectUtils.isEmpty(borrowRepository.save(borrow));
    }

    public boolean updateById(Borrow borrow) {
        if (ObjectUtils.isEmpty(borrow) || ObjectUtils.isEmpty(borrow.getId())) {
            return false;
        }
        return !ObjectUtils.isEmpty(borrowRepository.save(borrow));
    }

    public Borrow findByIdLock(Long borrowId) {
        return borrowRepository.findById(borrowId);
    }

    /**
     * 检查是否招标中
     *
     * @param borrow
     * @return
     */
    public boolean checkBidding(Borrow borrow) {
        if (ObjectUtils.isEmpty(borrow)) {
            return false;
        }
        return (borrow.getStatus() == 1 && borrow.getMoneyYes() < borrow.getMoney());
    }

    /**
     * 检查是否在发布时间内
     *
     * @param borrow
     * @return
     */
    public boolean checkReleaseAt(Borrow borrow) {
        Date releaseAt = borrow.getReleaseAt();
        if (ObjectUtils.isEmpty(borrow) || ObjectUtils.isEmpty(releaseAt)) {
            return false;
        }
        return new Date().getTime() > releaseAt.getTime();
    }

    /**
     * 检查招标时间是否有效
     *
     * @param borrow
     * @return
     */
    public boolean checkValidDay(Borrow borrow) {
        Date nowDate = new Date();
        Date validDate = DateHelper.beginOfDate(DateHelper.addDays(borrow.getReleaseAt(), borrow.getValidDay() + 1));
        return nowDate.getTime() < validDate.getTime();
    }

    public Borrow findById(Long borrowId) {
        return borrowRepository.findOne(borrowId);
    }

    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<Borrow> findList(Specification<Borrow> specification) {
        return borrowRepository.findAll(specification);
    }

    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<Borrow> findList(Specification<Borrow> specification, Sort sort) {
        return borrowRepository.findAll(specification, sort);
    }

    /**
     * 查询列表
     *
     * @param specification
     * @return
     */
    public List<Borrow> findList(Specification<Borrow> specification, Pageable pageable) {
        return borrowRepository.findAll(specification, pageable).getContent();
    }

    public long count(Specification<Borrow> specification) {
        return borrowRepository.count(specification);
    }

    /**
     * 查询最后一条borrow
     *
     * @return
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Borrow getLastBorrowLock() {
        Specification<Borrow> bs = Specifications
                .<Borrow>and()
                .build();

        Pageable pageable = new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "id"));
        List<Borrow> borrowList = borrowRepository.findAll(bs, pageable).getContent();
        return borrowList.get(0);
    }


}
