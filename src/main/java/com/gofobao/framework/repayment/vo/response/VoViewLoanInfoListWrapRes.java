package com.gofobao.framework.repayment.vo.response;

import com.gofobao.framework.core.vo.VoBaseResp;
import lombok.Data;

/**
 * Created by admin on 2017/6/6.
 */
@Data
public class VoViewLoanInfoListWrapRes extends VoBaseResp {
  private  VoViewLoanList  voLoanInfoList =new VoViewLoanList();
}
