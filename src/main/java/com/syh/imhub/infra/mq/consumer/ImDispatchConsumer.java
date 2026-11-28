package com.syh.imhub.infra.mq.consumer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.infra.mq.DispatchPayload;
import com.syh.imhub.infra.mq.EventEnvelope;
import com.syh.imhub.infra.ws.WsSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImDispatchConsumer {
    // 消费失败日志：只 warn 不抛，避免一条坏消息阻塞整个分区
    private static final Logger log = LoggerFactory.getLogger(ImDispatchConsumer.class);

    private  final ObjectMapper objectMapper;
    private  final String instanceId;
    private final WsSessionRegistry registry;


    public ImDispatchConsumer(ObjectMapper objectMapper, WsSessionRegistry registry,
                              @Value("${im.instance-id}") String instanceId) {
        this.objectMapper = objectMapper;
        this.instanceId = instanceId;
        this.registry = registry;
    }

    @KafkaListener(topics = "im.message.dispatch", groupId = "im-dispatch-${im.instance-id}")
    public void onMessage(String value , Acknowledgment ack) {
        try {
            //反序列化为envelope
            EventEnvelope envelope = objectMapper.readValue(value, EventEnvelope.class);
            //只处理其他实例发送的消息
            if (!instanceId.equals(envelope.instanceId())) {
                DispatchPayload payload = objectMapper.convertValue(envelope.payload(), DispatchPayload.class);
                //判断消息类型
                String wsType = switch (payload.kind() == null ? "" : payload.kind()) {
                    // 新消息
                    case "MSG" -> "MSG";
                    // 已读同步（用户在其他端的已读要回推本端）
                    case "READ" -> "READ_SYNC";
                    // 撤回
                    case "RECALL" -> "RECALL";
                    // 正在输入
                    case "TYPING" -> "TYPING";
                    // 其他事件类型不推 WS
                    default -> null;
                };
                //类型有效才能推 WS
                if (wsType != null && !payload.memberIds().isEmpty()) {
                    // 推送消息
                    for(Long userId : payload.memberIds()){
                        registry.sendToUser(userId, Map.of(
                                "type", wsType,
                                "conversationId", payload.conversationId(),
                                "data", payload.data() == null ? Map.of() : payload.data()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("dispatch 消费失败", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
