package com.gofobao.framework.currency.controller;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.currency.biz.CurrencyBiz;
import com.gofobao.framework.currency.vo.request.VoConvertCurrencyReq;
import com.gofobao.framework.currency.vo.request.VoListCurrencyReq;
import com.gofobao.framework.currency.vo.response.VoListCurrencyResp;
import com.gofobao.framework.security.contants.SecurityContants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

/**
 * Created by Zeke on 2017/5/23.
 */
@RestController
@RequestMapping
@Api(description = "广富币模块")
@Slf4j
public class CurrencyController {

    @Autowired
    private CurrencyBiz currencyBiz;

    @ApiOperation("获取用户广富币列表")
    @GetMapping("/pub/currency/list/{pageIndex}/{pageSize}")
    public ResponseEntity<VoListCurrencyResp> list(@PathVariable Integer pageIndex,
                                           @PathVariable Integer pageSize,
                                           @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        VoListCurrencyReq voListCurrencyReq = new VoListCurrencyReq();
        voListCurrencyReq.setPageIndex(pageIndex);
        voListCurrencyReq.setPageSize(pageSize);
        voListCurrencyReq.setUserId(userId);
        return currencyBiz.list(voListCurrencyReq);
    }

    /**
     * 兑换广福币
     *
     * @return
     */
    @ApiOperation("兑换广富币")
    @PostMapping("/currency/convert")
    public ResponseEntity<VoBaseResp> convert(@Valid @ModelAttribute VoConvertCurrencyReq voConvertCurrencyReq,
                                              @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        voConvertCurrencyReq.setUserId(userId);
        try {
            return currencyBiz.convert(voConvertCurrencyReq);
        } catch (Throwable e) {
            log.error("广富币兑换失败", e);
        }
        return ResponseEntity.badRequest().body(VoBaseResp.error(VoBaseResp.ERROR,"兑换广富币失败!"));
    }
}
