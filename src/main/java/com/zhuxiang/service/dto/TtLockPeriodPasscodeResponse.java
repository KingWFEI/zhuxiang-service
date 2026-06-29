package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * TTLock 生成期限密码接口响应，日志输出时始终隐藏明文密码。
 */
@Getter
@Setter
@ToString(exclude = "keyboardPwd")
public class TtLockPeriodPasscodeResponse {

    private String keyboardPwd;
    private Long keyboardPwdId;

    @JsonAlias({"errcode", "errorCode", "error_code"})
    private Integer errcode;

    @JsonAlias({"errmsg", "errorMsg", "error_msg", "description"})
    private String errmsg;

    /** 判断平台是否返回完整的成功结果。 */
    public boolean success() {
        return (errcode == null || errcode == 0)
                && StringUtils.hasText(keyboardPwd)
                && keyboardPwdId != null;
    }
}
