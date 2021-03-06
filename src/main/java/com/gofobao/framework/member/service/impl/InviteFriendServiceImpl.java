package com.gofobao.framework.member.service.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.common.constans.MoneyConstans;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.member.entity.BrokerBouns;
import com.gofobao.framework.member.entity.Users;
import com.gofobao.framework.member.repository.BrokerBounsRepository;
import com.gofobao.framework.member.repository.UsersRepository;
import com.gofobao.framework.member.service.InviteFriendsService;
import com.gofobao.framework.member.vo.request.VoFriendsReq;
import com.gofobao.framework.member.vo.request.VoFriendsTenderReq;
import com.gofobao.framework.member.vo.response.FriendsTenderInfo;
import com.gofobao.framework.member.vo.response.InviteAwardStatistics;
import com.gofobao.framework.member.vo.response.InviteFriends;
import com.gofobao.framework.member.vo.response.pc.PcInviteFriends;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.repository.TenderRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by admin on 2017/6/6.
 */
@Component
public class InviteFriendServiceImpl implements InviteFriendsService {

    @Autowired
    private BrokerBounsRepository brokerBounsRepository;

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private TenderRepository tenderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<InviteFriends> list(VoFriendsReq voFriendsReq) {
        BrokerBouns brokerBouns = new BrokerBouns();
        brokerBouns.setUserId(voFriendsReq.getUserId());
        Example<BrokerBouns> example = Example.of(brokerBouns);
        Page<BrokerBouns> brokerBounss = brokerBounsRepository.findAll(example,
                new PageRequest(
                        voFriendsReq.getPageIndex(),
                        voFriendsReq.getPageSize(),
                        new Sort(Sort.Direction.DESC, "createdAt")));
        List<BrokerBouns> bounsList = brokerBounss.getContent();
        if (CollectionUtils.isEmpty(bounsList)) {
            return Collections.EMPTY_LIST;
        }
        List<InviteFriends> friendsList = new ArrayList<>();
        bounsList.stream().forEach(p -> {
            InviteFriends friends = new InviteFriends();
            friends.setCreatedAt(DateHelper.dateToString(p.getCreatedAt()));
            friends.setMoney(StringHelper.formatMon(p.getBounsAward() / 100D));
            friends.setLeave(p.getLevel());
            friends.setScale(StringHelper.formatMon(p.getAwardApr() / 100D));
            friends.setWaitPrincipalTotal(StringHelper.formatMon(p.getWaitPrincipalTotal() / 100D));
            friendsList.add(friends);
        });
        return Optional.ofNullable(friendsList).orElse(Collections.EMPTY_LIST);
    }

    @Override
    public Map<String, Object> pcBrokerBounsList(VoFriendsTenderReq friendsTenderReq) {
        Map<String, Object> resultMaps = Maps.newHashMap();

        Date beginAt = DateHelper.beginOfDate(
                DateHelper.stringToDate(friendsTenderReq.getBeginAt(),
                        DateHelper.DATE_FORMAT_YMD));
        Date endAt = DateHelper.endOfDate(DateHelper.stringToDate(
                friendsTenderReq.getEndAt(),
                DateHelper.DATE_FORMAT_YMD));
        Specification specification = Specifications.<BrokerBouns>and()
                .eq("userId", friendsTenderReq.getUserId())
                .between("createdAt", new Range<>(beginAt, endAt))
                .build();
        Page<BrokerBouns> bounsPage = brokerBounsRepository.findAll(specification,
                new PageRequest(friendsTenderReq.getPageIndex(),
                        friendsTenderReq.getPageSize(),
                        new Sort(Sort.Direction.DESC, "id")));

        Long totalCount = bounsPage.getTotalElements();
        List<BrokerBouns> bounsList = bounsPage.getContent();

        resultMaps.put("totalCount", totalCount);
        if (CollectionUtils.isEmpty(bounsList)) {
            resultMaps.put("bounsList", new ArrayList<>(0));
            return resultMaps;
        }
        List<InviteFriends> brokerBouns = Lists.newArrayList();
        bounsList.stream().forEach(p -> {
            InviteFriends friends = new InviteFriends();
            friends.setScale(StringHelper.formatMon(p.getAwardApr() / 100D));
            friends.setWaitPrincipalTotal(StringHelper.formatMon(p.getWaitPrincipalTotal() / 100D));
            friends.setLeave(p.getLevel());
            friends.setMoney(StringHelper.formatMon(p.getBounsAward() / 100D));
            friends.setCreatedAt(DateHelper.dateToString(p.getCreatedAt()));
            brokerBouns.add(friends);
        });
        resultMaps.put("bounsList", brokerBouns);
        return resultMaps;
    }


    @Override
    public List<InviteFriends> toExcel(VoFriendsTenderReq friendsTenderReq) {
        Specification specification = Specifications.<BrokerBouns>and()
                .eq("userId", friendsTenderReq.getUserId())
                .between("createdAt", new Range<>(DateHelper.stringToDate(friendsTenderReq.getBeginAt(),
                        DateHelper.DATE_FORMAT_YMD),
                        DateHelper.stringToDate(friendsTenderReq.getEndAt(),
                                DateHelper.DATE_FORMAT_YMD)))
                .build();
        List<BrokerBouns> bounsList = brokerBounsRepository.findAll(specification, new Sort(Sort.Direction.DESC, "id"));
        List<InviteFriends> brokerBouns = new ArrayList<>(bounsList.size());
        bounsList.forEach(p -> {
            InviteFriends inviteFriends = new InviteFriends();
            inviteFriends.setCreatedAt(DateHelper.dateToString(p.getCreatedAt()));
            inviteFriends.setMoney(StringHelper.formatMon(p.getBounsAward() / 100D));
            inviteFriends.setLeave(p.getLevel());
            inviteFriends.setWaitPrincipalTotal(StringHelper.toString(p.getWaitPrincipalTotal() / 100D));
            inviteFriends.setScale(StringHelper.formatMon(p.getAwardApr() / 100D) + MoneyConstans.PERCENT);
            brokerBouns.add(inviteFriends);
        });
        return brokerBouns;
    }

    @Override
    public InviteAwardStatistics query(Long userId) {
        try {
            InviteAwardStatistics inviteAwardStatistics = new InviteAwardStatistics();
            Users users1 = usersRepository.getOne(userId);
            inviteAwardStatistics.setInviteCode1(users1.getInviteCode());
            inviteAwardStatistics.setInviteCode2(StringUtils.isEmpty(users1.getPhone()) ? "" : users1.getPhone());
            Users users = new Users();
            users.setParentId(userId);
            Example<Users> example = Example.of(users);
            Long count = usersRepository.count(example);
            //邀请总人数
            inviteAwardStatistics.setCountNum(count.intValue());
            //奖励
            Specification<BrokerBouns> specification = Specifications.<BrokerBouns>and()
                    .eq("userId", userId)
                    .build();
            Page<BrokerBouns> brokerBounsPage = brokerBounsRepository.findAll(specification,
                    new PageRequest(0,
                            1,
                            new Sort(Sort.Direction.DESC, "id")));
            List<BrokerBouns> brokerBounss = brokerBounsPage.getContent();
            if (!ObjectUtils.isEmpty(brokerBounss)) {
                BrokerBouns brokerBouns = brokerBounss.get(0);
                //昨日奖励
                Integer yestodayBounsAward = brokerBouns.getBounsAward();
                //昨日奖励
                inviteAwardStatistics.setYesterdayAward(StringHelper.formatMon(yestodayBounsAward / 100D));
                //年化率
                inviteAwardStatistics.setApr(StringHelper.formatMon(brokerBouns.getAwardApr() / 100D));
                //待收本金
                Long waitPrincipalTotal = brokerBouns.getWaitPrincipalTotal();
                inviteAwardStatistics.setWaitPrincipalTotal(StringHelper.formatMon(waitPrincipalTotal / 100D));
                inviteAwardStatistics.setLevel(brokerBouns.getLevel());
            }
            //总奖励
            Integer sumBounsAward = brokerBounsRepository.sumBounsAward(userId);
            inviteAwardStatistics.setSumAward(StringHelper.formatMon(StringUtils.isEmpty(sumBounsAward) ? 0 : sumBounsAward / 100D));
            return inviteAwardStatistics;
        } catch (Exception e) {
            e.printStackTrace();
            return new InviteAwardStatistics();
        }

    }

    /**
     * @param voFriendsReq
     * @return
     */
    @Override
    public List<FriendsTenderInfo> inviteUserFirstTender(VoFriendsReq voFriendsReq) {
        Specification<Users> usersSpecification = Specifications.<Users>and()
                .eq("parentId", voFriendsReq.getUserId())
                .build();
        Page<Users> usersPage = usersRepository.findAll(usersSpecification,
                new PageRequest(
                        voFriendsReq.getPageIndex(),
                        voFriendsReq.getPageSize(), new Sort(Sort.Direction.DESC, "createdAt")));
        List<Users> usersList = usersPage.getContent();

        return commonHandle(usersList);
    }

    private List<FriendsTenderInfo> commonHandle(List<Users> usersList) {
        if (CollectionUtils.isEmpty(usersList)) {
            return Collections.EMPTY_LIST;
        }
        Set<Long> userArray = usersList.stream()
                .map(p -> p.getId())
                .collect(Collectors.toSet());

        List<Tender> tenderList = tenderRepository.findUserFirstTender(new ArrayList(userArray));

        Map<Long, Tender> tenderMap = tenderList.stream()
                .collect(Collectors.toMap(Tender::getUserId, a -> a));

        List<FriendsTenderInfo> tenderInfoList = new ArrayList<>();
        usersList.stream().forEach(p -> {
            FriendsTenderInfo info = new FriendsTenderInfo();
            info.setUserName(StringUtils.isEmpty(p.getPhone()) ? p.getUsername() : p.getPhone());
            info.setRegisterTime(DateHelper.dateToString(p.getCreatedAt()));
            Tender tender = tenderMap.get(p.getId());
            if (ObjectUtils.isEmpty(tender)) {
                info.setFirstTenderTime("---");
            } else {
                info.setFirstTenderTime(DateHelper.dateToString(tender.getCreatedAt()));
            }
            tenderInfoList.add(info);
        });
        return Optional.ofNullable(tenderInfoList).orElse(Collections.EMPTY_LIST);
    }


    @Override
    public List<FriendsTenderInfo> employeeInviteUserFirstTender(VoFriendsReq voFriendsReq) {
        String sql = "SELECT u FROM Users u WHERE  u.parentId=:userId AND ";
        sql += !voFriendsReq.getSource() ? "u.type!=:type" : "u.type=:type";
        Query query = entityManager.createQuery(sql.toString(), Users.class);
        query.setParameter("type", "finance");
        query.setParameter("userId", voFriendsReq.getUserId());
        query.setFirstResult(voFriendsReq.getPageIndex());
        query.setMaxResults(voFriendsReq.getPageSize() * voFriendsReq.getPageIndex());
        List<Users> usersList = query.getResultList();
        return commonHandle(usersList);
    }

    /**
     * PC:邀请用户投标记录
     *
     * @param voFriendsReq
     * @return
     */
    @Override
    public Map<String, Object> pcInviteUserFirstTender(VoFriendsReq voFriendsReq) {

        Map<String, Object> resultMaps = Maps.newHashMap();

        Specification specification = null;
        if (voFriendsReq.getType() == 0) {
            specification = Specifications.<Users>and()
                    .eq("parentId", voFriendsReq.getUserId())
                    .build();
        } else if (voFriendsReq.getType() == 1) {
            specification = Specifications.<Users>and()
                    .eq("parentId", voFriendsReq.getUserId())
                    .between("createdAt", new Range<>(DateHelper.subDays(new Date(), 365), new Date()))
                    .build();
        }

        if (voFriendsReq.getPageIndex().intValue() > new Integer(1)) {
            voFriendsReq.setPageIndex((voFriendsReq.getPageIndex() - 1) * voFriendsReq.getPageSize());
        }
        Page<Users> usersPage = usersRepository.findAll(specification,
                new PageRequest(voFriendsReq.getPageIndex(),
                        voFriendsReq.getPageSize(),
                        new Sort("id")));
        Long totalCount = usersPage.getTotalElements();

        List<Users> usersList = usersPage.getContent();
        resultMaps.put("totalCount", totalCount.intValue());

        if (CollectionUtils.isEmpty(usersList)) {

            resultMaps.put("userList", new ArrayList<>(0));
            return resultMaps;
        }
        Set<Long> userArray = usersList.stream()
                .map(p -> p.getId())
                .collect(Collectors.toSet());

        List<Tender> tenderList = tenderRepository.findUserFirstTender(new ArrayList(userArray));

        Map<Long, Tender> tenderMap = tenderList.stream()
                .collect(Collectors.toMap(Tender::getUserId, a -> a));
        List<PcInviteFriends> tenderInfoList = new ArrayList<>();
        usersList.stream().forEach(p -> {
            PcInviteFriends info = new PcInviteFriends();
            info.setUserName(StringUtils.isEmpty(p.getPhone()) ? p.getUsername() : p.getPhone());
            info.setCreateAt(DateHelper.dateToString(p.getCreatedAt()));
            Tender tender = tenderMap.get(p.getId());
            info.setTender(ObjectUtils.isEmpty(tender) ? false : true);
            info.setRealName(StringUtils.isEmpty(p.getCardId()) ? false : true);
            tenderInfoList.add(info);
        });
        resultMaps.put("userList", tenderInfoList);
        return resultMaps;
    }

}
