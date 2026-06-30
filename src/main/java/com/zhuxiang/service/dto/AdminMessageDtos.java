package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminMessageDtos {

    private AdminMessageDtos() {
    }

    @Schema(description = "管理端发送系统消息请求")
    public record SendSystemMessageRequest(
            @Schema(description = "接收用户 ID 列表，重复 ID 将自动去重", example = "[\"user-1\", \"user-2\"]")
            @NotEmpty(message = "接收用户不能为空")
            @Size(max = 100, message = "单次最多向 100 个用户发送消息")
            List<@Valid @NotBlank(message = "接收用户 ID 不能为空") String> userIds,

            @Schema(description = "消息标题", example = "系统维护通知")
            @NotBlank(message = "消息标题不能为空")
            @Size(max = 100, message = "消息标题不能超过 100 个字符")
            String title,

            @Schema(description = "消息内容", example = "系统将于今晚进行维护")
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 1000, message = "消息内容不能超过 1000 个字符")
            String content,

            @Schema(description = "点击消息后的动作类型；不传时为 none", example = "none")
            @Size(max = 30, message = "动作类型不能超过 30 个字符")
            String actionType,

            @Schema(description = "动作目标，例如业务 ID 或路由；可为空", example = "")
            @Size(max = 500, message = "动作目标不能超过 500 个字符")
            String actionTarget
    ) {
    }
}
