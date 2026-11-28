package com.syh.imhub.infra.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    // 消息分发主题：消息落库后广播给其他实例，由各实例推给自己持有的 WebSocket 连接
    public static final String TOPIC_DISPATCH = "im.message.dispatch";
    // 消息副作用主题：未读数 +1 这类最终一致的操作，与主推送链路解耦
    public static final String TOPIC_SIDE_EFFECT = "im.message.side-effect";
    // 客服会话事件主题：排队/接入/转接/结束等状态变更，供看板与统计消费
    public static final String TOPIC_CS_EVENT = "cs.session.event";
    // 工单事件主题：工单创建/流转/SLA 升级的通知类事件
    public static final String TOPIC_TICKET_EVENT = "ticket.event";
    // 工单 SLA 到期主题：由 TicketSlaScheduler 投递，TicketSlaConsumer 收到后自动升级优先级
    public static final String TOPIC_TICKET_SLA = "ticket.sla.due";
}
