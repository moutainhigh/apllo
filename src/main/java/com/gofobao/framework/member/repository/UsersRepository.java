package com.gofobao.framework.member.repository;

import com.gofobao.framework.member.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * Created by Max on 17/5/16.
 */
@Repository
public interface UsersRepository extends JpaRepository<Users, Long>, JpaSpecificationExecutor<Users> {

    List<Users> findByUsernameOrPhoneOrEmail(String username, String phone, String email);

    /**
     * 通过手机号码查找会员
     */
    List<Users> findByPhone(String phone);

    /**
     * 通过邮箱查找会员
     */
    List<Users> findByEmail(String email);

    /**
     * 带锁查询会员
     **/
    Users findById(Long userId);

    /**
     * 通过用户名查找会员
     */
    List<Users> findByUsername(String userName);

    /**
     * 根据邀请码获取用户信息
     */
    List<Users> findByInviteCodeOrPhoneOrUsername(String inviteCode, String phone,String username);

    /**
     * @return
     */
    List<Users> findByIdIn(List<Long> userIds);

    /**
     * 邀请好友列表
     *
     * @param parentId
     * @return
     */
    Page<Users> findByParentId(Long parentId, Pageable pageable);

    @Query("SELECT COUNT (user.id) FROM Users user")
    Long registerUserCount();

}
