package com.zhuxiang.service.common;

import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 房源付款方式及押金计算规则。房源押金由月租金和付款方式派生，客户端传入的押金不作为入库依据。
 */
public final class HousePaymentRule {

    private static final String DEFAULT_METHOD = "押一付一";

    private static final Map<String, Rule> RULES = Map.of(
            "无押金月付", new Rule("无押金月付", 0, 1),
            "押一付一", new Rule("押一付一", 1, 1),
            "押一付三", new Rule("押一付三", 1, 3),
            "押一付六", new Rule("押一付六", 1, 6),
            "押一付十二", new Rule("押一付十二", 1, 12),
            "押二付一", new Rule("押二付一", 2, 1)
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("无押金", "无押金月付"),
            Map.entry("免押", "无押金月付"),
            Map.entry("零押金", "无押金月付"),
            Map.entry("月付", "押一付一"),
            Map.entry("季付", "押一付三"),
            Map.entry("半年付", "押一付六"),
            Map.entry("年付", "押一付十二"),
            Map.entry("押一付年", "押一付十二")
    );

    private HousePaymentRule() {
    }

    public static Rule require(String paymentMethod) {
        String normalized = normalize(paymentMethod);
        Rule rule = RULES.get(normalized);
        if (rule == null) {
            throw BusinessException.badRequest("不支持的付款方式：" + paymentMethod);
        }
        return rule;
    }

    public static String normalize(String paymentMethod) {
        String value = StringUtils.hasText(paymentMethod)
                ? paymentMethod.trim() : DEFAULT_METHOD;
        return ALIASES.getOrDefault(value, value);
    }

    public static int calculateDeposit(int monthlyRent, String paymentMethod) {
        if (monthlyRent < 0) {
            throw BusinessException.badRequest("月租金不能小于0");
        }
        return Math.multiplyExact(monthlyRent, require(paymentMethod).depositMonths());
    }

    public record Rule(String paymentMethod, int depositMonths, int paymentMonths) {
    }
}
