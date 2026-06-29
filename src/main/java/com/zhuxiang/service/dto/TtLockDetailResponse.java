package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * TTLock 门锁详情的最小安全映射，不接收管理员密码和控制密钥。
 */
public class TtLockDetailResponse {

    private Integer keyboardPwdVersion;
    private Long timezoneRawOffset;

    @JsonAlias({"errcode", "errorCode", "error_code"})
    private Integer errcode;

    @JsonAlias({"errmsg", "errorMsg", "error_msg", "description"})
    private String errmsg;

    public Integer getKeyboardPwdVersion() {
        return keyboardPwdVersion;
    }

    public void setKeyboardPwdVersion(Integer keyboardPwdVersion) {
        this.keyboardPwdVersion = keyboardPwdVersion;
    }

    public Long getTimezoneRawOffset() {
        return timezoneRawOffset;
    }

    public void setTimezoneRawOffset(Long timezoneRawOffset) {
        this.timezoneRawOffset = timezoneRawOffset;
    }

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    /** 判断详情接口是否成功返回版本和时区。 */
    public boolean success() {
        return (errcode == null || errcode == 0)
                && keyboardPwdVersion != null
                && timezoneRawOffset != null;
    }
}
