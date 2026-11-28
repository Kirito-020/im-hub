package com.syh.imhub.infra.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsSessionRegistry {
    // 发送失败与序列化失败的日志
    private static final Logger log = LoggerFactory.getLogger(WsSessionRegistry.class);

    // userId → 该用户在本实例的全部连接（同一用户可能多端同时在线）
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    // JSON 序列化：把业务对象转成 WS 帧文本
    private final ObjectMapper objectMapper;

    public WsSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void sendToUsers(Collection<Long> userIds, Object payload) {
        for(Long userId : userIds){
            sendToUser(userId, payload);
        }
    }

    public void sendToUser(Long userId, Object payload) {
        // 取该用户的连接集合
        Set<WebSocketSession> set = sessions.get(userId);
        // 没有任何连接（可能连在别的实例）就直接返回
        if (set == null || set.isEmpty()) {
            // 空集合等同离线，交给跨实例分发处理
            return;
        }
        // 只序列化一次，多端复用同一个 JSON
        String json;
        // 序列化失败要兜住，否则一个坏对象会拖垮整次推送
        try {
            // 转成 JSON 文本
            json = objectMapper.writeValueAsString(payload);
            // 序列化异常
        } catch (Exception e) {
            // 记 warn 后放弃
            log.warn("序列化 WS 消息失败", e);
            // 结束
            return;
        }
        // 逐个连接发送
        for (WebSocketSession session : set) {
            // 复用 sendRaw 做实际发送
            sendRaw(session, json);
        }
    }

    public void sendRaw(WebSocketSession session, String json) {
        // 连接为空或已关闭直接跳过（关闭瞬间常有这种调用）
        if (session == null || !session.isOpen()) {
            // 什么都不做
            return;
        }
        // 发送同样可能因为连接中断而失败
        try {
            // WebSocketSession 不支持并发发送，用 session 自身当锁
            synchronized (session) {
                // 发送文本帧
                session.sendMessage(new TextMessage(json));
            }
            // 连接已断导致的写失败很常见，用 debug 级别避免刷日志
        } catch (IOException e) {
            // 记录失败原因
            log.debug("WS 发送失败: {}", e.getMessage());
        }
    }

    public void register(Long userId, WebSocketSession session) {
        // computeIfAbsent 保证并发首次注册只建一个集合，值用线程安全的 Set
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }
}
