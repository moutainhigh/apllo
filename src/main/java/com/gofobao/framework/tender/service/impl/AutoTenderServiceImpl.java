package com.gofobao.framework.tender.service.impl;

import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.service.BorrowService;
import com.gofobao.framework.helper.BeanHelper;
import com.gofobao.framework.tender.entity.AutoTender;
import com.gofobao.framework.tender.repository.AutoTenderRepository;
import com.gofobao.framework.tender.service.AutoTenderService;
import com.gofobao.framework.tender.vo.VoFindAutoTenderList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Zeke on 2017/5/27.
 */
@Service
public class AutoTenderServiceImpl implements AutoTenderService {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private AutoTenderRepository autoTenderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public boolean insert(AutoTender autoTender) {
        if (ObjectUtils.isEmpty(autoTender)) {
            return false;
        }
        autoTender.setId(null);
        return !ObjectUtils.isEmpty(autoTenderRepository.save(autoTender));
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
        return CollectionUtils.isEmpty(autoTenderRepository.save(updAutoTenders));
    }

    public List<Map<String, Object>> findQualifiedAutoTenders(VoFindAutoTenderList voFindAutoTenderList) {
        Long borrowId = voFindAutoTenderList.getBorrowId();
        if (ObjectUtils.isEmpty(borrowId)) {
            return null;
        }
        Borrow borrow = borrowService.findById(voFindAutoTenderList.getBorrowId());

        StringBuffer sql = new StringBuffer("select t.id AS id,t. STATUS AS status,t.user_id AS userId,t.lowest AS lowest,t.borrow_types AS borrowTypes," +
                "t.repay_fashions AS repayFashions,t.tender_0 AS tender0,t.tender_1 AS tender1,t.tender_3 AS tender3,t.tender_4 AS tender4,t.`mode` AS mode,t.tender_money AS tenderMoney,t.timelimit_first AS timelimitFirst,t.timelimit_last AS timelimitLast,t.timelimit_type AS timelimitType,t.apr_first AS aprFirst,t.apr_last AS aprLast,t.save_money AS saveMoney,t.`order` AS `order`,t.auto_at AS autoAt,t.created_at AS createdAt," +
                "t.updated_at AS updatedAt,a.use_money AS useMoney,a.no_use_money AS noUseMoney,a.virtual_money AS virtualMoney,a.collection AS collection,a.payment AS payment " +
                "from gfb_auto_tender t left join gfb_asset a on t.user_id = a.user_id where 1=1 ");
        String status = voFindAutoTenderList.getStatus();
        if (!StringUtils.isEmpty(status)) {
            sql.append("and t.status = ").append(status);
        }
        String userId = voFindAutoTenderList.getUserId();
        if (!StringUtils.isEmpty(userId)) {
            sql.append("and t.user_id = ").append(userId);
        }
        String notUserId = voFindAutoTenderList.getNotUserId();
        if (!StringUtils.isEmpty(notUserId)) {
            sql.append("and t.user_id <> ").append(notUserId);
        }
        String inRepayFashions = voFindAutoTenderList.getInRepayFashions();
        if (!StringUtils.isEmpty(inRepayFashions)) {
            sql.append("and t.repay_fashions in (").append(inRepayFashions).append(")");
        }
        String tender0 = voFindAutoTenderList.getTender0();
        if (!StringUtils.isEmpty(tender0)) {
            sql.append("and t.tender_0 = ").append(tender0);
        }
        String tender1 = voFindAutoTenderList.getTender1();
        if (!StringUtils.isEmpty(tender1)) {
            sql.append("and t.tender_1 = ").append(tender1);
        }
        String tender3 = voFindAutoTenderList.getTender3();
        if (!StringUtils.isEmpty(tender3)) {
            sql.append("and t.tender_3 = ").append(tender3);
        }
        String tender4 = voFindAutoTenderList.getTender4();
        if (!StringUtils.isEmpty(tender4)) {
            sql.append("and t.tender_4 = ").append(tender4);
        }
        String timelimitType = voFindAutoTenderList.getTimelimitType();
        if (!StringUtils.isEmpty(timelimitType)) {
            sql.append("and t.timelimit_type = ").append(timelimitType);
        }
        String gtTimelimitLast = voFindAutoTenderList.getGtTimelimitLast();
        if (!StringUtils.isEmpty(gtTimelimitLast)) {
            sql.append("and t.timelimit_last >= ").append(gtTimelimitLast);
        }
        String ltTimelimitFirst = voFindAutoTenderList.getLtTimelimitFirst();
        if (!StringUtils.isEmpty(ltTimelimitFirst)) {
            sql.append("and  t.timelimit_first <= ").append(ltTimelimitFirst);
        }
        String ltAprFirst = voFindAutoTenderList.getLtAprFirst();
        if (!StringUtils.isEmpty(ltAprFirst)) {
            sql.append("and t.apr_first <= ").append(ltAprFirst);
        }
        String gtAprLast = voFindAutoTenderList.getGtAprLast();
        if (!StringUtils.isEmpty(gtAprLast)) {
            sql.append("and  t.apr_last >= ").append(gtAprLast);
        }
        sql.append("and (t.timelimit_type = 0 or ");
        sql.append("(t.timelimit_type = " + (borrow.getRepayFashion() == 1 ? 2 : 1));
        sql.append(" and t.timelimit_first <= " + borrow.getTimeLimit());
        sql.append(" and t.timelimit_last >= " + borrow.getTimeLimit() + " ))");
        //排序
        sql.append(" order by t.`order`");
        //分页
        Integer pageIndex = voFindAutoTenderList.getPageIndex();
        Integer pageSize = voFindAutoTenderList.getPageSize();
        sql.append(" limit ").append(pageIndex * pageSize).append(",").append(pageSize);
        Query query = entityManager.createNativeQuery(sql.toString(), Map.class);

        return query.getResultList();
    }

    public boolean updateAutoTenderOrder() {
        StringBuffer sql = new StringBuffer("UPDATE gfb_auto_tender t1, ( SELECT id, @rownum := @rownum + 1 AS listorder FROM" +
                " gfb_auto_tender t2, ( SELECT @rownum :=0 )t3  ORDER BY t2.auto_at ASC, t2.order ASC ) t4  SET t1.`order` = t4.listorder WHERE t1.id = t4.id");
        Query query = entityManager.createNativeQuery(sql.toString());
        return query.executeUpdate() > 0;
    }
}
