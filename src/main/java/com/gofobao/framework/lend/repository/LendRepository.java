package com.gofobao.framework.lend.repository;

import com.gofobao.framework.lend.entity.Lend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

/**
 * Created by Zeke on 2017/5/26.
 */
@Repository
public interface LendRepository extends JpaRepository<Lend,Long>,JpaSpecificationExecutor<Lend>{

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Lend findById(Long id);
}
