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
import com.gofobao.framework.tender.biz.TransferBiz;
import com.gofobao.framework.tender.contants.TenderConstans;
import com.gofobao.framework.tender.contants.TransferContants;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.entity.Transfer;
import com.gofobao.framework.tender.repository.TenderRepository;
import com.gofobao.framework.tender.repository.TransferRepository;
import com.gofobao.framework.tender.service.TransferService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    private TransferRepository transferRepository;


    @Autowired
    private TransferBiz transferBiz;


    //过滤掉 发标待审 初审不通过；复审不通过 已取消
    private static List statusArray = Lists.newArrayList(
            new Integer(BorrowContants.CANCEL),
            new Integer(BorrowContants.NO_PASS),
            new Integer(BorrowContants.RECHECK_NO_PASS),
            new Integer(BorrowContants.PENDING));


    LoadingCache<String, Borrow> newBorrow = CacheBuilder
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build(new CacheLoader<String, Borrow>() {
                ImmutableList states = ImmutableList.of(1, 3);

                @Override
                public Borrow load(String type) throws Exception {
                    Specification<Borrow> specification = Specifications
                            .<Borrow>and()
                            .eq("isNovice", true)
                            .in("status", states.toArray())
                            .build();
                    Page<Borrow> page = borrowRepository.findAll(specification,
                            new PageRequest(0, 1, new Sort(new Sort.Order(Sort.Direction.DESC, "id"))));
                    List<Borrow> content = page.getContent();
                    if (CollectionUtils.isEmpty(content)) {
                        Borrow borrow = new Borrow();
                        borrow.setId(0L);
                        return borrow;
                    } else {
                        return content.get(0);
                    }
                }
            });

    /**
     * 首页标列表
     *
     * @param voBorrowListReq
     * @return
     */
    @Override
    public List<VoViewBorrowList> findNormalBorrow(VoBorrowListReq voBorrowListReq) {
        VoPcBorrowList warpRes = new VoPcBorrowList();
        Integer type = voBorrowListReq.getType();
        List<Integer> typeArray = Arrays.asList(-1, 1, 2, 0, 4, 5);
        Boolean flag = typeArray.contains(type);
        if (!flag) {  //用户非法访问
            return new ArrayList<>(0);
        }
        if (type.intValue() == -1) {
            type = null;
        }
        StringBuilder condtionSql = new StringBuilder(" SELECT b.* FROM gfb_borrow  b   WHERE 1 = 1 AND is_finance = 0 AND b.product_id is not null  ");
        if (!StringUtils.isEmpty(type)) { // 标的状态
            condtionSql.append(" AND b.type = " + type + " AND  b.status NOT IN(:statusArray)  ");
        } else {  // 全部
            condtionSql.append(" AND b.status=:statusArray  AND b.type!=:type AND b.success_at is null AND  b.money_yes <>  b.money ");   // 为满标的
        }
        condtionSql.append(" AND b.verify_at IS Not NULL AND b.close_at is null AND b.product_id IS NOT NULL ");  // 排除验证时间不等于空, 关闭时间为空, 而且申报成功
        if (StringUtils.isEmpty(type)) {   // 全部
            condtionSql.append(" ORDER BY  FIELD(b.type, 0, 4, 1), b.status ASC ,  b.lend_repay_status ASC, (b.money_yes / b.money) DESC, b.id DESC ");
        } else {
            if (type.equals(BorrowContants.CE_DAI)) {
                condtionSql.append(" ORDER BY b.status ASC, b.lend_repay_status ASC, ( b.money_yes / b.money ) DESC, b.success_at DESC, b.id DESC ");
            } else {
                condtionSql.append(" ORDER BY  b.lend_repay_status ASC, ( b.money_yes / b.money ) DESC, b.status, b.success_at DESC, b.id DESC ");
            }
        }

        Query pageQuery = entityManager.createNativeQuery(condtionSql.toString(), Borrow.class);
        List<Borrow> borrowLists = Lists.newArrayList();
        if (StringUtils.isEmpty(type)) {
            if (voBorrowListReq.getPageIndex().intValue() == 0) {
                pageQuery.setParameter("statusArray", BorrowContants.BIDDING);
                pageQuery.setParameter("type", BorrowContants.JING_ZHI);
                pageQuery.setFirstResult(0);
                pageQuery.setMaxResults(30);
                borrowLists = pageQuery.getResultList();
            }
        } else {
            pageQuery.setParameter("statusArray", statusArray);
            int firstResult = voBorrowListReq.getPageIndex() * voBorrowListReq.getPageSize();
            pageQuery
                    .setFirstResult(firstResult)
                    .setMaxResults(voBorrowListReq.getPageSize());
            borrowLists = pageQuery.getResultList();
        }

        if (CollectionUtils.isEmpty(borrowLists) && !StringUtils.isEmpty(type)) {
            return Collections.EMPTY_LIST;
        }
        List<VoViewBorrowList> voViewBorrowLists = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(borrowLists)) {
            voViewBorrowLists = commonHandle(borrowLists, voBorrowListReq);
        }
        //全部显示列表
        if (StringUtils.isEmpty(type) && voBorrowListReq.getPageIndex().intValue() == 0) {
            //加上流转标的
            List<Transfer> transfers = transferRepository.indexList();
            if (!CollectionUtils.isEmpty(transfers)) {
                List<VoViewBorrowList> transferBorrow = transferBiz.commonHandel(transfers);
                for (VoViewBorrowList voBorrowList : transferBorrow) {
                    voViewBorrowLists.add(voBorrowList);
                }
            }
        }
        Integer tempCount = 10;
        //如果第一页 并且是“全部可投列表”
        if (voBorrowListReq.getPageIndex().intValue() == 0 && StringUtils.isEmpty(type)) {
            List<VoViewBorrowList> tempVoViewBorrows = Lists.newArrayList();
            if (!CollectionUtils.isEmpty(voViewBorrowLists)) {
                if (voViewBorrowLists.size() < tempCount) { //小于10条
                    tempVoViewBorrows = commonHandle(otherAdd(tempCount - voViewBorrowLists.size()), voBorrowListReq);
                }
            } else {  //为空 凑10条
                tempVoViewBorrows = commonHandle(otherAdd(tempCount), voBorrowListReq);
            }
            if (!CollectionUtils.isEmpty(tempVoViewBorrows)) {
                for (VoViewBorrowList viewBorrowList : tempVoViewBorrows) {
                    voViewBorrowLists.add(viewBorrowList);
                }
            }
        }
        return voViewBorrowLists;
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
            item.setIsPassWord(StringUtils.isEmpty(m.getPassword()) ? false : true);
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
            double spend = NumberHelper.floorDouble((m.getMoneyYes().doubleValue() / m.getMoney() * 100), 2);

            if (status == BorrowContants.BIDDING) {//招标中
                Integer validDay = m.getValidDay();
                Date endAt = DateHelper.addDays(DateHelper.beginOfDate(m.getReleaseAt()), validDay + 1);
                //待发布
                if (releaseAt.getTime() >= nowDate.getTime()) {
                    status = 1;
                    item.setSurplusSecond(((releaseAt.getTime() - nowDate.getTime()) / 1000) + 60);
                } else if (nowDate.getTime() >= endAt.getTime()) {  //当前时间大于招标有效时间
                    //流转标没有过期时间
                    if (!StringUtils.isEmpty(m.getTenderId())) {
                        status = 3; //招标中
                    } else {
                        status = 5; //已过期
                    }
                } else {
                    status = 3; //招标中
                    //  进度
                    if (spend == 100) {
                        status = 6;
                    }

                }
            } else if (!ObjectUtils.isEmpty(m.getRecheckAt()) && !ObjectUtils.isEmpty(m.getCloseAt())) {   //满标时间 结清
                status = 4; //已完成
            } else if (status == BorrowContants.PASS && ObjectUtils.isEmpty(m.getCloseAt())) {
                status = 2; //还款中
            }
            item.setSpend(spend);
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
            item.setReleaseAt(StringUtils.isEmpty(m.getReleaseAt()) ? "" : DateHelper.dateToString(m.getReleaseAt()));
            item.setRecheckAt(StringUtils.isEmpty(m.getRecheckAt()) ? "" : DateHelper.dateToString(m.getRecheckAt()));
            item.setStatus(status);
            item.setRepayFashion(m.getRepayFashion());
            item.setIsContinued(m.getIsContinued());
            item.setIsConversion(m.getIsConversion());
            item.setIsVouch(m.getIsVouch());
            item.setTenderCount(m.getTenderCount());
            item.setAvatar(StringUtils.isEmpty(user.getAvatarPath()) ? imageDomain + "/images/user/default_avatar.jpg" : user.getAvatarPath());
            listResList.add(item);
        });

        Optional<List<VoViewBorrowList>> result = Optional.empty();
        return result.ofNullable(listResList).orElse(Collections.emptyList());

    }


    /**
     * pc:首页理财标列表
     *
     * @param voBorrowListReq
     * @return
     */
    @Override
    public VoPcBorrowList pcFindAll(VoBorrowListReq voBorrowListReq) {
        VoPcBorrowList warpRes = new VoPcBorrowList();
        Integer type = voBorrowListReq.getType();
        List<Integer> typeArray = Arrays.asList(-1, 1, 2, 0, 4);
        Boolean flag = typeArray.contains(type);
        if (!flag) {  //用户非法访问
            return warpRes;
        }
        //全部
        if (type.intValue() == -1) {
            type = null;
        }
        StringBuilder pageSb = new StringBuilder(" SELECT b FROM Borrow b WHERE 1=1  AND b.productId IS NOT NULL ");
        StringBuilder countSb = new StringBuilder(" SELECT COUNT(id) FROM Borrow b WHERE 1=1 AND b.productId IS NOT NULL ");
        StringBuilder condtionSql = new StringBuilder(" AND isFinance = 0 ");

        if (StringUtils.isEmpty(type)) {  // 全部
            condtionSql.append(" AND b.successAt is null AND  b.moneyYes <> b.money AND  b.status=:statusArray and type!=:type");  // 可投
        } else {
            condtionSql.append(" AND b.closeAt is null AND b.status NOT IN(:statusArray ) AND  b.type=" + type);  //
        }
        condtionSql.append(" AND b.verifyAt IS Not NULL  ");
        // 排序
        if (StringUtils.isEmpty(type)) {   // 全部
            condtionSql.append(" ORDER BY  FIELD(b.type, 0, 4, 1, 2), b.status ASC,  b.lendRepayStatus ASC, (b.moneyYes / b.money) DESC,  b.id desc");
        } else {
            if (type.equals(BorrowContants.CE_DAI)) {
                condtionSql.append(" ORDER BY  b.lendRepayStatus ASC, (b.moneyYes / b.money) DESC, b.status asc, b.successAt desc, b.id desc ");
            } else {
                condtionSql.append(" ORDER BY  b.lendRepayStatus ASC, (b.moneyYes / b.money) DESC,  b.status, b.successAt desc, b.id desc ");
            }
        }
        //分页
        Query pageQuery = entityManager.createQuery(pageSb.append(condtionSql).toString(), Borrow.class);
        List<Borrow> borrowLists = Lists.newArrayList();
        if (StringUtils.isEmpty(type)) {  //首页全部 投标标+转让标（去掉净值标的）
            if (voBorrowListReq.getPageIndex().intValue() == 0) {
                pageQuery.setParameter("statusArray", BorrowContants.BIDDING);
                pageQuery.setParameter("type", BorrowContants.JING_ZHI);
                pageQuery.setFirstResult(0);
                pageQuery.setMaxResults(30);
                borrowLists = pageQuery.getResultList();
            }
        } else {
            pageQuery.setParameter("statusArray", statusArray);
            pageQuery
                    .setFirstResult(voBorrowListReq.getPageIndex() * voBorrowListReq.getPageSize())
                    .setMaxResults(voBorrowListReq.getPageSize());
            borrowLists = pageQuery.getResultList();
        }

        //不是全部显示列表并且普通标集合为空
        if (!StringUtils.isEmpty(type) && CollectionUtils.isEmpty(borrowLists)) {
            return warpRes;
        }
        //普通标集合
        List<VoViewBorrowList> borrowListList = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(borrowLists)) {
            borrowListList = commonHandle(borrowLists, voBorrowListReq);
        }
        //全部显示 可投 加上 转让标集合
        if (StringUtils.isEmpty(type)) {
            List<Transfer> transfers = transferRepository.indexList();
            if (!CollectionUtils.isEmpty(transfers)) {
                List<VoViewBorrowList> transferBorrowList = transferBiz.commonHandel(transfers);
                for (VoViewBorrowList viewBorrowList : transferBorrowList) {
                    borrowListList.add(viewBorrowList);
                }
            }
        }
        Long count = 10L;
        //如果当前没有可投
        Integer tempCount = 10;
        Boolean isLeeZoer = false;  //是否小于10条
        //是第一页并且是 ”全部列表“
        if (voBorrowListReq.getPageIndex().intValue() == 0 && StringUtils.isEmpty(type)) {
            List<VoViewBorrowList> otherAddListBorrows = Lists.newArrayList();
            if (CollectionUtils.isEmpty(borrowListList)) {
                otherAddListBorrows = commonHandle(otherAdd(tempCount), voBorrowListReq);
                isLeeZoer = true;
            } else if (borrowListList.size() < 10) { //小于10条
                otherAddListBorrows = commonHandle(otherAdd(tempCount - borrowListList.size()), voBorrowListReq);
                isLeeZoer = true;
            }
            if (!CollectionUtils.isEmpty(otherAddListBorrows)) {
                for (VoViewBorrowList tempVo : otherAddListBorrows) {
                    borrowListList.add(tempVo);
                }
            }
        }
        //总记录数
        Query countQuery = entityManager.createQuery(countSb.append(condtionSql).toString(), Long.class);
        if (StringUtils.isEmpty(type)) {
            if (isLeeZoer && voBorrowListReq.getPageIndex() == 0) { //是第一页 并且小于10条
                count = 10L;
            } else {
                countQuery.setParameter("statusArray", BorrowContants.BIDDING);
                countQuery.setParameter("type", BorrowContants.JING_ZHI);
                count = (Long) countQuery.getSingleResult();
            }
        } else {
            countQuery.setParameter("statusArray", statusArray);
            count = (Long) countQuery.getSingleResult();
        }
        warpRes.setBorrowLists(borrowListList);
        warpRes.setPageIndex(voBorrowListReq.getPageIndex() + 1);
        warpRes.setPageSize(voBorrowListReq.getPageSize());
        warpRes.setTotalCount(count.intValue());
        return warpRes;

    }

    private List<Borrow> otherAdd(Integer pageSize) {
        StringBuilder contionStr = new StringBuilder(" SELECT b FROM Borrow b\n " +
                "WHERE\n" +
                "b.productId IS NOT NULL\n" +
                "AND\n" +
                "isFinance = 0\n" +
                "AND\n" +
                "b.closeAt is null\n" +
                "AND\n" +
                "b.status=:status\n" +
                "AND\n" +
                "b.type in (:typeArrays)\n" +
                "AND\n" +
                "b.recheckAt IS Not NULL\n" +
                "ORDER BY  FIELD(b.type, 0, 4), " +
                "b.status ASC," +
                "b.lendRepayStatus ASC, " +
                "(b.moneyYes / b.money) DESC, " +
                "b.id desc");
        Query query = entityManager.createQuery(contionStr.toString(), Borrow.class);
        query.setParameter("status", BorrowContants.PASS);
        query.setParameter("typeArrays", Lists.newArrayList(BorrowContants.CE_DAI, BorrowContants.INDEX_TYPE_QU_DAO));
        query.setFirstResult(0);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    /**
     * 首页标列表
     *
     * @return
     */
    @Override
    public List<VoViewBorrowList> pcIndexBorrowList() {
        //公共sql
        List<Integer> typeArray = Lists.newArrayList(BorrowContants.CE_DAI,
                BorrowContants.JING_ZHI,
                BorrowContants.QU_DAO);
        Map<Integer, String> sqlMap = Maps.newHashMap();
        for (Integer type : typeArray) {
            String sql = " SELECT b.* FROM gfb_borrow  b  " +
                    "WHERE " +
                    "b.product_id IS NOT NULL " +
                    "AND " +
                    "b.type =" + type +
                    " AND " +
                    "b.status " +
                    "NOT IN(" + BorrowContants.CANCEL + "," +
                    BorrowContants.NO_PASS + "," +
                    BorrowContants.RECHECK_NO_PASS + "," +
                    BorrowContants.PENDING + ") " +
                    "AND " +
                    " b.is_finance = 0 " +
                    "AND " +
                    "b.verify_at IS Not NULL " +
                    "AND " +
                    "b.close_at is null " +
                    "AND " +
                    "b.product_id IS NOT NULL";
            sqlMap.put(type, sql);
        }
        //车贷排序
        StringBuffer cheDaiOrderBy = new StringBuffer(" ORDER BY " +
                "b.status ASC, " +
                "b.lend_repay_status ASC, " +
                "( b.money_yes / b.money ) DESC, " +
                "b.success_at DESC, " +
                "b.id DESC limit 4");
        //其他标排序
        StringBuffer otherOrderBy = new StringBuffer(" ORDER BY " +
                "b.lend_repay_status ASC, " +
                "( b.money_yes / b.money ) DESC, " +
                "b.status, " +
                "b.success_at DESC, " +
                "b.id DESC limit 2 ");
        //拼接
        String sqlStr = "";
        for (Integer key : sqlMap.keySet()
                ) {
            if (key.intValue() == BorrowContants.CE_DAI)
                sqlStr += "(" + sqlMap.get(key) + cheDaiOrderBy + ") UNION ALL ";
            else
                sqlStr += "(" + sqlMap.get(key) + otherOrderBy + ") UNION ALL ";
        }

        //调用查询
        String sql = sqlStr.substring(0, sqlStr.lastIndexOf("UNION ALL "));
        Query query = entityManager.createNativeQuery(sql, Borrow.class);
        List<Borrow> borrows = query.getResultList();
        /*查询债转标*/
        Pageable pageable = new PageRequest(0,
                2);
        Page<Transfer> transferPage = transferRepository.findByStateIsOrStateIsAndAprThanLee(pageable);
        List<VoViewBorrowList> transferViewList = transferBiz.commonHandel(transferPage.getContent());
        //装配处理
        List<VoViewBorrowList> borrowLists = commonHandle(borrows, new VoBorrowListReq());
        borrowLists.addAll(transferViewList);
        return borrowLists;
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
                attachment.setFilepath(p.getFilepath());
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
        borrowMap.put("successAt", StringUtils.isEmpty(borrow.getRecheckAt()) ? null : DateHelper.dateToString(borrow.getRecheckAt()));
        borrowMap.put("endAt", DateHelper.dateToString(DateHelper.addDays(borrow.getReleaseAt(), borrow.getValidDay())));


        if (!ObjectUtils.isEmpty(borrow.getRecheckAt())) { //判断是否满标
            boolean successAtBool = DateHelper.getMonth(DateHelper.addMonths(borrow.getRecheckAt(), borrow.getTimeLimit())) % 12
                    !=
                    (DateHelper.getMonth(borrow.getRecheckAt()) + borrow.getTimeLimit()) % 12;

            String borrowExpireAtStr = null;
            String monthAsReimbursement = null;//月截止还款日
            if (borrow.getRepayFashion() == 1) {
                borrowExpireAtStr = DateHelper.dateToString(DateHelper.addDays(borrow.getRecheckAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
                monthAsReimbursement = borrowExpireAtStr;
            } else {
                monthAsReimbursement = "每月" + DateHelper.getDay(borrow.getRecheckAt()) + "日";

                if (successAtBool) {
                    borrowExpireAtStr = DateHelper.dateToString(DateHelper.subDays(DateHelper.addDays(DateHelper.setDays(borrow.getRecheckAt(), borrow.getTimeLimit()), 1), 1), "yyyy-MM-dd HH:mm:ss");
                } else {
                    borrowExpireAtStr = DateHelper.dateToString(DateHelper.addMonths(borrow.getRecheckAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
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
            Specification<Tender> specification = null;
            if (!borrowUserId.equals(userId)) {  //当前用户是否 发标用户
                if (borrow.getUserId().intValue() == userId.intValue()) {
                    //发标用户 可以查看所有的的投资信息
                    specification = Specifications.<Tender>and()
                            .eq("borrowId", borrowId)
                            .build();
                    borrowTenderList = tenderRepository.findAll(specification);
                } else {
                    //不是访客 查询当前用户是否是投资用户
                    specification = Specifications.<Tender>and()
                            .eq("userId", userId)
                            .eq("borrowId", borrowId)
                            .build();
                    borrowTenderList = tenderRepository.findAll(specification);
                }
            }
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
                calculatorMap.put("earnings", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("earnings").toString()), 100D)));
                calculatorMap.put("eachRepay", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("eachRepay").toString()), 100D)));
                calculatorMap.put("repayTotal", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("repayTotal").toString()), 100D)));
                calculatorMap.put("repayDetailList", calculatorMap.get("repayDetailList"));
                tempTenderMap.put("calculatorMap", calculatorMap);
                tempTenderMap.put("validMoney", StringHelper.formatMon(MoneyHelper.divide(Double.valueOf(tempTenderMap.get("validMoney").toString()), 100D)));

            }
        } else {
            calculatorMap.put("earnings", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("earnings").toString()), 100D)));
            calculatorMap.put("eachRepay", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("eachRepay").toString()), 100D)));
            calculatorMap.put("repayTotal", StringHelper.formatMon(MoneyHelper.divide(Double.parseDouble(calculatorMap.get("repayTotal").toString()), 100D)));
        }

        //使用thymeleaf模版引擎渲染 借款合同html
        Map<String, Object> templateMap = new HashMap<>();
        templateMap.put("borrowMap", borrowMap);
        templateMap.put("tenderMapList", tenderMapList);
        templateMap.put("calculatorMap", calculatorMap);
        return templateMap;
    }

    @Autowired
    private TransferService transferService;

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
                "b.status=:status " +
                "AND " +
                "b.successAt is null ");
        TypedQuery query1 = entityManager.createQuery(sql + "", Borrow.class);
        query1.setParameter("status", BorrowContants.BIDDING);
        BorrowStatistics borrowStatistics = new BorrowStatistics();
        Integer cheDai = 0;
        Integer jingZhi = 0;
        Integer quDao = 0;
        Integer miaoBiao = 0;
        Integer sum1 = 0;
        List<Borrow> borrowList = query1.getResultList();
        Date nowDate = new Date();
        if (!CollectionUtils.isEmpty(borrowList)) {
            sum1 = borrowList.size();
            Map<Integer, List<Borrow>> borrowMaps = borrowList.stream().collect(groupingBy(Borrow::getType));
            borrowList = borrowMaps.get(BorrowContants.CE_DAI);
            cheDai = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.JING_ZHI);
            jingZhi = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.QU_DAO);
            quDao = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
            borrowList = borrowMaps.get(BorrowContants.MIAO_BIAO);
            miaoBiao = CollectionUtils.isEmpty(borrowList) ? 0 : borrowList.size();
        }

        Specification<Transfer> specification = Specifications.<Transfer>and()
                .eq("state", TransferContants.TRANSFERIND)
                .eq("type", TransferContants.GENERAL)
                .eq("successAt", null)
                .build();
        Integer liuZhuanCount = transferService.findList(specification).size();
        borrowStatistics.setJingZhi(jingZhi);
        borrowStatistics.setCheDai(cheDai);
        borrowStatistics.setMiaoBiao(miaoBiao);
        borrowStatistics.setQuDao(quDao);
        borrowStatistics.setLiuZhuan(liuZhuanCount);
        borrowStatistics.setSum(quDao + cheDai + liuZhuanCount);
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
        long borrowMoney = 0;
        if (!StringUtils.isEmpty(borrow.getRecheckAt())) {
            borrowMoney = borrow.getMoneyYes();
        } else {
            borrowMoney = borrow.getMoney();
        }
        String repaymentAt;
        if (borrow.getRepayFashion() == 1) {
            repaymentAt = DateHelper.dateToString(DateHelper.addDays(borrow.getReleaseAt(), borrow.getTimeLimit()), "yyyy-MM-dd");
        } else {
            repaymentAt = "每月" + DateHelper.getDay(borrow.getReleaseAt()) + "日";
        }
        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(new Double(borrowMoney),
                new Double(borrow.getApr()),
                borrow.getTimeLimit(),
                borrow.getRecheckAt());
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
                BorrowCalculatorHelper userCalculator = new BorrowCalculatorHelper(new Double(p.getValidMoney()), new Double(borrow.getApr()), borrow.getTimeLimit(), borrow.getRecheckAt());
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

    public Borrow insert(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    public boolean updateById(Borrow borrow) {
        if (ObjectUtils.isEmpty(borrow) || ObjectUtils.isEmpty(borrow.getId())) {
            return false;
        }
        return !ObjectUtils.isEmpty(borrowRepository.save(borrow));
    }

    public Borrow save(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    public List<Borrow> save(List<Borrow> borrowList) {
        return borrowRepository.save(borrowList);
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

    public Borrow getBorrowByProductId(String productId) {
        return borrowRepository.findByProductId(productId);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED)
    public Borrow flushSave(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    @Override
    public Borrow findNoviceBorrow() {
        try {
            return newBorrow.get("new");
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public List<Borrow> findByBorrowIds(List<Long> ids) {
        return borrowRepository.findByIdIn(ids);
    }
}
