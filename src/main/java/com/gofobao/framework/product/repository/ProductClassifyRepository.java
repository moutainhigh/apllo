package com.gofobao.framework.product.repository;

import com.gofobao.framework.product.entity.ProductClassify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Created by master on 2017/10/23.
 */
@Repository
public interface ProductClassifyRepository extends JpaRepository<ProductClassify, Long>, JpaSpecificationExecutor<ProductClassify> {

}
