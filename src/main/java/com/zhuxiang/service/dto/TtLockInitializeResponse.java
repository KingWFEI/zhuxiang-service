package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 通通锁初始化接口响应。
 */
public class TtLockInitializeResponse {

    /**
     * 通通锁平台门锁ID。
     */
    private Long lockId;

    /**
     * 管理员eKey ID。
     */
    private Long keyId;

    /**
     * 开放平台错误码。
     */
    @JsonAlias({"errcode", "errorCode", "error_code"})
    private Integer errcode;

    /**
     * 开放平台错误信息。
     */
    @JsonAlias({"errmsg", "errorMsg", "error_msg", "description"})
    private String errmsg;

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

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
     * 判断开放平台是否返回业务成功。
     */
    public boolean success() {
        return errcode == null || errcode == 0;
    }
}
