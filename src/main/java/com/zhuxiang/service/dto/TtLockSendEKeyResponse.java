package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * TTLock发送eKey接口响应。
 */
public class TtLockSendEKeyResponse {

    /**
     * TTLock平台生成的租客eKey ID。
     */
    private Long keyId;

    /**
     * TTLock业务错误码，0表示成功。
     */
    @JsonAlias({"errcode", "errorCode", "error_code"})
    private Integer errcode;

    /**
     * TTLock业务错误信息。
     */
    @JsonAlias({"errmsg", "errorMsg", "error_msg", "description"})
    private String errmsg;

    public Long getKeyId() {
        return keyId;
    }

    public void setKeyId(Long keyId) {
        this.keyId = keyId;
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

    /**
     * 判断TTLock是否返回业务成功。
     */
    public boolean success() {
        return (errcode == null || errcode == 0) && keyId != null;
    }
}
