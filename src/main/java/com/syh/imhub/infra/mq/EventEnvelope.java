package com.syh.imhub.infra.mq;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(// 事件唯一 ID：UUID 去掉横线，消费端用它写 consume_record 防重复处理
                            String eventId,
                            // 事件类型：如 IM_MESSAGE_CREATED、CS_QUEUED、TICKET_SLA_UPGRADE
                            String eventType,
                            // 事件发生时间：OffsetDateTime.now() 的 ISO-8601 字符串，用于排查与排序
                            String occurredAt,
                            // 上游 traceId：把 HTTP/WS 请求的链路号透传到消费端日志
                            String traceId,
                            // 信封版本号：当前固定写 1，留给以后做不兼容升级
                            int version,
                            // 生产实例 ID（配置项 im.instance-id）：消费端据此跳过本实例已经推过的消息
                            String instanceId,
                            // 业务负载：JsonNode 形式，消费端按事件类型 convertValue 成 DispatchPayload 等具体结构
                            JsonNode payload) {
}
