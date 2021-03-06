package com.gofobao.framework.tender.service;

import com.gofobao.framework.borrow.vo.response.VoBorrowTenderUserRes;
import com.gofobao.framework.helper.NumberHelper;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.vo.request.TenderUserReq;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.Query;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/5/19.
 */
public interface TenderService {

    Tender save(Tender tender);

    List<Tender> save(List<Tender> tender);

    boolean updateById(Tender tender);


    List<VoBorrowTenderUserRes> findBorrowTenderUser(TenderUserReq tenderUserReq);

    /**
     * 债转标的原始标的投标记录
     *
     * @param tenderUserReq
     * @return
     */
    List<VoBorrowTenderUserRes> originalBorrowTenderUser(TenderUserReq tenderUserReq);

    List<Tender> findList(Specification<Tender> specification);

    List<Tender> findList(Specification<Tender> specification, Pageable pageable);

    List<Tender> findList(Specification<Tender> specification, Sort sort);

    long count(Specification<Tender> specification);

    /**
     * 检查投标是否太频繁
     *
     * @param borrowId
     * @param userId
     * @return
     */
    boolean checkTenderNimiety(Long borrowId, Long userId);

    Tender findById(Long tenderId);

    Tender findByAuthCode(String authCode);

    /**
     * 查询投标复审金额
     *
     * @param userId
     * @return
     */
    long findTenderAgainVerifyMoney(long userId);

    /**
     * 昨日 今日 成交统计
     *
     * @return
     */
    Map<String, Long> statistic();


}
