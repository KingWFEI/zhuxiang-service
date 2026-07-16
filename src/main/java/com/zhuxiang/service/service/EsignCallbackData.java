package com.zhuxiang.service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * e签宝回调通知数据。
 */
public class EsignCallbackData {
    private String signFlowId;
    private Integer signFlowStatus;
    private String contractNum;
    private Long signFlowFinishTime;

    public String getSignFlowId() { return signFlowId; }
    public void setSignFlowId(String v) { this.signFlowId = v; }
    public Integer getSignFlowStatus() { return signFlowStatus; }
    public void setSignFlowStatus(Integer v) { this.signFlowStatus = v; }
    public String getContractNum() { return contractNum; }
    public void setContractNum(String v) { this.contractNum = v; }
    public Long getSignFlowFinishTime() { return signFlowFinishTime; }
    public void setSignFlowFinishTime(Long v) { this.signFlowFinishTime = v; }

    public static LocalDateTime toLocalDateTime(Long millis) {
        if (millis == null) return null;
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
    }
}
