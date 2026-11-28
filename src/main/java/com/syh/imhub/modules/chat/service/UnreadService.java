package com.syh.imhub.modules.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.syh.imhub.modules.chat.eneity.ImConversationMember;
import com.syh.imhub.modules.chat.mapper.ImConversationMemberMapper;
import com.syh.imhub.modules.chat.mapper.ImMessageMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UnreadService {
    private final StringRedisTemplate redis;
    private final ImMessageMapper imMessageMapper;
    private final ImConversationMemberMapper memberMapper;

    public UnreadService(StringRedisTemplate redis, ImMessageMapper imMessageMapper, ImConversationMemberMapper memberMapper) {
        this.redis = redis;
        this.imMessageMapper = imMessageMapper;
        this.memberMapper = memberMapper;
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

    public int getUnreadMessageCount(Long userId) {
        //先读缓存
        Map<Object, Object> entries = redis.opsForHash().entries(key(userId));
        int sum=0;
        //缓存不为空直接累加返回
        if (!entries.isEmpty()){
            for (Object value : entries.values()) {
                try {
                    sum += Integer.parseInt(value.toString());
                } catch (NumberFormatException ignored) {

                }
            }
            return sum;
        }
        //缓存为空，取数据库数据
        List<ImConversationMember> members = memberMapper.selectList
                (Wrappers.<ImConversationMember>lambdaQuery().eq(ImConversationMember::getUserId, userId));
        //计算未读消息总数
        for(ImConversationMember member : members){
            sum += imMessageMapper.countUnread(userId, member.getConversationId(),
                    member.getReadSeq() == null ? 0 : member.getReadSeq());
        }
        return sum;
    }

    public void increment(Long userId, long conversationId, int delta) {
        redis.opsForHash().increment(key(userId), String.valueOf(conversationId), delta);
    }
}
