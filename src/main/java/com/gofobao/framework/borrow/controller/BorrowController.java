package com.gofobao.framework.borrow.controller;

import com.gofobao.framework.borrow.biz.BorrowBiz;
import com.gofobao.framework.borrow.vo.request.VoAddNetWorthBorrow;
import com.gofobao.framework.borrow.vo.request.VoBorrowListReq;
import com.gofobao.framework.borrow.vo.request.VoCancelBorrow;
import com.gofobao.framework.borrow.vo.response.VoViewBorrowInfoWarpRes;
import com.gofobao.framework.borrow.vo.response.VoViewBorrowListWarpRes;
import com.gofobao.framework.borrow.vo.response.VoViewBorrowStatisticsWarpRes;
import com.gofobao.framework.borrow.vo.response.VoViewVoBorrowDescWarpRes;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.helper.ThymeleafHelper;
import com.gofobao.framework.security.contants.SecurityContants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.Map;

/**
 * Created by Max on 17/5/16.
 */

@RequestMapping("/borrow")
@RestController
@Slf4j
@Api(description = "首页标接口")
@SuppressWarnings("all")
public class BorrowController {

    @Autowired
    private BorrowBiz borrowBiz;
    @Autowired
    private ThymeleafHelper thymeleafHelper;

    @ApiOperation(value = "首页标列表; type:-1：全部 0：车贷标；1：净值标；2：秒标；4：渠道标 ; 5流转标")
    @GetMapping("v2/list/{type}/{pageIndex}/{pageSize}")
    public ResponseEntity<VoViewBorrowListWarpRes> borrowList(@PathVariable Integer pageIndex,
                                                              @PathVariable Integer pageSize,
                                                              @PathVariable Integer type) {
        VoBorrowListReq voBorrowListReq = new VoBorrowListReq();
        voBorrowListReq.setPageIndex(pageIndex);
        voBorrowListReq.setPageSize(pageSize);
        voBorrowListReq.setType(type);
        return borrowBiz.findAll(voBorrowListReq);
    }

    @ApiOperation(value = "pc:首页标列表; type:-1：全部 0：车贷标；1：净值标；2：秒标；4：渠道标 ; 5流转标")
    @GetMapping("pc/v2//{type}/{pageIndex}/{pageSize}")
    public ResponseEntity<VoViewBorrowListWarpRes> pcList(@PathVariable Integer pageIndex,
                                                          @PathVariable Integer pageSize,
                                                          @PathVariable Integer type){
        VoBorrowListReq voBorrowListReq = new VoBorrowListReq();
        voBorrowListReq.setPageIndex(pageIndex);
        voBorrowListReq.setPageSize(pageSize);
        voBorrowListReq.setType(type);
        return borrowBiz.findAll(voBorrowListReq);
    }

    @ApiOperation("标信息")
    @GetMapping("v2/info/{borrowId}")
    public ResponseEntity<VoViewBorrowInfoWarpRes> getByBorrowId(@PathVariable Long borrowId) {
        return borrowBiz.info(borrowId);
    }
    @ApiOperation("标信息")
    @GetMapping("pc/v2/info/{borrowId}")
    public ResponseEntity<VoViewBorrowInfoWarpRes> pcgetByBorrowId(@PathVariable Long borrowId) {
        return borrowBiz.info(borrowId);
    }



    @ApiOperation("标简介")
    @GetMapping("v2/desc/{borrowId}")
    public ResponseEntity<VoViewVoBorrowDescWarpRes> desc(@PathVariable Long borrowId) {
        return borrowBiz.desc(borrowId);
    }
    @ApiOperation("pc：标简介")
    @GetMapping("pc/v2/desc/{borrowId}")
    public ResponseEntity<VoViewVoBorrowDescWarpRes> pcDesc(@PathVariable Long borrowId) {
        return borrowBiz.desc(borrowId);
    }


    @ApiOperation(value = "标合同")
    @GetMapping(value = "/pub/borrowProtocol/{borrowId}")
    public ResponseEntity<String> takeRatesDesc(@PathVariable Long borrowId,
                                                @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId){
        Map<String,Object>paramMaps= borrowBiz.contract(borrowId,userId);
        String content = thymeleafHelper.build("borrowProtocol",paramMaps) ;
        return ResponseEntity.ok(content);
    }

    @ApiOperation(value = "pc:标合同")
    @GetMapping(value = "pc/pub/borrowProtocol/{borrowId}")
    public ResponseEntity<Map<String,Object>> takeRatesDesc(@PathVariable Long borrowId /*,
                                               @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId*/){
        Map<String,Object>paramMaps= borrowBiz.pcContract(borrowId,901L);
        return ResponseEntity.ok(paramMaps);
    }



    @ApiOperation(value = "pc：招标中统计")
    @GetMapping(value = "pc/v2/statistics")
    public ResponseEntity<VoViewBorrowStatisticsWarpRes> statistics(){
        return borrowBiz.statistics();
    }

    /**
     * 新增净值借款
     *
     * @param voAddNetWorthBorrow
     * @return
     */
    @PostMapping("/addNetWorth")
    @ApiOperation("发布净值借款")
    public ResponseEntity<VoBaseResp> addNetWorth(@Valid @ModelAttribute VoAddNetWorthBorrow voAddNetWorthBorrow, @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) throws Exception {
        voAddNetWorthBorrow.setUserId(userId);
        return borrowBiz.addNetWorth(voAddNetWorthBorrow);
    }

    /**
     * 取消借款
     *
     * @param voCancelBorrow
     * @return
     */
    @PostMapping("/cancelBorrow")
    @ApiOperation("取消借款")
    public ResponseEntity<VoBaseResp> cancelBorrow(@Valid @ModelAttribute VoCancelBorrow voCancelBorrow, @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        voCancelBorrow.setUserId(userId);
        return borrowBiz.cancelBorrow(voCancelBorrow);
    }



}
