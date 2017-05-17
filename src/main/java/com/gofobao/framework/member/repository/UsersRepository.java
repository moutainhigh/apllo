package com.gofobao.framework.member.repository;

import com.gofobao.framework.member.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by Max on 17/5/16.
 */
@Repository
public interface UsersRepository extends JpaRepository<Users,Long>{

    List<Users> findByUsernameOrPhoneOrEmail(String username, String phone, String email);
}