package com.example.demo;

import com.example.demo.cart.Cart;
import com.example.demo.enums.DiscountEnum;
import com.example.demo.product.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
@RequestMapping
@Validated
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    private static final String TEMPLATE = """
            ***购物清单***
            %s
            ***************
            """;

    public List<Product> products =  List.of(
            Product.builder().id("1001").name("苹果").price(BigDecimal.valueOf(1)).discounts(List.of(DiscountEnum.TWO_PLUS_ONE, DiscountEnum.PERCENT_95)).remark("买二赠一 和 95折").build(),
            Product.builder().id("1002").name("橘子").price(BigDecimal.valueOf(2)).discounts(List.of()).remark("").build(),
            Product.builder().id("1003").name("香蕉").price(BigDecimal.valueOf(3)).discounts(List.of(DiscountEnum.PERCENT_95)).remark("95折").build(),
            Product.builder().id("1004").name("西瓜").price(BigDecimal.valueOf(4)).discounts(List.of()).remark("").build(),
            Product.builder().id("1005").name("橙子").price(BigDecimal.valueOf(5)).discounts(List.of(DiscountEnum.TWO_PLUS_ONE)).remark("买二赠一").build(),
            Product.builder().id("1006").name("草莓").price(BigDecimal.valueOf(6)).discounts(List.of()).remark("").build(),
            Product.builder().id("1007").name("葡萄").price(BigDecimal.valueOf(7)).discounts(List.of(DiscountEnum.PERCENT_95)).remark("95折").build()
    );

    @GetMapping({"/", ""})
    public Object ticket(@RequestParam(value = "ids", required = false) List<String> ids) {
        if (ObjectUtils.isEmpty(ids)) {
            return products;
        }
        List<Cart> carts = buyFruit(ids);
        if (ObjectUtils.isEmpty(carts)) {
            return "No Product Found,Please Enter Correct Product ID";
        }

        return printTicket(carts).replaceAll("\\n", "<br>");
    }

    public String printTicket(List<Cart> carts) {
        if (ObjectUtils.isEmpty(carts)) {
            return "";
        }
        BigDecimal total = carts.stream()
                .map(Cart::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaved = carts.stream()
                .map(Cart::getSavedPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String details = carts.stream()
                .map(c -> String.format("名称：%s，数量：%s，单价：%s，小计：%s %s",
                        c.getProduct().getName(), c.getNum(), c.getProduct().getPrice(), c.getSubTotal(), c.getDiscount() != DiscountEnum.PERCENT_95 ? "" : ("，节省：" + c.getSavedPrice().stripTrailingZeros().toPlainString())))
                .collect(Collectors.joining("\n"));
        details = ObjectUtils.isEmpty(details) ? "" : details.concat("\n").concat("--------------------------").concat("\n");

        String savedNums = carts.stream()
                .filter(c -> Objects.nonNull(c.getSavedNum()) && c.getSavedNum() > 0)
                .map(c -> String.format("名称：%s，数量：%s", c.getProduct().getName(), c.getSavedNum()))
                .collect(Collectors.joining("\n"));
        savedNums = ObjectUtils.isEmpty(savedNums) ? "" : "买二赠一商品\n" + savedNums.concat("\n").concat("--------------------------");

        String summary = "\n"
                + "总计：" + total.stripTrailingZeros().toPlainString()
                + (totalSaved.compareTo(BigDecimal.ZERO) == 0 ? "" : ("\n节省：" + totalSaved.stripTrailingZeros().toPlainString()));

        String format = String.format(TEMPLATE, details + savedNums + summary);
        System.out.println("\n" + format + "\n");
        return format;
    }

    public List<Cart> buyFruit(List<String> ids) {
        List<Cart> carts = new ArrayList<>();

        for (String id : ids) {
            int num = 1;
            if (id.contains("-")) {
                String[] split = id.split("-");
                id = split[0];
                try {
                    num = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    // 转换出错时当做1个算
                }
            }
            String finalId = id;
            Optional<Product> optionalProduct = products.stream().filter(product -> product.getId().equals(finalId)).findFirst();
            if (optionalProduct.isEmpty()) {
                continue;
            }
            Product product = optionalProduct.get();
            Optional<Cart> optionalCart = carts.stream()
                    .filter(c -> Objects.equals(c.getProduct().getId(), product.getId()))
                    .findFirst();
            //优先选择 买二赠一 其次选择 95折
            Optional<DiscountEnum> optionalDiscount = product.getDiscounts().stream()
                    .filter(discountEnum -> discountEnum == DiscountEnum.TWO_PLUS_ONE)
                    .findFirst()
                    .or(() -> product.getDiscounts().stream()
                            .filter(discountEnum -> discountEnum == DiscountEnum.PERCENT_95)
                            .findFirst());
            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(num));
            Cart cart;
            //购物车为空是直接添加
            if (optionalCart.isEmpty()) {
                cart = Cart.builder().product(product).num(num).subTotal(subTotal).build();
                carts.add(cart);
                log.info("购物车加入：{} - {} ", num, product.getName());
            } else {
                //已有同类时加数量、改小计
                cart = optionalCart.get();
                cart.setNum(cart.getNum() + num);
                cart.setSubTotal(cart.getSubTotal().add(subTotal));
                log.info("购物车更新：{} - {}", cart.getNum(), product.getName());
            }
            optionalDiscount.ifPresent(discount -> {
                BigDecimal originalTotal = BigDecimal.valueOf(cart.getNum()).multiply(cart.getProduct().getPrice());
                cart.setDiscount(discount);
                switch (discount) {
                    case PERCENT_95 -> cart.setSubTotal(originalTotal.multiply(discount.getPercent()));
                    case TWO_PLUS_ONE -> {
                        int remainder = cart.getNum() / (discount.getBuy() + discount.getGift());
                        int actualPayNum = remainder == 0 ? cart.getNum() : (cart.getNum() - remainder);
                        cart.setSubTotal(BigDecimal.valueOf(actualPayNum).multiply(cart.getProduct().getPrice()));
                        cart.setSavedNum(cart.getNum() - actualPayNum);
                    }
                    default -> {
                    }
                }
                cart.setSavedPrice(originalTotal.subtract(cart.getSubTotal()));
            });
        }
        return carts;
    }


}
