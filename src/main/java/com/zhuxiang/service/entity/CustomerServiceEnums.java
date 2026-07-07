package com.zhuxiang.service.entity;

/**
 * 智能客服相关枚举常量
 */
public final class CustomerServiceEnums {

    private CustomerServiceEnums() {}

    /** 会话状态 */
    public static final class SessionStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String CLOSED = "CLOSED";
    }

    /** 会话关闭原因 */
    public static final class ClosedReason {
        public static final String TIMEOUT = "TIMEOUT";
        public static final String USER_NEW_SESSION = "USER_NEW_SESSION";
        public static final String USER_CLOSED = "USER_CLOSED";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

    /** 会话超时时长（分钟） */
    public static final long SESSION_TIMEOUT_MINUTES = 15;

    /** 消息角色 */
    public static final class MessageRole {
        public static final String USER = "USER";
        public static final String ASSISTANT = "ASSISTANT";
        public static final String SYSTEM = "SYSTEM";
    }

    /** 消息状态 */
    public static final class MessageStatus {
        public static final String SENT = "SENT";
        public static final String STREAMING = "STREAMING";
        public static final String DONE = "DONE";
        public static final String FAILED = "FAILED";
    }

    /** 知识库文档状态 */
    public static final class KbDocumentStatus {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String ACTIVE = "ACTIVE";
        public static final String DISABLED = "DISABLED";
        public static final String FAILED = "FAILED";
    }

    /** 知识库文档分类 */
    public static final class KbDocumentCategory {
        public static final String PLATFORM_RULE = "PLATFORM_RULE";
        public static final String APP_USAGE = "APP_USAGE";
        public static final String LOCK_FAQ = "LOCK_FAQ";
        public static final String DEPOSIT = "DEPOSIT";
        public static final String BILL = "BILL";
        public static final String LEASE = "LEASE";
        public static final String APPOINTMENT = "APPOINTMENT";
        public static final String REPAIR = "REPAIR";
        public static final String GENERAL = "GENERAL";
    }

    /** 反馈类型 */
    public static final class FeedbackType {
        public static final String LIKE = "LIKE";
        public static final String DISLIKE = "DISLIKE";
    }

    /** 意图分类 */
    public static final class Intent {
        public static final String PLATFORM_RULE = "PLATFORM_RULE";
        public static final String HOUSE_INFO = "HOUSE_INFO";
        public static final String LEASE_QUERY = "LEASE_QUERY";
        public static final String BILL_QUERY = "BILL_QUERY";
        public static final String LOCK_QUERY = "LOCK_QUERY";
        public static final String APPOINTMENT_QUERY = "APPOINTMENT_QUERY";
        public static final String REPAIR_QUERY = "REPAIR_QUERY";
        public static final String APP_USAGE = "APP_USAGE";
        public static final String HUMAN_REQUIRED = "HUMAN_REQUIRED";
        public static final String UNKNOWN = "UNKNOWN";
    }
}
