package com.gofobao.framework.finance.biz;

import com.gofobao.framework.finance.vo.response.VoViewFinanceTenderDetailWarpRes;
import com.gofobao.framework.system.vo.request.VoFinanceDetailReq;
import com.gofobao.framework.system.vo.response.VoViewFinanceReturnMoneyWarpRes;
import com.gofobao.framework.tender.vo.request.VoDetailReq;
import com.gofobao.framework.tender.vo.request.VoFinanceInvestListReq;
import com.gofobao.framework.tender.vo.request.VoInvestListReq;
import com.gofobao.framework.tender.vo.response.*;
import org.springframework.http.ResponseEntity;

/**
 * Created by admin on 2017/6/6.
 */
public interface MyFinanceInvestBiz {


    /**
     * 回款中列表
     * @param voInvestListReq
     * @return
     */
    ResponseEntity<VoViewFinanceBackMoneyListWarpRes> backMoneyList(VoFinanceInvestListReq voInvestListReq);

    /**
     * 投标中列表
     * @param voInvestListReq
     * @return
     */
    ResponseEntity<VoViewFinanceBiddingListWrapRes> biddingList(VoFinanceInvestListReq voInvestListReq);

    /**
     * 已结清
     * @param voInvestListReq
     * @return
     */
    ResponseEntity<VoViewFinanceSettleWarpRes> settleList(VoFinanceInvestListReq voInvestListReq);


    /**
     * 已结清 and 回款中 详情
     * @param voDetailReq
     * @return
     */
    ResponseEntity<VoViewFinanceTenderDetailWarpRes> tenderDetail(VoFinanceDetailReq voDetailReq) ;

    /**
     * 回款详情
     * @param voDetailReq
     * @return
     */
    ResponseEntity<VoViewFinanceReturnMoneyWarpRes> infoList(VoFinanceDetailReq voDetailReq) ;

}
