package com.gofobao.framework.windmill.borrow.service.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.borrow.contants.BorrowContants;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.repository.BorrowRepository;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.tender.contants.TenderConstans;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.repository.TenderRepository;
import com.gofobao.framework.windmill.borrow.service.WindmillBorrowService;
import com.gofobao.framework.windmill.borrow.vo.request.BySomeDayReq;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by admin on 2017/8/1.
 */
@Component
public class WindmillBorrowServiceImpl implements WindmillBorrowService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BorrowRepository borrowRepository;


    @Autowired
    private TenderRepository tenderRepository;

    /**
     * 不传id,查当前可投标的列表。如带id查询时，只返回这个id信息，满标也要返回
     *
     * @param borrowId
     * @return
     */
    @Override
    public List<Borrow> list(Long borrowId) {

        //过滤掉 发标待审 初审不通过；复审不通过 已取消
        List statusArray = Lists.newArrayList(
                new Integer(BorrowContants.CANCEL),
                new Integer(BorrowContants.NO_PASS),
                new Integer(BorrowContants.RECHECK_NO_PASS),
                new Integer(BorrowContants.PENDING),
                new Integer(BorrowContants.PASS));
        //過濾掉秒标,信用标
        List typeArray = Lists.newArrayList(new Integer(BorrowContants.JING_ZHI),
                new Integer(BorrowContants.MIAO_BIAO));
        StringBuilder sql = new StringBuilder("SELECT b FROM Borrow  AS b WHERE 1=1 ");
        //条件
        StringBuilder condition = new StringBuilder("AND b.status NOT IN(:statusArray) AND b.type NOT IN (:typeArray) AND b.closeAt IS NULL AND b.tenderId IS NULL  AND b.isWindmill=:isWindmill  ");
        if (!ObjectUtils.isEmpty(borrowId)) {
            condition.append(" AND  b.id=" + borrowId);
        }
        String orderBy = " ORDER BY  FIELD(b.type, 0, 4, 1, 2), b.status ASC,  b.lendRepayStatus ASC, (b.moneyYes / b.money) DESC,  b.id desc ";
        Query query = entityManager.createQuery(sql.append(condition).append(orderBy).toString(), Borrow.class);

        query.setParameter("statusArray", statusArray);
        query.setParameter("isWindmill", true);
        query.setParameter("typeArray", typeArray);
        List<Borrow> borrowList = query.getResultList();
        Optional<List<Borrow>> result = Optional.empty();
        return result.ofNullable(borrowList).orElse(Collections.emptyList());
    }

    /**
     * 查询某天投资情况
     *
     * @param someDayReq
     * @return
     */
    @Override
    public List<Tender> bySomeDayTenders(BySomeDayReq someDayReq) {
        Date date = DateHelper.stringToDate(someDayReq.getInvest_date(), DateHelper.DATE_FORMAT_YMD);

        Specification<Tender> specification = Specifications.<Tender>and()
                .between("created_at", new Range<>(DateHelper.beginOfDate(date), DateHelper.endOfDate(date)))
                .eq("status", TenderConstans.SUCCESS)
                .build();
        return tenderRepository.findAll(specification, new PageRequest(someDayReq.getLimit(), someDayReq.getLimit())).getContent();
    }

    /**
     * @param borrowId
     * @param date
     * @return
     */
    @Override
    public List<Tender> tenderList(Long borrowId, String date) {
        //获取当前标是否是信用标
        Specification<Borrow> specification = Specifications.<Borrow>and()
                .eq("id", borrowId)
                .eq("type", BorrowContants.JING_ZHI)
                .build();
        Borrow borrow = borrowRepository.findOne(specification);
        if (!ObjectUtils.isEmpty(borrow)) {
            return Lists.newArrayList();
        }
        StringBuilder sql = new StringBuilder("SELECT t FROM Tender t WHERE t.borrowId=:borrowId AND t.status=:status   AND t.createdAt>='" + date + "' ORDER BY t.id ASC");
        Query query = entityManager.createQuery(sql.toString(), Tender.class);
        query.setParameter("status", TenderConstans.SUCCESS);
        query.setParameter("borrowId",borrowId);
        return query.getResultList();
    }
}
