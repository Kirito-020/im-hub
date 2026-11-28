package com.syh.imhub.infra.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.modules.agent.service.CsSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImWebSocketHandler extends TextWebSocketHandler {

    private final WsSessionRegistry registry;
    private final PresenceService presenceService;
    private final CsSessionService csSessionService;
    // 每个连接的最后活跃时间（sessionId → 毫秒），sweepIdle 靠它判空闲
    private final Map<String, Long> lastActive = new ConcurrentHashMap<>();
    // 本实例 ID：CONNECTED 帧回给前端，便于排查连到了哪台机器
    private final String instanceId;
    private final ObjectMapper objectMapper;


    public ImWebSocketHandler(WsSessionRegistry registry, PresenceService presenceService,
                              CsSessionService csSessionService,
                              @Value("${im.instance-id}") String instanceId, ObjectMapper objectMapper) {
        this.registry = registry;
        this.presenceService = presenceService;
        this.csSessionService = csSessionService;
        this.instanceId = instanceId;
        this.objectMapper = objectMapper;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        //获取用户ID
        Long userId = (Long) session.getAttributes().get("userId");
        //没有用户ID说明握手没走拦截器或票据无效
        if(userId == null){
            //直接关闭
            closeQuietly(session);
            return;
        }
        //登记会话，后续所有推送都考这个会话连接
        registry.register(userId, session);
        // Redis 标记在线（TTL 120 秒，靠后续帧续期）
        presenceService.online(userId);
        //判断用户role:只有agent需要维护心跳
        boolean agent = session.getAttributes().get("role") != null && isAgentRole(String.valueOf(session.getAttributes().get("role")));
        if(agent){
            // 维护心跳 (只有主动点在线的agent才会真正续期)
            csSessionService.heartBeat(userId);
        }
        // 更新最后活跃时间
        lastActive.put(session.getId(), System.currentTimeMillis());
        // 回发 CONNECTED：带 userId、服务端时间和实例 ID，前端据此确认连接可用
        send(session, Map.of("type", "CONNECTED", "data", Map.of(
                "userId", userId,
                "serverTime", Instant.now().toString(),
                "instance", instanceId)));

    }

    /** 序列化并发送：把任意对象转成 JSON 后经会话表发出。
     * 参数 session 目标连接、payload 待发对象；无返回值；序列化失败会被吞掉，只丢弃这一帧。 */
    private void send(WebSocketSession session, Object payload) {
        // 序列化可能失败，不允许异常冒到 WS 回调里
        try {
            // 经会话表发送，复用它的锁与「连接是否打开」判断
            registry.sendRaw(session, objectMapper.writeValueAsString(payload));
            // 序列化失败（一般是对象不可序列化）直接忽略这一帧
        } catch (Exception e) {

        }
    }

    private boolean isAgentRole(String role) {
        return "agent".equals(role) || "SUPERVISOR".equals(role) || "ADMIN".equals(role);
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            //SESSION_NOT_RELIABLE：表示会话不可靠，即可能已经关闭
            session.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (Exception e) {
            // ignore
        }
    }

}
