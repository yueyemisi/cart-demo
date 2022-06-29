package com.example.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * @author lqp0817@gmail.com
 **/
@Getter
@AllArgsConstructor
public enum DiscountEnum {

    PERCENT_95(BigDecimal.valueOf(0.95), null, null),

    TWO_PLUS_ONE(BigDecimal.ONE, 2, 1),

    ;

    private final BigDecimal percent;

    private final Integer buy;

    private final Integer gift;
}
