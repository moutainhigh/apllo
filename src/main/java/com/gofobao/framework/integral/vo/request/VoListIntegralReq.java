package com.gofobao.framework.integral.vo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gofobao.framework.common.page.Page;
import com.gofobao.framework.core.vo.VoBaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Zeke on 2017/5/22.
 */
@ApiModel
@Data
public class VoListIntegralReq extends Page{

    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private Long userId;
}
