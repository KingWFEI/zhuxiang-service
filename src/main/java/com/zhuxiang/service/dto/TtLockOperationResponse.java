package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** 通通锁无业务数据操作的通用响应。 */
public class TtLockOperationResponse {

    @JsonAlias({"errcode", "errorCode", "error_code"})
    private Integer errcode;

    @JsonAlias({"errmsg", "errorMsg", "error_msg", "description"})
    private String errmsg;

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

    public boolean success() {
        return errcode == null || errcode == 0;
    }
}
