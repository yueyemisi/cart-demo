package com.example.demo.product;

import com.example.demo.enums.DiscountEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author lqp0817@gmail.com
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    private String id;

    private String name;

    private BigDecimal price;

    private List<DiscountEnum> discounts;

    private String remark;
}

