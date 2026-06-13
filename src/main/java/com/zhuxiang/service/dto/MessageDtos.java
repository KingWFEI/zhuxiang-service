package com.zhuxiang.service.dto;

import java.time.LocalDateTime;
import java.util.Map;

public final class MessageDtos {

    private MessageDtos() {
    }

    public record MessageView(
            String id,
            String category,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean isRead,
            String actionType,
            String actionTarget,
            String iconKey
    ) {
    }

    public record UnreadCounts(
            long total,
            long system,
            long lease,
            long lock,
            long bill,
            long appointment,
            long repair
    ) {
        public static UnreadCounts from(Map<String, Long> counts) {
            long system = counts.getOrDefault("system", 0L);
            long lease = counts.getOrDefault("lease", 0L);
            long lock = counts.getOrDefault("lock", 0L);
            long bill = counts.getOrDefault("bill", 0L);
            long appointment = counts.getOrDefault("appointment", 0L);
            long repair = counts.getOrDefault("repair", 0L);
            return new UnreadCounts(
                    system + lease + lock + bill + appointment + repair,
                    system, lease, lock, bill, appointment, repair
            );
        }
    }
}
