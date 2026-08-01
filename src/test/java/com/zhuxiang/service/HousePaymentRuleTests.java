package com.zhuxiang.service;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.HousePaymentRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HousePaymentRuleTests {

    @Test
    void calculatesDepositFromPaymentMethod() {
        assertThat(HousePaymentRule.calculateDeposit(280000, "无押金月付"))
                .isZero();
        assertThat(HousePaymentRule.calculateDeposit(280000, "押一付三"))
                .isEqualTo(280000);
        assertThat(HousePaymentRule.calculateDeposit(280000, "押二付一"))
                .isEqualTo(560000);
    }

    @Test
    void normalizesLegacyPaymentMethodNames() {
        assertThat(HousePaymentRule.normalize("半年付")).isEqualTo("押一付六");
        assertThat(HousePaymentRule.normalize("押一付年")).isEqualTo("押一付十二");
        assertThat(HousePaymentRule.normalize("免押")).isEqualTo("无押金月付");
    }

    @Test
    void rejectsUnsupportedPaymentMethod() {
        assertThatThrownBy(() ->
                HousePaymentRule.calculateDeposit(280000, "双方另行约定"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).contains("不支持的付款方式"));
    }
}
