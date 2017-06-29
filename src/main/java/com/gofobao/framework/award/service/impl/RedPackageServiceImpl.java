package com.gofobao.framework.award.service.impl;

import com.gofobao.framework.award.contants.RedPacketContants;
import com.gofobao.framework.award.entity.ActivityRedPacket;
import com.gofobao.framework.award.entity.ActivityRedPacketLog;
import com.gofobao.framework.award.repository.RedPackageLogRepository;
import com.gofobao.framework.award.repository.RedPackageRepository;
import com.gofobao.framework.award.service.RedPackageService;
import com.gofobao.framework.award.vo.request.VoOpenRedPackageReq;
import com.gofobao.framework.award.vo.request.VoRedPackageReq;
import com.gofobao.framework.award.vo.response.OpenRedPackage;
import com.gofobao.framework.award.vo.response.RedPackageRes;
import com.gofobao.framework.common.capital.CapitalChangeEntity;
import com.gofobao.framework.common.capital.CapitalChangeEnum;
import com.gofobao.framework.common.constans.TypeTokenContants;
import com.gofobao.framework.common.rabbitmq.MqConfig;
import com.gofobao.framework.common.rabbitmq.MqHelper;
import com.gofobao.framework.common.rabbitmq.MqQueueEnum;
import com.gofobao.framework.common.rabbitmq.MqTagEnum;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.helper.project.CapitalChangeHelper;
import com.gofobao.framework.system.entity.Notices;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.gofobao.framework.listener.providers.NoticesMessageProvider.GSON;

/**
 * Created by admin on 2017/6/7.
 */
@Slf4j
@Component
public class RedPackageServiceImpl implements RedPackageService {
    @Autowired
    private RedPackageRepository redPackageRepository;
    @Autowired
    private RedPackageLogRepository redPackageLogRepository;
    @Autowired
    private MqHelper mqHelper;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 红包列表
     *
     * @param voRedPackageReq
     * @return
     */
    @Override
    public List<RedPackageRes> list(VoRedPackageReq voRedPackageReq) {
        Page<ActivityRedPacket> packetPage;

        if (RedPacketContants.unUsed == voRedPackageReq.getStatus()) {//未使用
            packetPage = redPackageRepository.findByUserIdAndStatusIsAndEndAtGreaterThanEqual(voRedPackageReq.getUserId(),
                    voRedPackageReq.getStatus(),
                    new Date(),
                    new PageRequest(voRedPackageReq.getPageIndex(),
                            voRedPackageReq.getPageSize(),
                            new Sort(Sort.Direction.DESC, "id")
                    ));
        } else if (RedPacketContants.used == voRedPackageReq.getStatus()) { //已领取
            packetPage = redPackageRepository.findByUserIdAndStatusIs(voRedPackageReq.getUserId(),
                    voRedPackageReq.getStatus(),
                    new PageRequest(voRedPackageReq.getPageIndex(),
                            voRedPackageReq.getPageSize(),
                            new Sort(Sort.Direction.DESC, "updateDate")
                    ));
        } else if (RedPacketContants.Past == voRedPackageReq.getStatus()) { //已过期
            packetPage = redPackageRepository.findByUserIdAndStatusIsOrEndAtLessThanEqual(voRedPackageReq.getUserId(),
                    voRedPackageReq.getStatus(),
                    new Date(),
                    new PageRequest(voRedPackageReq.getPageIndex(),
                            voRedPackageReq.getPageSize(),
                            new Sort(Sort.Direction.DESC, "id")
                    ));
        } else {
            return Collections.EMPTY_LIST;
        }
        List<ActivityRedPacket> packetList = packetPage.getContent();
        if (CollectionUtils.isEmpty(packetList)) {
            return Collections.EMPTY_LIST;
        }

        List<RedPackageRes> redPackageRes = Lists.newArrayList();
        packetList.stream().forEach(p -> {
            RedPackageRes packageRes = new RedPackageRes();
            if (voRedPackageReq.getStatus() == RedPacketContants.unUsed) {
                packageRes.setRedPackageId(p.getId());
            }
            if (voRedPackageReq.getStatus() == RedPacketContants.used) {
                packageRes.setExpiryDate(DateHelper.dateToString(p.getUpdateDate()));
                packageRes.setMoney(StringHelper.toString(p.getMoney() / 100D));
            } else {
                packageRes.setExpiryDate(DateHelper.dateToString(p.getBeginAt()) + "~" + DateHelper.dateToString(p.getBeginAt()));
            }
            packageRes.setTitle(p.getActivityName());
            redPackageRes.add(packageRes);
        });
        return redPackageRes;
    }


    /**
     * 拆红包
     *
     * @param req
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ActivityRedPacket> openRedPackage(VoOpenRedPackageReq req) {

        StringBuffer sb = new StringBuffer();
        sb.append(" SELECT redPackage FROM ActivityRedPacket redPackage " +
                " WHERE " +
                " redPackage.userId =:userId " +
                " AND " +
                " (redPackage.id=:redId OR redPackage.vparam1=:redId )" +
                " AND " +
                " redPackage.del=0 " +
                " AND  " +
                " redPackage.status=0 " +
                " AND " +
                " redPackage.endAt>NOW()");

        TypedQuery<ActivityRedPacket> redPackets = entityManager
                .createQuery(sb.toString(), ActivityRedPacket.class)
                .setParameter("userId", req.getUserId())
                .setParameter("redId", req.getRedPackageId());

        List<ActivityRedPacket> packetList = Lists.newArrayList();
        int looper = 2;
        while (looper > 0) {
            packetList = redPackets.getResultList();
            if (!CollectionUtils.isEmpty(packetList)) {
                break;
            }
            --looper;
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                break;
            }
        }

        return packetList;

    }
}
