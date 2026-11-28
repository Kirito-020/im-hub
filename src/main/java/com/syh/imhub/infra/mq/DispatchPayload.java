package com.syh.imhub.infra.mq;

import java.util.List;

public record DispatchPayload(// 事件类型：MSG=新消息、READ=已读同步、RECALL=撤回、TYPING=正在输入
                              String kind,
                              // 会话 ID：消费端推 WS 帧时带回去，前端按会话路由
                              Long conversationId,
                              // 推送接收人列表：消费端逐个查本实例有没有该用户的连接
                              List<Long> memberIds,
                              // 业务数据：MessageVO 等具体内容，消费端原样塞进 WS 帧的 data
                              Object data) {
}
