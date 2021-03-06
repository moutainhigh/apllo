package com.gofobao.framework.currency.biz.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.api.contants.ChannelContant;
import com.gofobao.framework.api.contants.DesLineFlagContant;
import com.gofobao.framework.api.contants.JixinResultContants;
import com.gofobao.framework.api.helper.JixinManager;
import com.gofobao.framework.api.helper.JixinTxCodeEnum;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayRequest;
import com.gofobao.framework.api.model.voucher_pay.VoucherPayResponse;
import com.gofobao.framework.common.assets.AssetChange;
import com.gofobao.framework.common.assets.AssetChangeProvider;
import com.gofobao.framework.common.assets.AssetChangeTypeEnum;
import com.gofobao.framework.common.constans.JixinContants;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.currency.biz.CurrencyBiz;
import com.gofobao.framework.currency.entity.Currency;
import com.gofobao.framework.currency.entity.CurrencyLog;
import com.gofobao.framework.currency.repository.CurrencyLogRepository;
import com.gofobao.framework.currency.service.CurrencyLogService;
import com.gofobao.framework.currency.service.CurrencyService;
import com.gofobao.framework.currency.vo.request.VoConvertCurrencyReq;
import com.gofobao.framework.currency.vo.request.VoListCurrencyReq;
import com.gofobao.framework.currency.vo.response.VoCurrency;
import com.gofobao.framework.currency.vo.response.VoListCurrencyResp;
import com.gofobao.framework.helper.DateHelper;
import com.gofobao.framework.helper.NumberHelper;
import com.gofobao.framework.helper.StringHelper;
import com.gofobao.framework.helper.ThirdAccountHelper;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.system.entity.DictItem;
import com.gofobao.framework.system.entity.DictValue;
import com.gofobao.framework.system.service.DictItemService;
import com.gofobao.framework.system.service.DictValueService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Created by Zeke on 2017/5/23.
 */
@Service
@Slf4j
public class CurrencyBizImpl implements CurrencyBiz {

    /**
     * 最小兑换广富币
     */
    public static final int CURRENCY_CONVERT_MIN = 100;

    @Autowired
    private CurrencyService currencyService;
    @Autowired
    private CurrencyLogService currencyLogService;

    @Autowired
    AssetChangeProvider assetChangeProvider;
    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private JixinManager jixinManager;

    @Autowired
    private CurrencyLogRepository currencyLogRepository;
    @Autowired
    private DictItemService dictItemService;

    @Autowired
    private DictValueService dictValueService;

    LoadingCache<String, DictValue> jixinCache = CacheBuilder
            .newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build(new CacheLoader<String, DictValue>() {
                @Override
                public DictValue load(String bankName) throws Exception {
                    DictItem dictItem = dictItemService.findTopByAliasCodeAndDel("JIXIN_PARAM", 0);
                    if (ObjectUtils.isEmpty(dictItem)) {
                        return null;
                    }

                    return dictValueService.findTopByItemIdAndValue01(dictItem.getId(), bankName);
                }
            });

    private static Map<String, String> currencyTypeMap = new HashMap<>();

    static {
        currencyTypeMap.put("daily_settlement", "每日结算");
        currencyTypeMap.put("month_settlement", "每月结算");
        currencyTypeMap.put("convert", "兑换");
    }

    public ResponseEntity<VoListCurrencyResp> list(VoListCurrencyReq voListCurrencyReq) {
        int pageSize = voListCurrencyReq.getPageSize();
        int pageIndex = voListCurrencyReq.getPageIndex();
        Long userId = voListCurrencyReq.getUserId();


        Currency currency = currencyService.findByUserId(userId);
        if (currency == null) {
            return null;
        }

        //分页和排序
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "id"));

        Pageable pageable = new PageRequest(pageIndex, pageSize, sort);

        Specification specification = Specifications.<Currency>and()
                .eq("userId", userId)
                .build();
        Page<CurrencyLog> logPage = currencyLogRepository.findAll(specification, pageable);

        List<CurrencyLog> currencyLogs = logPage.getContent();

        VoListCurrencyResp voListCurrencyResp = VoBaseResp.ok("查询成功!", VoListCurrencyResp.class);
        voListCurrencyResp.setAvailableCurrency(currency.getUseCurrency());
        voListCurrencyResp.setInvalidCurrency(currency.getNoUseCurrency());
        voListCurrencyResp.setTotalCurrency(currency.getNoUseCurrency() + currency.getUseCurrency());

        List<VoCurrency> currencyList = new ArrayList<>();
        Optional<List<CurrencyLog>> objCurrencys = Optional.ofNullable(currencyLogs);
        objCurrencys.ifPresent(p -> p.forEach(currencyLog -> {
            VoCurrency voCurrency = new VoCurrency();
            voCurrency.setTotalCurrency(currencyLog.getUseCurrency());
            voCurrency.setDate(DateHelper.dateToStringYearMouthDay(currencyLog.getCreatedAt()));
            voCurrency.setCurrency("convert".equalsIgnoreCase(currencyLog.getType()) ? String.format("-%s", currencyLog.getValue()) : String.format("+%s", currencyLog.getValue()));
            voCurrency.setType(currencyLog.getType());
            voCurrency.setTypeName(findCurrencyMap(currencyLog.getType()));
            voCurrency.setNoCurrency(currencyLog.getNoUseCurrency());
            currencyList.add(voCurrency);
        }));
        Long totalCount = logPage.getTotalElements();
        voListCurrencyResp.setTotalCount(totalCount.intValue());
        voListCurrencyResp.setVoCurrencyList(currencyList);
        return ResponseEntity.ok(voListCurrencyResp);
    }

    /**
     * 兑换广福币
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<VoBaseResp> convert(VoConvertCurrencyReq voConvertCurrencyReq) throws Exception {
        Long userId = voConvertCurrencyReq.getUserId();
        Integer currency = voConvertCurrencyReq.getCurrency();
        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        ResponseEntity<VoBaseResp> checkResponse = ThirdAccountHelper.allConditionCheck(userThirdAccount);
        if (!checkResponse.getStatusCode().equals(HttpStatus.OK)) {
            return checkResponse;
        }

        Currency currencyObj = currencyService.findByUserIdLock(userId);
        if (ObjectUtils.isEmpty(currencyObj)) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "兑换广富币异常：获取用户广富币失败!"));
        }

        if (currencyObj.getUseCurrency() < currency) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前兑换数量大于已有数量!"));
        }

        if (currency < CURRENCY_CONVERT_MIN) {
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR, "当前兑换数量最小兑换数量!"));
        }

        //查询红包账户
        DictValue dictValue = jixinCache.get(JixinContants.RED_PACKET_USER_ID);
        UserThirdAccount redPacketAccount = userThirdAccountService.findByUserId(NumberHelper.toLong(dictValue.getValue03()));

        //调用即信发送红包接口
        VoucherPayRequest voucherPayRequest = new VoucherPayRequest();
        voucherPayRequest.setAccountId(redPacketAccount.getAccountId());
        voucherPayRequest.setTxAmount(StringHelper.formatDouble(currency, 100, false));
        voucherPayRequest.setForAccountId(userThirdAccount.getAccountId());
        voucherPayRequest.setDesLineFlag(DesLineFlagContant.TURE);
        voucherPayRequest.setChannel(ChannelContant.HTML);
        voucherPayRequest.setDesLine("用户广富币兑换");
        VoucherPayResponse response = jixinManager.send(JixinTxCodeEnum.SEND_RED_PACKET, voucherPayRequest, VoucherPayResponse.class);
        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
            log.error(String.format("广富币兑换失败: %s", new Gson().toJson(voucherPayRequest)));
            String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
            throw new Exception("广富币兑换异常：" + msg);
        }
        currencyObj.setUseCurrency(currencyObj.getUseCurrency() - currency);
        currencyObj.setNoUseCurrency(currencyObj.getNoUseCurrency() + currency);
        currencyObj.setUpdatedAt(new Date());
        currencyService.save(currencyObj);
        CurrencyLog currencyLog = new CurrencyLog();
        currencyLog.setUseCurrency(currencyObj.getUseCurrency());
        currencyLog.setNoUseCurrency(currencyObj.getNoUseCurrency());
        currencyLog.setUserId(userId);
        currencyLog.setType("convert");
        currencyLog.setValue(currency);
        currencyLog.setCreatedAt(new Date());
        long money = currency; //金额
        currencyLog = currencyLogService.insert(currencyLog);
        String groupSeqNo = assetChangeProvider.getGroupSeqNo();
        long redId = assetChangeProvider.getRedpackAccountId();
        Date nowDate = new Date();

        // 平台发放广富币兑换金额
        AssetChange redpackPublish = new AssetChange();
        redpackPublish.setMoney(money);
        redpackPublish.setType(AssetChangeTypeEnum.platformPublishCurrencyExchangeRedpack);  // 广富币兑换
        redpackPublish.setUserId(userId);
        redpackPublish.setRemark(String.format("派发用户在%s, 使用广富币(%s)兑换%s元",
                DateHelper.dateToString(nowDate),
                voConvertCurrencyReq.getCurrency(),
                StringHelper.formatDouble(money / 100D, true)));
        redpackPublish.setSeqNo(String.format("%s%s%s", response.getTxDate(), response.getTxTime(), response.getSeqNo()));
        redpackPublish.setGroupSeqNo(groupSeqNo);
        redpackPublish.setForUserId(redId);
        redpackPublish.setSourceId(currencyLog.getId());
        assetChangeProvider.commonAssetChange(redpackPublish);

        // 接收红包
        AssetChange redpackR = new AssetChange();
        redpackR.setMoney(money);
        redpackR.setType(AssetChangeTypeEnum.currencyExchangeRedpack);  // 积分兑换
        redpackR.setUserId(userId);
        redpackR.setRemark(String.format("你在%s, 成功使用广富币(%s)兑换%s元",
                DateHelper.dateToString(nowDate),
                voConvertCurrencyReq.getCurrency(),
                StringHelper.formatDouble(money / 100D, true)));
        redpackR.setSeqNo(String.format("%s%s%s", response.getTxDate(), response.getTxTime(), response.getSeqNo()));
        redpackR.setGroupSeqNo(groupSeqNo);
        redpackR.setForUserId(redId);
        redpackR.setSourceId(currencyLog.getId());
        assetChangeProvider.commonAssetChange(redpackR);
        return ResponseEntity.ok(VoBaseResp.ok("广富币兑换成功!"));
    }


    /**
     * 查找积分类型名称
     *
     * @param type
     * @return
     */
    private String findCurrencyMap(String type) {
        String typeName = currencyTypeMap.get(type);
        return StringUtils.isEmpty(typeName) ? "其他" : typeName;
    }
}
