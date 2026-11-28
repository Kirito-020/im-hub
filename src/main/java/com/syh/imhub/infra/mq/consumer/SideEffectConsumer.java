package com.syh.imhub.infra.mq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.infra.mq.EventEnvelope;
import com.syh.imhub.modules.chat.service.UnreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SideEffectConsumer {

    // 消费失败日志
    private static final Logger log = LoggerFactory.getLogger(SideEffectConsumer.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final UnreadService unreadService;


    public SideEffectConsumer(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate, UnreadService unreadService) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.unreadService = unreadService;
    }

    @KafkaListener(topics = "im.message.side-effect", groupId = "im-side-effect")
    public void onMessage(String value , Acknowledgment ack) {
        try {
            EventEnvelope envelope = objectMapper.readValue(value, EventEnvelope.class);
            //将SideEffectEvent插入数据库
            int inserted = jdbcTemplate.update("INSERT IGNORE INTO consume_record (event_id,consumer) VALUES ( ?, ?)",
                    envelope.eventId(), "im-side-effect");
            //插入0说明插入失败，这条事件已被消费
            if(inserted == 0){
                return;
            }
            //取业务负载
            JsonNode payload = envelope.payload();
            long conversationId = payload.get("conversationId").asLong();
            JsonNode data = payload.get("data");
            long senderId = data.get("sender").asLong();
            // 接收人列表转成 Long 集合（JsonNode 需要显式指定元素类型）
            List<Long> memberIds = objectMapper.convertValue(payload.path("memberIds"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
            // 逐个成员累加未读
            for (Long memberId : memberIds) {
                // 跳过发送人自己
                if (memberId != senderId) {
                    // Redis 里该成员对该会话的未读数 +1
                    unreadService.increment(memberId, conversationId, 1);
                }
            }
        } catch (JsonProcessingException e) {
            //打印失败原因
            log.error("side effect 消费失败: {}", value, e);
        } finally {
            ack.acknowledge();
        }


    }
}
