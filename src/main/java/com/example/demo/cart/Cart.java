package com.example.demo.cart;

import com.example.demo.enums.DiscountEnum;
import com.example.demo.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author lqp0817@gmail.com
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    private Product product;

    private Integer num;

    private BigDecimal subTotal;

    private BigDecimal savedPrice;

    private Integer savedNum;

    private DiscountEnum discount;
}
