package com.syh.imhub.modules.chat.event;

/*
 * 进程内事件：客户在客服会话里发出一条消息。
 * 位置：MessageService.send 落库后通过 ApplicationEventPublisher 发布，工单侧监听后决定是否重新排队。
 * 与 Kafka 事件的区别：这条事件不跨进程，只在本实例内的 Spring 容器里同步消费。
 */
/**
 * 客户在客服会话里发消息（用于已结束会话自动重新排队）。
 */
// conversationId 会话；senderId 发送者（监听方用它判断是不是客户）；messageId 触发的消息
public record CustomerMessageEvent(Long conversationId, Long senderId, Long messageId) {
}