package com.gofobao.framework.windmill.borrow.biz.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.asset.biz.AssetBiz;
import com.gofobao.framework.asset.entity.Asset;
import com.gofobao.framework.asset.entity.AssetLog;
import com.gofobao.framework.asset.repository.AssetLogRepository;
import com.gofobao.framework.asset.service.AssetService;
import com.gofobao.framework.asset.vo.response.VoAssetIndexResp;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.member.entity.UserCache;
import com.gofobao.framework.member.service.UserCacheService;
import com.gofobao.framework.system.entity.Article;
import com.gofobao.framework.system.repository.ArticleRepository;
import com.gofobao.framework.windmill.borrow.biz.WindmillStatisticsBiz;
import com.gofobao.framework.windmill.borrow.service.WindmillStatisticsService;
import com.gofobao.framework.windmill.borrow.vo.response.ByDayStatistics;
import com.gofobao.framework.windmill.borrow.vo.response.UserAccountStatistics;
import com.gofobao.framework.windmill.user.vo.respones.Notices;
import com.gofobao.framework.windmill.user.vo.respones.VoNoticesRes;
import com.gofobao.framework.windmill.util.StrToJsonStrUtil;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/8/3.
 */
@Service
public class WindmillStatisticsBizImpl implements WindmillStatisticsBiz {


    @Value("${windmill.des-key}")
    private String desKey;


    private static final Gson GSON = new Gson();

    @Autowired
    private WindmillStatisticsService windmillStatisticsService;

    @Autowired
    private AssetLogRepository assetLogRepository;


    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AssetBiz assetBiz;

    @Value("${gofobao.h5Domain}")
    private String h5Domain;


    private static final String DETAILS_PATH = "/#/finder/article/detail/";


    /**
     * 查询每日的汇总数据
     *
     * @param request
     * @return
     */
    @Override
    public ByDayStatistics byDayStatistics(HttpServletRequest request) {
        ByDayStatistics byDayStatistics = new ByDayStatistics();
        Map<String, Object> paramMap;
        try {
            String paramStr = request.getParameter("param");
            paramMap = StrToJsonStrUtil.commonUrlParamToMap(paramStr, desKey);
        } catch (Exception e) {
            byDayStatistics.setRetcode(VoBaseResp.ERROR);
            byDayStatistics.setRetmsg("平台转json失败");
            return byDayStatistics;
        }
        return windmillStatisticsService.bySomeDayStatistics(paramMap.get("date").toString());
    }

    /**
     * 5.5账户信息查询接口
     *
     * @param request
     * @return
     */
    @Override
    public UserAccountStatistics userStatistics(HttpServletRequest request) {

        UserAccountStatistics accountStatistics = new UserAccountStatistics();
        Map<String, Object> paramMap;
        Long userId;
        try {
            String paramStr = request.getParameter("param");
            paramMap = StrToJsonStrUtil.commonUrlParamToMap(paramStr, desKey);

            userId = Long.valueOf(paramMap.get("pf_user_id").toString());
        } catch (Exception e) {
            accountStatistics.setRetcode(VoBaseResp.ERROR);
            accountStatistics.setRetmsg("平台转json失败");
            return accountStatistics;
        }
        try {
            ResponseEntity<VoAssetIndexResp> asset = assetBiz.asset(userId);
            VoAssetIndexResp voAssetIndexResp = asset.getBody();
            //资产
            Asset userAsset = assetService.findByUserId(userId);
            //用户信息
            UserCache userCache = userCacheService.findById(userId);
            //资产记录
            List<String> typeArray = Lists.newArrayList("integral_cash", "virtual_tender", "bonus", "red_package");
            Specification specification = Specifications.<AssetLog>and()
                    .eq("userId", userId)
                    .in("type", typeArray.toArray())
                    .build();
            List<AssetLog> assetLogs = assetLogRepository.findAll(specification);
            //  账户总额
            accountStatistics.setAll_balance(voAssetIndexResp.getAccountMoney());
            //可用余额
            accountStatistics.setAvailable_balance(StringHelper.formatDouble(userAsset.getUseMoney() / 100D, false));
            //冻结金额
            accountStatistics.setFrozen_money(StringHelper.formatDouble(userAsset.getNoUseMoney() / 100D, false));
            //待收本金
            accountStatistics.setInvesting_principal(StringHelper.formatDouble(userCache.getWaitCollectionPrincipal() / 100D, false));
            //待收利息
            accountStatistics.setInvesting_interest(StringHelper.formatDouble(userCache.getWaitCollectionInterest() / 100D, false));
            //累计已回款收益
            accountStatistics.setEarned_interest(voAssetIndexResp.getAccruedMoney());
            //活期金额
            accountStatistics.setPf_user_id(userId.toString());
            accountStatistics.setCurrent_money(userCache.getCashTotal() + "");
            //奖励余额
            String reward = "";
            if (!CollectionUtils.isEmpty(assetLogs)) {
                for (AssetLog assetLog : assetLogs
                        ) {
                    reward += assetLog.getRemark() + "-" + assetLog.getMoney() + "-分-" + DateHelper.dateToString(assetLog.getCreatedAt()) + "-无" + "-无;";
                }
                reward = reward.substring(0, reward.length() - 1);
            }
            accountStatistics.setReward(reward);
            accountStatistics.setRetcode(VoBaseResp.OK);
            accountStatistics.setRetmsg("查询成功");
            accountStatistics.setReward(reward);
        } catch (Exception e) {
            accountStatistics.setRetcode(VoBaseResp.ERROR);
            accountStatistics.setRetmsg("平台查询异常");
        }
        return accountStatistics;
    }


    /**
     * 平臺公告
     *
     * @param request )
     * @return
     */
    @Override
    public VoNoticesRes noticesList(HttpServletRequest request) {
        VoNoticesRes voNoticesRes = new VoNoticesRes();
        Map<String, Object> paramMap;
        try {
            String param = request.getParameter("param");
            paramMap = StrToJsonStrUtil.commonUrlParamToMap(param, desKey);

        } catch (Exception e) {
            voNoticesRes.setRetmsg("平台转json失败");
            voNoticesRes.setRetcode(VoBaseResp.ERROR);
            return voNoticesRes;
        }

        Specification specification = Specifications.<Article>and()
                .eq("type", "notice")
                .build();
        List<Article> articles = articleRepository.findAll(specification,
                new PageRequest(
                        Integer.valueOf(paramMap.get("page").toString()),
                        Integer.valueOf(paramMap.get("limit").toString()),
                        new Sort(Sort.Direction.DESC, "id")))
                .getContent();

        if (CollectionUtils.isEmpty(articles)) {
            voNoticesRes.setRetmsg("平台公告为空");
            voNoticesRes.setRetcode(VoBaseResp.OK);
            return voNoticesRes;
        }
        List<Notices> all_notices = Lists.newArrayList();
        articles.stream().forEach(p -> {
            Notices notices = new Notices();
            notices.setContent(p.getContent());
            notices.setId(p.getId());
            notices.setRelease_time(DateHelper.dateToString(p.getCreatedAt()));
            notices.setTitle(p.getTitle());
            notices.setUrl(h5Domain + DETAILS_PATH + p.getId());
            all_notices.add(notices);
        });
        voNoticesRes.setRetcode(VoBaseResp.OK);
        voNoticesRes.setRetmsg("查询成功");
        voNoticesRes.setAll_notices(all_notices);
        return voNoticesRes;
    }


}
