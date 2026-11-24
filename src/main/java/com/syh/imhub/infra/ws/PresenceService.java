package com.syh.imhub.infra.ws;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PresenceService {

    // Redis 键前缀：im:online:{userId}
    private static final String PREFIX = "im:online:";
    // 在线 TTL：120 秒没有心跳就自动过期，比 WS 的 90 秒空闲断开略长
    private static final Duration TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isOnline(Long userId) {
        return userId != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + userId));
    }
}
