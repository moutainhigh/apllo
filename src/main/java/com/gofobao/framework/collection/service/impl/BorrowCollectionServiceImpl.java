package com.gofobao.framework.collection.service.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.borrow.entity.Borrow;
import com.gofobao.framework.borrow.repository.BorrowRepository;
import com.gofobao.framework.collection.vo.request.VoCollectionOrderReq;
import com.gofobao.framework.collection.vo.request.VoOrderDetailReq;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderListRes;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderRes;
import com.gofobao.framework.collection.contants.BorrowCollectionContants;
import com.gofobao.framework.collection.entity.BorrowCollection;
import com.gofobao.framework.collection.repository.BorrowCollectionRepository;
import com.gofobao.framework.collection.service.BorrowCollectionService;
import com.gofobao.framework.collection.vo.response.VoViewOrderDetailRes;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.NumberHelper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by admin on 2017/5/31.
 */
@Service
public class BorrowCollectionServiceImpl implements BorrowCollectionService {

    @Autowired
    private BorrowCollectionRepository borrowCollectionRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    /**
     * 回款列表
     * @param voCollectionOrderReq
     * @return VoViewCollectionOrderListRes
     */
    @Override
    public VoViewCollectionOrderListRes orderList(VoCollectionOrderReq voCollectionOrderReq) {
        Date date = DateHelper.stringToDate(voCollectionOrderReq.getTime());

        Specification<BorrowCollection> specification = Specifications.<BorrowCollection>and()
                .eq("userId", voCollectionOrderReq.getUserId())
                .between("startAt", new Range<>(date, DateHelper.addDays(date, 1)))
                .eq("transferFlag", BorrowCollectionContants.TRANSFER_FLAG_NO)
                .build();
        List<BorrowCollection> borrowCollections = borrowCollectionRepository.findAll(specification);
        if (CollectionUtils.isEmpty(borrowCollections)) {
            return null;
        }
        Set<Integer> borrowIdSet = borrowCollections.stream()
                .map(f -> f.getBorrowId())
                .collect(Collectors.toSet());
        List<Borrow> borrowList = borrowRepository.findByIdIn(Lists.newArrayList(borrowIdSet));
        Map<Long, Borrow> borrowMap = borrowList
                .stream()
                .collect(Collectors.toMap(Borrow::getId, Function.identity()));

        VoViewCollectionOrderListRes voViewCollectionOrderListRes = new VoViewCollectionOrderListRes();

        List<VoViewCollectionOrderRes> orderResList = new ArrayList<>();

        borrowCollections.stream().forEach(p -> {
            VoViewCollectionOrderRes item = new VoViewCollectionOrderRes();
            Borrow borrow = borrowMap.get(p.getBorrowId());
            item.setBorrowName(borrow.getName());
            item.setOrder(p.getOrder() + 1);
            item.setTimeLime(borrow.getTimeLimit());
            item.setCollectionMoney(NumberHelper.to2DigitString(p.getCollectionMoney() / 100));
            item.setCollectionMoneyYes(NumberHelper.to2DigitString(p.getCollectionMoneyYes() / 100));
            orderResList.add(item);
        });
        //回款列表
        voViewCollectionOrderListRes.setOrderResList(orderResList);
        //总回款期数
        voViewCollectionOrderListRes.setOrder(orderResList.size());
        //已回款金额
        Integer sumCollectionMoneyYes = borrowCollections.stream()
                .filter(p -> p.getStatus() == BorrowCollectionContants.STATUS_YES)
                .mapToInt(w -> w.getCollectionMoneyYes())
                .sum();
        voViewCollectionOrderListRes.setSumCollectionMoneyYes(NumberHelper.to2DigitString(sumCollectionMoneyYes / 100));
        Optional<VoViewCollectionOrderListRes> orderListRes = Optional.ofNullable(voViewCollectionOrderListRes);
        return orderListRes.orElseGet(() -> null);
    }

    /**
     * 回款详情
     * @param voOrderDetailReq
     * @return VoViewOrderDetailRes
     */
    @Override
    public VoViewOrderDetailRes orderDetail(VoOrderDetailReq voOrderDetailReq) {
        BorrowCollection borrowCollection = borrowCollectionRepository.findOne(voOrderDetailReq.getCollectionId());
        if (Objects.isNull(borrowCollection)) {
            return null;
        }
        Borrow borrow = borrowRepository.findOne(borrowCollection.getBorrowId().longValue());
        VoViewOrderDetailRes detailRes = new VoViewOrderDetailRes();
        detailRes.setOrder(borrowCollection.getOrder() + 1);
        detailRes.setCollectionMoney(NumberHelper.to2DigitString(borrowCollection.getCollectionMoney() / 100));
        detailRes.setLateDays(borrowCollection.getLateDays());
        detailRes.setStartAt(DateHelper.dateToString(borrowCollection.getStartAtYes()));
        detailRes.setBorrowName(borrow.getName());
        Integer interest = 0;  //利息
        Integer principal = 0;//本金
        if (borrowCollection.getStatus() == 1) {
            interest = borrowCollection.getInterest();
            principal = borrowCollection.getPrincipal();
            detailRes.setStatus(BorrowCollectionContants.STATUS_YES_STR);
        }else{
            detailRes.setStatus(BorrowCollectionContants.STATUS_YES_STR);
        }
        detailRes.setPrincipal(NumberHelper.to2DigitString(interest / 100));
        detailRes.setInterest(NumberHelper.to2DigitString(principal / 100));
        return detailRes;
    }
}
