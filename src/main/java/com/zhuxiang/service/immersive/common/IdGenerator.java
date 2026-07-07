package com.zhuxiang.service.immersive.common;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * ID 生成器。格式：前缀 + Snowflake 风格 ID。
 */
@Component
public class IdGenerator {

    private final Snowflake snowflake = new Snowflake(1);

    public String nextTourId() { return "tour_" + snowflake.nextId(); }
    public String nextSceneId() { return "scene_" + snowflake.nextId(); }
    public String nextImageId() { return "image_" + snowflake.nextId(); }
    public String nextHotspotId() { return "hotspot_" + snowflake.nextId(); }

    private static class Snowflake {
        private static final long EPOCH = 1700000000000L;
        private static final long WORKER_ID_BITS = 5L;
        private static final long SEQUENCE_BITS = 12L;
        private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
        private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

        private final long workerId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;

        Snowflake(long workerId) {
            if (workerId > MAX_WORKER_ID || workerId < 0)
                throw new IllegalArgumentException("Worker ID out of range: " + workerId);
            this.workerId = workerId;
        }

        synchronized long nextId() {
            long timestamp = System.currentTimeMillis();
            if (timestamp < lastTimestamp) throw new RuntimeException("Clock moved backwards");
            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) timestamp = tilNextMillis();
            } else {
                sequence = 0L;
            }
            lastTimestamp = timestamp;
            return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
        }

        private long tilNextMillis() {
            long timestamp = System.currentTimeMillis();
            while (timestamp <= lastTimestamp) timestamp = System.currentTimeMillis();
            return timestamp;
        }
    }
}
