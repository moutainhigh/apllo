package com.gofobao.framework.tender.biz.impl;

import com.gofobao.framework.api.contants.ChannelContant;
import com.gofobao.framework.api.contants.JixinResultContants;
import com.gofobao.framework.api.helper.JixinManager;
import com.gofobao.framework.api.helper.JixinTxCodeEnum;
import com.gofobao.framework.api.model.bid_auto_apply.BidAutoApplyRequest;
import com.gofobao.framework.api.model.bid_auto_apply.BidAutoApplyResponse;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.JixinHelper;
import com.gofobao.framework.helper.NumberHelper;
import com.gofobao.framework.member.entity.UserThirdAccount;
import com.gofobao.framework.member.service.UserThirdAccountService;
import com.gofobao.framework.tender.biz.TenderThirdBiz;
import com.gofobao.framework.tender.entity.Tender;
import com.gofobao.framework.tender.service.TenderService;
import com.gofobao.framework.tender.vo.request.VoCreateThirdTenderReq;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * Created by Zeke on 2017/6/1.
 */
@Service
public class TenderThirdBizImpl implements TenderThirdBiz{

    @Autowired
    private UserThirdAccountService userThirdAccountService;
    @Autowired
    private JixinManager jixinManager;
    @Autowired
    private TenderService tenderService;

    public ResponseEntity<VoBaseResp> createThirdTender(VoCreateThirdTenderReq voCreateThirdTenderReq){
        Long userId = voCreateThirdTenderReq.getUserId();
        String txAmount = voCreateThirdTenderReq.getTxAmount();

        UserThirdAccount userThirdAccount = userThirdAccountService.findByUserId(userId);
        Preconditions.checkNotNull(userThirdAccount, "投标人未开户!");

        String autoTenderOrderId = userThirdAccount.getAutoTenderOrderId();
        if (StringUtils.isEmpty(autoTenderOrderId)){
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR,"投标人未进行投标签约!"));
        }
        Long autoTenderTxAmount = userThirdAccount.getAutoTenderTxAmount();
        if (autoTenderTxAmount < (NumberHelper.toDouble(txAmount) * 100)){
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR,"投标金额超出签约单笔最大投标额!"));
        }
        Long autoTenderTotAmount = userThirdAccount.getAutoTenderTotAmount();
        if (autoTenderTotAmount < (NumberHelper.toDouble(txAmount) * 100)){
            return ResponseEntity
                    .badRequest()
                    .body(VoBaseResp.error(VoBaseResp.ERROR,"投标金额超出签约总投标额!"));
        }

        BidAutoApplyRequest request = new BidAutoApplyRequest();
        request.setAccountId(userThirdAccount.getAccountId());
        request.setOrderId(JixinHelper.getTenderOrderId());
        request.setTxAmount(voCreateThirdTenderReq.getTxAmount());
        request.setProductId(voCreateThirdTenderReq.getProductId());
        request.setFrzFlag(voCreateThirdTenderReq.getFrzFlag());
        request.setContOrderId(autoTenderOrderId);
        request.setAcqRes(voCreateThirdTenderReq.getAcqRes());
        request.setChannel(ChannelContant.HTML);

        BidAutoApplyResponse response = jixinManager.send(JixinTxCodeEnum.BID_AUTO_APPLY, request, BidAutoApplyResponse.class);
        if ((ObjectUtils.isEmpty(response)) || (!JixinResultContants.SUCCESS.equals(response.getRetCode()))) {
            String msg = ObjectUtils.isEmpty(response) ? "当前网络不稳定，请稍候重试" : response.getRetMsg();
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, msg));
        }

        //更新tender记录
        Tender updTender = new Tender();
        updTender.setId(NumberHelper.toLong(request.getAcqRes()));
        updTender.setAuthCode(response.getAuthCode());
        updTender.setTUserId(userThirdAccount.getId());
        if (tenderService.updateById(updTender)){
            return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR, "借款失败!"));
        }
        return null;
    }
}
