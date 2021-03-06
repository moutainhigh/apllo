package com.gofobao.framework.tender.vo.response;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by admin on 2017/6/6.
 */
@Data
public class VoViewBackMoneyListWarpRes extends VoBaseResp {

    private List<VoViewBackMoney> voViewBackMonies= Lists.newArrayList();

    @ApiModelProperty("总记录数")
    private Integer totalCount=0;
}
