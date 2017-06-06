package com.gofobao.framework.repayment.biz;

import com.gofobao.framework.collection.vo.request.VoCollectionOrderReq;
import com.gofobao.framework.collection.vo.response.VoViewCollectionOrderListResWarpRes;
import com.gofobao.framework.collection.vo.response.VoViewOrderDetailWarpRes;
import com.gofobao.framework.repayment.vo.request.VoInfoReq;
import org.springframework.http.ResponseEntity;

/**
 * Created by admin on 2017/6/5.
 */
public interface RepaymentBiz {

    /**
     *还款计划列表
     * @param voCollectionOrderReq
     * @return
     */
     ResponseEntity<VoViewCollectionOrderListResWarpRes> repaymentList(VoCollectionOrderReq voCollectionOrderReq);

    /**
     * 还款详情
     * @param voInfoReq
     * @return
     */
     ResponseEntity<VoViewOrderDetailWarpRes> info(VoInfoReq voInfoReq);
}
