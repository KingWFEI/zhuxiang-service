package com.zhuxiang.service.dto;

/**
 * 租客查看期限密码响应。toString 永不输出明文密码。
 */
public record LeaseLockPasscodeResponse(
        String leaseId,
        String smartLockId,
        String roomName,
        String passcode,
        String passcodeType,
        String startTime,
        String endTime,
        String status,
        String firstUseNotice
) {
    @Override
    public String toString() {
        return "LeaseLockPasscodeResponse[leaseId=" + leaseId
                + ", smartLockId=" + smartLockId
                + ", roomName=" + roomName
                + ", passcode=***, passcodeType=" + passcodeType
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", status=" + status
                + ", firstUseNotice=" + firstUseNotice + "]";
    }
}
