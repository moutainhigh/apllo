package com.gofobao.framework.tender.vo.request;

import com.gofobao.framework.core.vo.VoBaseReq;
import com.gofobao.framework.helper.MoneyHelper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Zeke on 2017/7/31.
 */
@ApiModel
@Data
public class VoBuyTransfer extends VoBaseReq {
    @ApiModelProperty(name = "userId", hidden = true)
    private Long userId;
    @ApiModelProperty(value = "债权转让记录id", required = true)
    @NotNull(message = "债权转让记录id不能为空!")
    private Long transferId;
    @ApiModelProperty(name = "buyMoney", value = "购买债权金额", dataType = "double", required = true)
    private Double buyMoney;
    @ApiModelProperty(hidden = true)
    private Boolean auto = false;
    @ApiModelProperty(hidden = true)
    private Integer autoOrder = 0;

    @NotNull(message = "购买债权金额不能为空!")
    public Double getBuyMoney() {
        return MoneyHelper.round(buyMoney, 0);
    }

    public void setBuyMoney(Double buyMoney) {
        this.buyMoney = MoneyHelper.round(buyMoney * 100.0, 0);
    }
}
