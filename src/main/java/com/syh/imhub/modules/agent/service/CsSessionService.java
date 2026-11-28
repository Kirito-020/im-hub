package com.syh.imhub.modules.agent.service;


import org.springframework.stereotype.Service;

@Service
public class CsSessionService {
    // 坐席在线状态服务：转接时判断目标坐席是否在线（90 秒心跳）
    private final AgentPresenceService agentPresence;

    public CsSessionService(AgentPresenceService agentPresenceService) {
        this.agentPresence = agentPresenceService;
    }

    public void heartBeat(Long agentId) {
        agentPresence.heartBeat(agentId);
    }
}
