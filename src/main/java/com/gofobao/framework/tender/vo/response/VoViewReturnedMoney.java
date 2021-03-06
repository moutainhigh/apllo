package com.gofobao.framework.tender.vo.response;

import com.gofobao.framework.tender.vo.request.ReturnedMoney;
import lombok.Data;

import java.util.List;

/**
 * Created by admin on 2017/6/2.
 */

@Data
public class VoViewReturnedMoney {

    private Integer orderCount;

    private String collectionMoneySum;

    private  List<ReturnedMoney> returnedMonies;
}
