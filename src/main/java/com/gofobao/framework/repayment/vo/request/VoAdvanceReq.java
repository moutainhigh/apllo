package com.gofobao.framework.repayment.vo.request;

import com.gofobao.framework.api.model.batch_bail_repay.BailRepayRun;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by Zeke on 2017/6/12.
 */
@Data
public class VoAdvanceReq {

    private Long repaymentId;
}
