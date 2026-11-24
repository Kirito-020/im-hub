package com.syh.imhub.modules.chat.service;

import com.syh.imhub.modules.chat.mapper.ImMessageMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UnreadService {
    private final StringRedisTemplate redis;
    private final ImMessageMapper imMessageMapper;

    public UnreadService(StringRedisTemplate redis, ImMessageMapper imMessageMapper) {
        this.redis = redis;
        this.imMessageMapper = imMessageMapper;
    }

    private String key(Long userId) {
        return "im:unread:" + userId;
    }

    public int get(Long userId, Long conversationId, long readSeq) {
        //先读缓存
        Object cached = redis.opsForHash().get(key(userId), String.valueOf(conversationId));
        //如果缓存有
        if (cached != null) {
            //缓存值是字符串，需要转换成整数
            try {
                return Math.max(0, Integer.parseInt(cached.toString()));
            } catch (NumberFormatException ignored) {

            }
        }
        //如果缓存没有,取数据库数据
        long count = imMessageMapper.countUnread(userId, conversationId, readSeq);
        //写到缓存
        redis.opsForHash().put(key(userId), String.valueOf(conversationId), String.valueOf(count));
        //返回结果
        return (int) count;
    }
}
