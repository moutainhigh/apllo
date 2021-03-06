package com.gofobao.framework.asset.biz.impl;

import com.github.wenhao.jpa.Specifications;
import com.gofobao.framework.asset.biz.UnionLineNumberBiz;
import com.gofobao.framework.asset.entity.UnionLineNumber;
import com.gofobao.framework.asset.service.UnionLineNumberService;
import com.gofobao.framework.asset.vo.request.VoUnionLineNoReq;
import com.gofobao.framework.asset.vo.response.pc.UnionLineNo;
import com.gofobao.framework.asset.vo.response.pc.UnionLineNoWarpRes;
import com.gofobao.framework.core.vo.VoBaseResp;
import com.gofobao.framework.member.biz.UserBiz;
import com.gofobao.framework.member.vo.response.VoBasicUserInfoResp;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Created by admin on 2017/8/21.
 */
@Service
public class UnionLineUnmberBizImpl implements UnionLineNumberBiz {

    @Autowired
    private UnionLineNumberService unionLineNumberService;
    @Autowired
    private UserBiz userBiz;

    @Override
    public ResponseEntity<UnionLineNoWarpRes> list(VoUnionLineNoReq unionLineNoReq) {
        UnionLineNoWarpRes warpRes = VoBaseResp.ok("查询成功", UnionLineNoWarpRes.class);
        ResponseEntity<VoBasicUserInfoResp> responseEntity = userBiz.userInfo(unionLineNoReq.getUserId());
        VoBasicUserInfoResp userInfoResp = responseEntity.getBody();
        if (Objects.isNull(userInfoResp) || StringUtils.isEmpty(userInfoResp.getSubbranch())) {
            return ResponseEntity.ok(warpRes);
        }
        Specification<UnionLineNumber> specification = Specifications.<UnionLineNumber>and()
                .eq(!StringUtils.isEmpty(unionLineNoReq.getCityId()), "city", unionLineNoReq.getCityId())
                .eq(!StringUtils.isEmpty(unionLineNoReq.getProvinceId()), "province", unionLineNoReq.getProvinceId())
                .like("bankName", "%" + userInfoResp.getSubbranch() + "%")
                .like(!StringUtils.isEmpty(unionLineNoReq.getKeyword()), "address", "%" + unionLineNoReq.getKeyword() + "%")
                .build();
        try {
            Page<UnionLineNumber> unionLineNumbers = unionLineNumberService.findAll(specification, new PageRequest(unionLineNoReq.getPageIndex(), unionLineNoReq.getPageSize(), new Sort(Sort.Direction.DESC, "id")));
            List<UnionLineNumber> lineNumbers = unionLineNumbers.getContent();
            if (CollectionUtils.isEmpty(lineNumbers)) {
                return ResponseEntity.ok(warpRes);
            }
            warpRes.setTotalCount(unionLineNumbers.getTotalElements());
            List<UnionLineNo> unionLineNos = Lists.newArrayList();
            lineNumbers.forEach(p -> {
                UnionLineNo unionLineNo = new UnionLineNo();
                unionLineNo.setAddress(p.getAddress());
                unionLineNo.setBankName(p.getBankName());
                unionLineNo.setNumber(p.getNumber());
                unionLineNo.setId(p.getId());
                unionLineNos.add(unionLineNo);
            });
            warpRes.setUnionLineNos(unionLineNos);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.ok(warpRes);
    }
}
