package com.zhuxiang.service.event;

/**
 * 租约已经提交为生效状态的领域事件。
 */
public record LeaseActivatedEvent(String leaseId) {
}
