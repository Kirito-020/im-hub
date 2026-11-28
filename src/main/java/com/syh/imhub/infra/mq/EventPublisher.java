package com.syh.imhub.infra.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.common.util.TraceId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class EventPublisher {
    private final String InstanceId;
    private final ObjectMapper objectMapper;
    private final OutboxMapper outboxMapper;
    private final KafkaTemplate<String, String> KafkaTemplate;


    public EventPublisher(@Value("${im.instance-id}") String instanceId, ObjectMapper objectMapper,
                          OutboxMapper outboxMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.InstanceId = instanceId;
        this.objectMapper = objectMapper;
        this.outboxMapper = outboxMapper;
        KafkaTemplate = kafkaTemplate;
    }


    public String publish(String topic , String key , String eventType , Object payload) {
        try {
            EventEnvelope envelope = new EventEnvelope(
                    UUID.randomUUID().toString().replace("-", ""),
                    eventType,
                    OffsetDateTime.now().toString(),
                    TraceId.get(),
                    1,
                    InstanceId,
                    objectMapper.valueToTree(payload));
            //将信封转换为JSON字符串
            String json = objectMapper.writeValueAsString(envelope);

            //构造发件箱
            OutboxMessage outbox = new OutboxMessage();
            outbox.setEventId(envelope.eventId());
            outbox.setTopic(topic);
            // 消息键：null 时写空串，避免 key 为 null 导致分区不固定
            outbox.setMsgKey(key == null ? "" : key);
            outbox.setPayload(json);
            // 初始状态 0=待发送（1=已发送，2=重试超限放弃）
            outbox.setStatus(0);
            // 重试次数从 0 开始，发送失败时累加
            outbox.setRetryCount(0);
            //保存发件箱
            outboxMapper.insert(outbox);

            // 真正投递的动作抽成 Runnable：可能立即执行，也可能等事务提交后执行
            Runnable send = () -> sendNow(outbox, json);

            //判断事务是否提交，如果提交则立即执行，否则等事务提交后执行
            if(TransactionSynchronizationManager.isSynchronizationActive()){
                //注册事务回调，等事务提交后执行
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        send.run();
                    }
                });
            //走到这说明当前没有事务，立即执行
            }else{
                //立即执行
                send.run();
            }
            //返回事件ID
            return envelope.eventId();
        //序列化或写库失败：包装成异常，让事务回滚
        } catch (Exception e) {
            throw new IllegalStateException("发布事件失败", e);
        }
    }

    private void sendNow(OutboxMessage outbox, String json) {
        //异步发送
        KafkaTemplate.send(outbox.getTopic(), outbox.getMsgKey(), json)
                .whenComplete((result, ex) -> {
                    //null 表示发送成功
                    if(ex == null ){
                        //更新发件箱状态为已发送
                        OutboxMessage updated = new OutboxMessage();
                        updated.setId(outbox.getId());
                        updated.setStatus(1);
                        outboxMapper.updateById(updated);
                    //发送失败
                    }else {
                        //更新发件箱状态为发送失败
                        OutboxMessage updated = new OutboxMessage();
                        updated.setId(outbox.getId());
                        //判断重试次数，是否达到上限
                        updated.setStatus(outbox.getRetryCount() != null && outbox.getRetryCount() >= 5 ? 2 : 0);
                        //更新重试次数+1
                        updated.setRetryCount(outbox.getRetryCount() != null ? outbox.getRetryCount() + 1 : 0);
                        //10秒后重试
                        updated.setNextRetryAt(LocalDateTime.now().plusSeconds(10));
                        outboxMapper.updateById(updated);
                    }
                });
    }
}
