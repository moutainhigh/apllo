package com.gofobao.framework.collection.controller.web;

import com.gofobao.framework.collection.biz.PaymentBiz;
import com.gofobao.framework.collection.vo.request.OrderListReq;
import com.gofobao.framework.collection.vo.request.VoCollectionOrderReq;
import com.gofobao.framework.collection.vo.request.VoOrderDetailReq;
import com.gofobao.framework.collection.vo.response.VoViewCollectionDaysWarpRes;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderListWarpResp;
import com.gofobao.framework.collection.vo.response.VoViewOrderDetailResp;
import com.gofobao.framework.collection.vo.response.web.VoViewCollectionListWarpRes;
import com.gofobao.framework.security.contants.SecurityContants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by admin on 2017/5/31.
 */
@Api(description = "pc:回款明细")
@RestController
@RequestMapping("payment/pc")
public class WebPaymentController {

    @Autowired
    private PaymentBiz paymentBiz;

    @ApiOperation("回款明细-回款列表 time:2017-05-06")
    @GetMapping("/v2/order/list/{time}")
    public ResponseEntity<VoViewCollectionOrderListWarpResp> collectionOrderList(@PathVariable("time") String time,
                                                                                 @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId) {
        VoCollectionOrderReq voCollectionOrderReq = new VoCollectionOrderReq();
        voCollectionOrderReq.setUserId(userId);
        voCollectionOrderReq.setTime(time);
        return paymentBiz.orderList(voCollectionOrderReq);
    }

    @ApiOperation("回款明细-回款详情")
    @GetMapping("/v2/order/detail/{collectionId}")
    public ResponseEntity<VoViewOrderDetailResp> orderDetail(@PathVariable("collectionId") Long collectionId/*,
                                                             @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId*/) {
        VoOrderDetailReq voOrderDetailReq = new VoOrderDetailReq();
        voOrderDetailReq.setCollectionId(collectionId);
        voOrderDetailReq.setUserId(901L);
        return paymentBiz.orderDetail(voOrderDetailReq);
    }
    @ApiOperation("回款明细")
    @GetMapping("/v2/day/collection/{pageIndex}/{pageSize}")
    public ResponseEntity<VoViewCollectionListWarpRes> orderDetail(@PathVariable("pageIndex") Integer pageIndex,
                                                             @PathVariable("pageSize") Integer pageSize/*,
                                                             @ApiIgnore @RequestAttribute(SecurityContants.USERID_KEY) Long userId*/) {
        OrderListReq orderListReq=new OrderListReq();
        orderListReq.setUserId(901L);
        orderListReq.setPageIndex(pageIndex);
        orderListReq.setPageSize(pageSize);
        return paymentBiz.pcOrderList(orderListReq);
    }



}
