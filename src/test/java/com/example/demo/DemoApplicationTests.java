package com.example.demo;

import com.example.demo.cart.Cart;
import com.example.demo.enums.DiscountEnum;
import com.example.demo.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
class DemoApplicationTests {

    @Mock
    private DemoApplication demoApplication = new DemoApplication();

    /*
       1001 - 苹果  -  单价 ： 1    优惠： 买二赠一 和 95折
       1002 - 橘子  -  单价 ： 2    优惠：无
       1003 - 香蕉  -  单价 ： 3 ，优惠：95折
       1004 - 西瓜  -  单价 ： 4     优惠：无
       1005 - 橙子  -  单价 ： 5     优惠：买二赠一
       1006 - 草莓  -  单价 ： 6     优惠：无
       1007 - 葡萄  -  单价 ： 7     优惠：95折
    * */

    @Test
    public void should_return_empty() {
        //错误的id
        List<String> ids = List.of("错误的id");
        List<Cart> carts = demoApplication.buyFruit(ids);
        org.junit.Assert.assertTrue(ObjectUtils.isEmpty(carts));
        String result = demoApplication.printTicket(carts);
        org.junit.Assert.assertTrue(ObjectUtils.isEmpty(result));
    }

    @Test
    public void should_ignore_invalid_split_string() {
        //错误的id
        List<String> ids = List.of("1001-无效的数量");
        List<Cart> carts = demoApplication.buyFruit(ids);
        org.junit.Assert.assertTrue(carts != null && carts.size() == 1 && carts.get(0).getProduct().getId().equals("1001"));
        demoApplication.printTicket(carts);
    }

    @Test
    public void should_return_match_num_and_price_and_contains_word() {
        //购买 3个苹果 (同时包含买二赠一和95折) +1个橘子+3个香蕉+6个橙子

        List<String> ids = List.of("1001", "1001", "1001", "1002", "1003-3", "1005-6");
        List<Cart> carts = demoApplication.buyFruit(ids);
        // 数量匹配
        assertEquals((3 + 1 + 3 + 6), (int) carts.stream().map(Cart::getNum).reduce(0, Integer::sum));
        //价格匹配
        assertEquals(0, carts.stream().map(Cart::getSubTotal).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.valueOf(32.55)));
        String ticket = """
                ***购物清单***
                名称：苹果，数量：3，单价：1，小计：2\s
                名称：橘子，数量：1，单价：2，小计：2\s
                名称：香蕉，数量：3，单价：3，小计：8.55 ，节省：0.45
                名称：橙子，数量：6，单价：5，小计：20\s
                --------------------------
                买二赠一商品
                名称：苹果，数量：1
                名称：橙子，数量：2
                --------------------------
                总计：32.55
                节省：11.45
                ***************
                """;
        //打印匹配 (同时包含买二赠一和95折 时 只有买二赠一生效)
        assertEquals(ticket, demoApplication.printTicket(carts));
    }

    @Test
    public void should_return_match_two_plus_one() {
        //购买 3个橙子
        Product product = Product.builder().id("1005").name("橙子").price(BigDecimal.valueOf(5)).discounts(List.of(DiscountEnum.TWO_PLUS_ONE)).remark("买二赠一").build();
        demoApplication.products = List.of(product);
        List<Cart> carts = demoApplication.buyFruit(List.of(product.getId()+"-3"));
        //size = 1
        assertEquals(carts.size(), 1);
        //数量是3
        assertEquals(carts.stream().findFirst().map(Cart::getNum), Optional.of(3));
        //只收两个的钱
        assertEquals(carts.stream().findFirst().map(Cart::getSubTotal).orElse(BigDecimal.ZERO), BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(DiscountEnum.TWO_PLUS_ONE.getBuy())));
        String ticket = """
                ***购物清单***
                名称：橙子，数量：3，单价：5，小计：10\s
                --------------------------
                买二赠一商品
                名称：橙子，数量：1
                --------------------------
                总计：10
                节省：5
                ***************
                """;
        assertEquals(ticket, demoApplication.printTicket(carts));
    }

    @Test
    public void should_return_match_percent_95() {
        //购买 1个95折的葡萄
        Product grape = Product.builder().id("1007").name("葡萄").price(BigDecimal.valueOf(7)).discounts(List.of(DiscountEnum.PERCENT_95)).remark("95折").build();
        demoApplication.products = List.of(grape);
        List<Cart> carts = demoApplication.buyFruit(List.of(grape.getId()));
        //只有一个
        assertEquals(carts.size(), 1);
        //价格是95折
        assertEquals(carts.stream().findFirst().map(Cart::getSubTotal).orElse(BigDecimal.ZERO), BigDecimal.valueOf(7).multiply(DiscountEnum.PERCENT_95.getPercent()));
        String ticket = """
                ***购物清单***
                名称：葡萄，数量：1，单价：7，小计：6.65 ，节省：0.35
                --------------------------
                                
                总计：6.65
                节省：0.35
                ***************
                """;
        assertEquals(ticket, demoApplication.printTicket(carts));
    }

    @Test
    public void should_return_non_discount() {
        //购买 无任何优惠的商品

        Product product = Product.builder().id("1002").name("橘子").price(BigDecimal.valueOf(2)).discounts(List.of()).remark("无优惠").build();
        demoApplication.products = List.of(product);
        List<Cart> carts = demoApplication.buyFruit(List.of(product.getId()));
        org.junit.Assert.assertTrue(carts.size() == 1 && carts.get(0).getProduct().getId().equals("1002"));
        //价格是没有折扣的
        org.junit.Assert.assertTrue(carts.size() == 1 && carts.get(0).getSubTotal().compareTo(BigDecimal.valueOf(2)) == 0);
        String ticket = """
                ***购物清单***
                名称：橘子，数量：1，单价：2，小计：2\s
                --------------------------

                总计：2
                ***************
                """;
        Assert.isTrue(ticket.equals(demoApplication.printTicket(carts)), "");

    }

}
