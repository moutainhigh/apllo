package com.gofobao.framework.product.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by Zeke on 2017/11/9.
 */
@Entity
@Table(name = "gfb_product_sku_classify")
@Data
public class ProductSkuClassify {
    @Id
    @GeneratedValue
    private Long id;
    private Integer no;
    private String name;
    private String remark;
    private Boolean isDel;
    private Date createAt;
    private Date updateAt;
}
