package com.syh.imhub.modules.agent.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AgentPresenceService {

    // 在线坐席集合键：坐席点上线时加入，掉线/心跳过期时移除，仅用于批量遍历
    private static final String ONLINE_SET = "cs:agent:online";
    // 心跳键前缀：cs:agent:hb:{agentId}，值恒为 "1"，靠 90 秒 TTL 判定在线
    private static final String HB_PREFIX = "cs:agent:hb:";
    // 在线意图键前缀：cs:agent:intent:{agentId}，不设 TTL，代表坐席主动选择的在线状态
    private static final String INTENT_PREFIX = "cs:agent:intent:";
    // 心跳有效期 90 秒：超过这个时间没有任何 WS 活动，isOnline 就会返回 false
    private static final Duration TTL = Duration.ofSeconds(90);

    private final StringRedisTemplate redis;

    public AgentPresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean heartBeat(Long agentId) {
        // agentId 为空（例如未登录连接）直接返回 false，避免写出 cs:agent:hb:null 这类脏键
        if (agentId == null) {
            // 直接返回 false，调用方按「心跳失败」处理
            return false;
        }
        // 没有在线意图（从未点上线或已点下线）时不续期，防止已下线坐席被任意 WS 活动拉回在线
        if (!Boolean.TRUE.equals(redis.hasKey(INTENT_PREFIX + agentId))) {
            // 返回 false 表示本次心跳被忽略，坐席需重新点上线
            return false;
        }
        // 有意图就顺手把坐席补回在线集合，覆盖「集合成员被 sweeper 清掉但人还在线」的情况
        redis.opsForSet().add(ONLINE_SET, String.valueOf(agentId));
        // 续期 90 秒心跳键，这是 isOnline 的唯一判据
        refresh(agentId);
        // 心跳成功，调用方无需再处理
        return true;
    }

    private void refresh(Long agentId) {
        // 心跳键写 "1" 并带 TTL，过期后 Redis 自动删除，isOnline 随之变为 false
        redis.opsForValue().set(HB_PREFIX + agentId, "1", TTL);
    }
}
