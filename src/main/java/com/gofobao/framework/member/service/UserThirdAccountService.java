package com.gofobao.framework.member.service;

import com.gofobao.framework.member.entity.UserThirdAccount;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Created by Max on 17/5/22.
 */
public interface UserThirdAccountService {

    /**
     * 根据用户Id查询用户存管信息
     * @param id
     * @return
     */
    UserThirdAccount findByUserId(Long id);

    /**
     * 保存存管账户
     * @param entity
     * @return
     */
    Long save(UserThirdAccount entity);

    UserThirdAccount findTopByCardNo(String account);

    UserThirdAccount findByMobile(String phone);

    void deleteByUserId(Long userId);


    /**
     * 查询已被删除的用户
     * @param userId
     * @return
     */
    UserThirdAccount findByDelUseid(Long userId);

    List<UserThirdAccount> findByAll();


    List<UserThirdAccount> findList(Specification<UserThirdAccount> userThirderAccountSpe);


}
