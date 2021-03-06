package com.gofobao.framework.asset.biz;

import com.gofobao.framework.asset.vo.response.VoBankListResp;
import com.gofobao.framework.asset.vo.response.VoBankTypeInfoResp;
import com.gofobao.framework.member.vo.response.VoHtmlResp;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

/**
 * Created by Zeke on 2017/5/22.
 */
public interface BankAccountBiz {

    /**
     * 查找银行卡类型基本信息和限额
     *
     * @param userId
     * @param account
     * @return
     */
    ResponseEntity<VoBankTypeInfoResp> findTypeInfo(Long userId, String account);

    ResponseEntity<VoHtmlResp> credit();

    /**
     *  查询银行卡信息列表
     * @param model
     */
    void showDesc(Model model);

    /**
     * 查询用户银行卡列表
     * @param userId
     * @return
     */
    ResponseEntity<VoBankListResp> list(Long userId);

    /**
     * 获取当天充值额度
     * @param userId
     * @return
     */
    int getCashCredit4Day(Long userId);
}
