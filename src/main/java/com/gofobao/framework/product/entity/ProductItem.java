package com.gofobao.framework.product.entity;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Zeke on 2017/11/9.
 */
@Entity
@Table(name = "gfb_product_item")
@Data
public class ProductItem {
    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private Long price;
    private Long discountPrice;
    private String afterSalesService;
    private String imgUrl;
    private String details;
    @Column(name = "q_and_a")
    private String qAndA;
    private Integer inventory;
    private Boolean isDel;
    private Boolean isEnable;
    private Date enableAt;
    private Date createAt;
    private Date updateAt;
}
