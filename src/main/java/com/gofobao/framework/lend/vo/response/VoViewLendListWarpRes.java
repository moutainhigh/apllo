package com.gofobao.framework.lend.vo.response;

import com.gofobao.framework.core.vo.VoBaseResp;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by admin on 2017/6/6.
 */
@Data
public class VoViewLendListWarpRes extends VoBaseResp {
    private List<VoViewLend> voViewLends= Lists.newArrayList();
    @ApiModelProperty("总记录数")
    private Integer totalCount=0;

}
