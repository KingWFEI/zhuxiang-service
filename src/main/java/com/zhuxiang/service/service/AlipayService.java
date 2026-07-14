package com.zhuxiang.service.service;

import java.util.Map;

/**
 * 支付宝支付服务。
 * 开发阶段使用沙箱 H5 支付，上线后切换到 APP 支付只需改配置 + 返回 payType。
 */
public interface AlipayService {

    /**
     * 构建支付宝 H5 支付页面 URL。
     *
     * @param outTradeNo 商户订单号（对应 payment_record.paymentNo）
     * @param totalAmount 支付金额，单位：分
     * @param subject 订单标题
     * @return 支付宝支付页面完整 URL
     */
    String buildH5PayUrl(String outTradeNo, int totalAmount, String subject);

    /**
     * 验证支付宝异步通知签名并解析参数。
     *
     * @param params 支付宝 POST 过来的所有参数
     * @return 验签通过返回解析后的通知数据；验签失败返回 null
     */
    AlipayNotifyResult verifyNotify(Map<String, String> params);

    /**
     * 主动查询支付宝订单状态。
     *
     * @param outTradeNo 商户订单号
     * @return 查询成功返回 AlipayNotifyResult（仅 TRADE_SUCCESS 时），否则返回 null
     */
    AlipayNotifyResult queryOrder(String outTradeNo);

    /** 当前支付类型：开发返回 "h5"，上线后改为 "app" */
    default String getPayType() {
        return "h5";
    }

    /** 支付宝异步通知解析结果 */
    record AlipayNotifyResult(
            String tradeNo,
            String outTradeNo,
            String totalAmount,
            String tradeStatus,
            String buyerLogonId
    ) {}
}
