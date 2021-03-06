package com.gofobao.framework.award.service.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.asset.contants.AssetTypeContants;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.entity.AssetLog;
import com.gofobao.framework.asset.repository.AssetLogRepository;
import com.gofobao.framework.asset.repository.AssetRepository;
import com.gofobao.framework.award.contants.CouponContants;
import com.gofobao.framework.award.contants.RedPacketContants;
import com.gofobao.framework.award.entity.Coupon;
import com.gofobao.framework.award.repository.CouponRepository;
import com.gofobao.framework.award.repository.VirtualBorrowRepository;
import com.gofobao.framework.award.repository.VirtualCollectionRepository;
import com.gofobao.framework.award.repository.VirtualTenderRepository;
import com.gofobao.framework.award.service.VirtualService;
import com.gofobao.framework.award.vo.request.VoVirtualReq;
import com.gofobao.framework.award.vo.response.AwardStatistics;
import com.gofobao.framework.award.vo.response.VirtualBorrowRes;
import com.gofobao.framework.award.vo.response.VirtualStatistics;
import com.gofobao.framework.award.vo.response.VirtualTenderRes;
import com.gofobao.framework.borrow.contants.BorrowVirtualContants;
import com.gofobao.framework.borrow.entity.BorrowVirtual;
import com.gofobao.framework.collection.entity.VirtualCollection;
import com.gofobao.framework.common.assets.AssetChange;
import com.gofobao.framework.common.assets.AssetChangeProvider;
import com.gofobao.framework.common.assets.AssetChangeTypeEnum;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.NumberHelper;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.helper.project.BorrowCalculatorHelper;
import com.gofobao.framework.marketing.entity.MarketingRedpackRecord;
import com.gofobao.framework.marketing.repository.MarketingRedpackRecordRepository;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.tender.contants.VirtualTenderContants;
import com.gofobao.framework.tender.entity.VirtualTender;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by admin on 2017/6/8.
 */


@Slf4j
@Component
public class VirtualServiceImpl implements VirtualService {


    @Autowired
    private AssetLogRepository assetLogRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private VirtualBorrowRepository virtualBorrowRepository;

    @Autowired
    private VirtualTenderRepository virtualTenderRepository;

    @Autowired
    private VirtualCollectionRepository virtualCollectionRepository;


    @Autowired
    private MarketingRedpackRecordRepository marketingRedpackRecordRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserThirdAccountService userThirdAccountService;

    @Autowired
    AssetChangeProvider assetChangeProvider;

    /**
     * 体验金统计
     *
     * @param userId
     * @return
     */
    @Override
    public VirtualStatistics statistics(Long userId) {
        VirtualStatistics virtualStatistics = new VirtualStatistics();
        Asset asset = assetRepository.findOne(userId);
        if (ObjectUtils.isEmpty(asset)) {
            return new VirtualStatistics();
        }
        List<AssetLog> assetLogs = assetLogRepository.findByUserIdAndTypeIs(userId, AssetTypeContants.VIRTUAL_TENDER);
        //可用
        virtualStatistics.setAvailable(asset.getVirtualMoney() / 100D);
        //已用

        virtualStatistics.setUsed(assetLogs.stream().mapToLong(m -> m.getMoney()).sum() / 100d);

        List<VirtualTender> virtualTenders = virtualTenderRepository.findByUserIdAndStatusIs(userId, VirtualTenderContants.VIRTUALTENDERSUCCESS);
        if (CollectionUtils.isEmpty(virtualTenders)) {
            virtualStatistics.setEarnings(0d);
            return virtualStatistics;
        }
        List<Long> tenderIdArray = virtualTenders.stream().map(p -> p.getId()).collect(Collectors.toList());
        List<VirtualCollection> virtualCollections = virtualCollectionRepository.findByTenderIdInAndStatusIs(tenderIdArray, VirtualTenderContants.VIRTUALTENDERSUCCESS);
        if (CollectionUtils.isEmpty(virtualCollections)) {
            virtualStatistics.setEarnings(0d);
            return virtualStatistics;
        }
        Integer collectionMoney = virtualCollections.stream().mapToInt(p -> p.getCollectionMoney()).sum();
        virtualStatistics.setEarnings(collectionMoney / 100d);
        return virtualStatistics;
    }

    /**
     * 用户投资体验金列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<VirtualTenderRes> userTenderList(Long userId) {
        List<AssetLog> assetLogs = assetLogRepository.findByUserIdAndTypeIs(userId, AssetTypeContants.VIRTUAL_TENDER);
        if (CollectionUtils.isEmpty(assetLogs)) {
            return Collections.EMPTY_LIST;
        }

        List<VirtualTenderRes> virtualTenderRes = Lists.newArrayList();
        assetLogs.stream().forEach(p -> {
            VirtualTenderRes item = new VirtualTenderRes();
            item.setMoney(StringHelper.toString(p.getMoney() / 100d));
            item.setRemark(p.getRemark());
            item.setTime(DateHelper.dateToString(p.getCreatedAt()));
            virtualTenderRes.add(item);
        });
        return Optional.ofNullable(virtualTenderRes).orElse(Collections.EMPTY_LIST);
    }

    @Override
    public List<VirtualBorrowRes> list() {
        List<BorrowVirtual> virtualList = virtualBorrowRepository.findByStatus(BorrowVirtualContants.STATUS_YES);
        if (CollectionUtils.isEmpty(virtualList)) {
            return Collections.EMPTY_LIST;
        }
        List<VirtualBorrowRes> virtualBorrowRes = Lists.newArrayList();
        virtualList.stream().forEach(p -> {
            VirtualBorrowRes borrowRes = new VirtualBorrowRes();
            borrowRes.setId(p.getId());
            borrowRes.setMoney(StringHelper.toString(p.getMost() / 100));
            borrowRes.setTimeLimit(p.getTimeLimit());
            borrowRes.setApr(StringHelper.toString(p.getApr() / 100));
            borrowRes.setName(p.getName());
            borrowRes.setRepayFashion("一次性还本付息");
            borrowRes.setTime(DateHelper.dateToString(p.getCreatedAt()));
            virtualBorrowRes.add(borrowRes);
        });
        return Optional.ofNullable(virtualBorrowRes).orElse(Collections.EMPTY_LIST);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public Boolean tenderCreate(VoVirtualReq voVirtualReq) throws Exception {

        //当前用户是否开通存款
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(voVirtualReq.getUserId());
        if (ObjectUtils.isEmpty(userThirdAccount)) {
            return false;
        }
        //体验标是否存在
        BorrowVirtual borrowVirtual = virtualBorrowRepository.findOne(voVirtualReq.getId());
        if (ObjectUtils.isEmpty(borrowVirtual)) {
            return false;
        }
        //当前有用户是否有可用体验金
        Specification specification = Specifications.<Asset>and()
                .eq("userId", voVirtualReq.getUserId())
                .ge("virtualMoney", borrowVirtual.getLowest())
                .build();
        Asset asset = assetRepository.findOne(specification);
        if (ObjectUtils.isEmpty(asset)) {
            return false;
        }
        Date date = new Date();
        VirtualTender virtualTender = new VirtualTender();
        virtualTender.setUserId(asset.getUserId());
        virtualTender.setStatus(VirtualTenderContants.VIRTUALTENDERSUCCESS);
        virtualTender.setBorrowId(voVirtualReq.getId());
        virtualTender.setCreatedAt(date);
        virtualTender.setUpdatedAt(date);
        virtualTender.setMoney(asset.getVirtualMoney());
        virtualTender = virtualTenderRepository.save(virtualTender);

        BorrowCalculatorHelper borrowCalculatorHelper = new BorrowCalculatorHelper(virtualTender.getMoney().doubleValue(), borrowVirtual.getApr().doubleValue(), borrowVirtual.getTimeLimit(), new Date());
        Map<String, Object> resultMap = borrowCalculatorHelper.ycxhbfx();

        //还款期数
        VirtualCollection virtualCollection = new VirtualCollection();
        virtualCollection.setStatus(BorrowVirtualContants.STATUS_NO);
        virtualCollection.setOrder(1);
        virtualCollection.setTenderId(virtualTender.getId());
        List objectMap = (ArrayList) resultMap.get("repayDetailList");
        Map<String, String> repayMaps = (Map<String, String>) objectMap.get(0);
        Integer collectionMoney = NumberHelper.toInt(repayMaps.get("repayMoney"));//应收本息
        Integer interest = NumberHelper.toInt(repayMaps.get("interest"));//应收利息
        Integer principal = NumberHelper.toInt(repayMaps.get("principal"));//应收本金
        Date collectionAt = DateHelper.stringToDate(repayMaps.get("repayAt"));
        virtualCollection.setCollectionMoney(collectionMoney);
        virtualCollection.setInterest(interest);
        virtualCollection.setPrincipal(principal);
        virtualCollection.setCollectionAt(collectionAt);
        virtualCollection.setCollectionAtYes(collectionAt);
        virtualCollection.setCollectionMoneyYes(0);
        virtualCollection.setUpdatedAt(date);
        virtualCollection.setCreatedAt(date);
        virtualCollectionRepository.save(virtualCollection);

        AssetChange assetChange = new AssetChange();
        assetChange.setType(AssetChangeTypeEnum.virtualTender);
        assetChange.setMoney(asset.getVirtualMoney());
        assetChange.setSourceId(virtualTender.getId());
        assetChange.setSeqNo(assetChangeProvider.getSeqNo());
        assetChange.setGroupSeqNo(assetChangeProvider.getGroupSeqNo());
        assetChange.setUserId(asset.getUserId());
        assetChange.setRemark("投资体验标扣除体验金");
        assetChangeProvider.commonAssetChange(assetChange);

        return true;
    }

    @Override
    public AwardStatistics query(Long userId) {
        AwardStatistics awardStatistics = new AwardStatistics();
        Specification<MarketingRedpackRecord> redPackageSpec = Specifications.<MarketingRedpackRecord>and()
                .eq("userId", userId)
                .eq("state", RedPacketContants.unUsed)
                .eq("del", 0)
                .build();
        Long redpackSize = marketingRedpackRecordRepository.count(redPackageSpec);
        awardStatistics.setRedPackageCount(redpackSize.intValue());
        Specification specification = Specifications.<Coupon>and()
                .eq("userId", userId)
                .eq("status", CouponContants.VALID)
                .build();

        Long couponSize = couponRepository.count(specification);
        awardStatistics.setCouponCount(couponSize.intValue());
        Asset asset = assetRepository.findOne(userId);
        awardStatistics.setVirtualMoney(asset.getVirtualMoney() / 100d);
        return awardStatistics;
    }

    public VirtualCollection save(VirtualCollection virtualCollection) {
        return virtualCollectionRepository.save(virtualCollection);
    }

    public List<VirtualCollection> save(List<VirtualCollection> virtualCollectionList) {
        return virtualCollectionRepository.save(virtualCollectionList);
    }

    public List<VirtualCollection> findList(Specification<VirtualCollection> specification) {
        return virtualCollectionRepository.findAll(specification);
    }

    public List<VirtualCollection> findList(Specification<VirtualCollection> specification, Sort sort) {
        return virtualCollectionRepository.findAll(specification, sort);
    }

    public List<VirtualCollection> findList(Specification<VirtualCollection> specification, Pageable pageable) {
        return virtualCollectionRepository.findAll(specification, pageable).getContent();
    }

    public long count(Specification<VirtualCollection> specification) {
        return virtualCollectionRepository.count(specification);
    }
}
