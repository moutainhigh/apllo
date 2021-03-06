package com.gofobao.framework.tender.service.impl;

import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.helper.BeanHelper;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.NumberHelper;
import com.gofobao.framework.tender.entity.AutoTender;
import com.gofobao.framework.tender.repository.AutoTenderRepository;
import com.gofobao.framework.tender.service.AutoTenderService;
import com.gofobao.framework.tender.vo.VoFindAutoTenderList;
import com.gofobao.framework.tender.vo.response.UserAutoTender;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;

/**
 * Created by Zeke on 2017/5/27.
 */
@Service
@Slf4j
public class AutoTenderServiceImpl implements AutoTenderService {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private AutoTenderRepository autoTenderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    public boolean insert(AutoTender autoTender) {
        if (ObjectUtils.isEmpty(autoTender)) {
            return false;
        }
        autoTender.setId(null);
        return !ObjectUtils.isEmpty(autoTenderRepository.save(autoTender));
    }

    public AutoTender save(AutoTender autoTender) {
        return autoTenderRepository.save(autoTender);
    }

    public boolean updateById(AutoTender autoTender) {
        if (ObjectUtils.isEmpty(autoTender) || ObjectUtils.isEmpty(autoTender.getId())) {
            return false;
        }
        return !ObjectUtils.isEmpty(autoTenderRepository.save(autoTender));
    }

    public boolean updateByExample(AutoTender autoTender, Example<AutoTender> example) {
        if (ObjectUtils.isEmpty(autoTender) || ObjectUtils.isEmpty(example.getProbe())) {
            return false;
        }

        List<AutoTender> autoTenderList = autoTenderRepository.findAll(example);
        List<AutoTender> updAutoTenders = new ArrayList<>();

        Optional<List<AutoTender>> autoTenderOptions = Optional.ofNullable(autoTenderList);
        autoTenderOptions.ifPresent(o -> o.forEach(temp -> {
            BeanHelper.copyParamter(autoTender, temp, true);
            updAutoTenders.add(temp);//更新对象
        }));
        autoTenderRepository.save(updAutoTenders);
        return true;
    }


    public AutoTender findById(long id){
        return autoTenderRepository.findOne(id);
    }

    public List<Map<String, Object>> findQualifiedAutoTenders(VoFindAutoTenderList voFindAutoTenderList) {
        Long borrowId = voFindAutoTenderList.getBorrowId();
        if (ObjectUtils.isEmpty(borrowId)) {
            return Collections.EMPTY_LIST;
        }

        Borrow borrow = borrowService.findById(voFindAutoTenderList.getBorrowId());

        StringBuffer sql = new StringBuffer("select t.id AS id,t. STATUS AS status,t.user_id AS userId,t.lowest AS lowest,t.borrow_types AS borrowTypes," +
                "t.repay_fashions AS repayFashions,t.tender_0 AS tender0,t.tender_1 AS tender1,t.tender_3 AS tender3,t.tender_4 AS tender4,t.`mode` AS mode,t.tender_money AS tenderMoney,t.timelimit_first AS timelimitFirst,t.timelimit_last AS timelimitLast,t.timelimit_type AS timelimitType,t.apr_first AS aprFirst,t.apr_last AS aprLast,t.save_money AS saveMoney,t.`order` AS `order`,t.auto_at AS autoAt,t.created_at AS createdAt," +
                "t.updated_at AS updatedAt,a.use_money AS useMoney,a.no_use_money AS noUseMoney,a.virtual_money AS virtualMoney,a.collection AS collection,a.payment AS payment " +
                "FROM\n" +
                "  gfb_auto_tender t\n" +
                "  LEFT JOIN gfb_asset a ON t.user_id = a.user_id\n" +
                "\tLEFT JOIN gfb_users u on t.user_id = u.id\n" +
                "  LEFT JOIN gfb_user_third_account uta ON t.user_id = uta.user_id\n" +
                "WHERE\n" +
                "  1 = 1\n" +
                "  AND uta.del = 0\n" +
                "\tAND u.is_lock = '0'\n");

        sql.append(String.format(" AND t.lowest <= %s ", borrow.getMoney()));
        sql.append(String.format(" AND a.use_money >= %s", borrow.getLowest()));

        Integer type = !ObjectUtils.isEmpty(borrow.getTenderId()) ? 3 : borrow.getType();
        sql.append(" and t.tender_" + type + " = 1");

        String status = voFindAutoTenderList.getStatus();
        if (!StringUtils.isEmpty(status)) {
            sql.append(" and t.status = ").append(status);
        }
        Long userId = voFindAutoTenderList.getUserId();
        if (!StringUtils.isEmpty(userId)) {
            sql.append(" and t.user_id = ").append(userId);
        }
        Long notUserId = voFindAutoTenderList.getNotUserId();
        if (!StringUtils.isEmpty(notUserId)) {
            sql.append(" and t.user_id <> ").append(notUserId);
        }
        String inRepayFashions = voFindAutoTenderList.getInRepayFashions();
        if (!StringUtils.isEmpty(inRepayFashions)) {
            sql.append(" and t.repay_fashions in (").append(inRepayFashions).append(")");
        }
        Integer ltAprFirst = voFindAutoTenderList.getLtAprFirst();
        if (!StringUtils.isEmpty(ltAprFirst)) {
            sql.append(" and t.apr_first <= ").append(ltAprFirst);
        }
        Integer gtAprLast = voFindAutoTenderList.getGtAprLast();
        if (!StringUtils.isEmpty(gtAprLast)) {
            sql.append(" and  t.apr_last >= ").append(gtAprLast);
        }
        sql.append(" and (t.timelimit_type = 0 or ");
        sql.append(" (t.timelimit_type = " + (borrow.getRepayFashion() == 1 ? 2 : 1));
        sql.append(" and t.timelimit_first <= " + borrow.getTimeLimit());
        sql.append(" and t.timelimit_last >= " + borrow.getTimeLimit() + " ))");
        //排序
        sql.append(" order by t.`order`");
        //分页
        Integer pageIndex = voFindAutoTenderList.getPageIndex();
        Integer pageSize = voFindAutoTenderList.getPageSize();
        sql.append(" limit ").append(pageIndex * pageSize).append(",").append(pageSize);
        log.info(sql.toString());
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql.toString());
        if (CollectionUtils.isEmpty(resultList)) {
            return new ArrayList<>(0);
        } else {
            return resultList;
        }
    }


    /**
     * 更新自动投标order
     *
     * @return
     */
    public boolean updateAutoTenderOrder() {
        StringBuffer sql = new StringBuffer("UPDATE gfb_auto_tender t1,(SELECT id,@rownum:=@rownum+1 AS listorder FROM" +
                " gfb_auto_tender t2, (SELECT @rownum:=0)t3  ORDER BY t2.auto_at ASC, t2.order ASC ) t4  SET t1.`order` = t4.listorder WHERE t1.id = t4.id");
        return jdbcTemplate.update(sql.toString()) > 1;
    }

    /**
     * 获取自动投标序号
     *
     * @return
     */
    public int getOrderNum() {
        StringBuffer sql = new StringBuffer("select MAX(`order`) FROM gfb_auto_tender");
        Query query = entityManager.createNativeQuery(sql.toString());
        return NumberHelper.toInt(query.getResultList().get(0));
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public List<UserAutoTender> list(Long userId) {
        List<AutoTender> autoTenders = autoTenderRepository.findByUserId(userId);
        List<UserAutoTender> userAutoTenders = Lists.newArrayList();
        autoTenders.stream().forEach(p -> {
            UserAutoTender userAutoTender = new UserAutoTender();
            userAutoTender.setId(p.getId());
            userAutoTender.setStatus(p.getStatus());
            userAutoTender.setOrder(p.getOrder());
            userAutoTender.setDays(DateHelper.diffInDays(new Date(), p.getCreatedAt(), true));
            userAutoTenders.add(userAutoTender);
        });
        return Optional.ofNullable(userAutoTenders).orElse(Collections.EMPTY_LIST);
    }

    public List<AutoTender> findList(Specification<AutoTender> specification) {
        return autoTenderRepository.findAll(specification);
    }

    public List<AutoTender> findList(Specification<AutoTender> specification, Sort sort) {
        return autoTenderRepository.findAll(specification, sort);
    }

    public List<AutoTender> findList(Specification<AutoTender> specification, Pageable pageable) {
        return autoTenderRepository.findAll(specification, pageable).getContent();
    }

    public long count(Specification<AutoTender> specification) {
        return autoTenderRepository.count(specification);
    }

    public void delete(long id) {
        autoTenderRepository.delete(id);
    }

    public void delete(AutoTender autoTender) {
        autoTenderRepository.delete(autoTender);
    }

    public void delete(List<AutoTender> autoTenderList) {
        autoTenderRepository.delete(autoTenderList);
    }
}
