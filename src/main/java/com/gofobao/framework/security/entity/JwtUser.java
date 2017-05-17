package com.gofobao.framework.security.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

/**
 * JWT 用户
 * Created by Max on 2017/5/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtUser implements UserDetails{
    private Long id;
    private Date updateAt ;
    private String username ;
    private String phone ;
    private String email ;
    private String password ;
    private Integer isLock ;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList() ;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        String temp = username ;
        if(Objects.isNull(temp)) temp = phone ;
        if(Objects.isNull(temp)) temp = email ;
        return temp;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isLock == 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}