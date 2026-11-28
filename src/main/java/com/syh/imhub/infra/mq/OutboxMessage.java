package com.syh.imhub.infra.mq;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("outbox_message")
public class OutboxMessage {

    @TableId(type = IdType.AUTO)
    // 主键：数据库自增，回写发送状态时按它定位记录
    private Long id;
    // 事件唯一 ID：与事件信封里的 eventId 一致，便于追一条事件的发送情况
    private String eventId;
    // 目标 Kafka 主题
    private String topic;
    // 消息键：一般写会话/工单 ID，保证同一聚合的事件落在同一分区
    private String msgKey;
    // 事件信封的完整 JSON（EventEnvelope 序列化结果）
    private String payload;
    // 发送状态：0=待发送、1=已发送、2=重试超限放弃
    private Integer status;
    // 已重试次数：sendNow 失败时加一，达到 5 次置 status=2
    private Integer retryCount;
    // 下次可重投时间：null 表示立即可投，失败后置为当前时间 +10 秒
    private LocalDateTime nextRetryAt;
    // 记录创建时间，插入时由 MetaObjectHandler 自动填充
    private LocalDateTime createdAt;
}
