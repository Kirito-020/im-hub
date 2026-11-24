package com.syh.imhub.modules.chat.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MessageVO(// 消息主键，撤回接口用的就是它
                        Long id,
                        // 所属会话 ID
                        Long conversationId,
                        // 会话内单调递增序号，前端排序与增量同步的游标
                        Long seq,
                        // 发送者 ID，0 表示系统消息
                        Long senderId,
                        // 发送者昵称，系统消息固定为「系统」
                        String senderName,
                        // 发送者头像，系统消息为空串
                        String senderAvatar,
                        // 消息类型：1 文本 / 2 图片 / 3 文件 / 4 系统
                        Integer type,
                        // 正文，已做敏感词替换；撤回后为空串
                        String content,
                        // 扩展字段，已从 JSON 字符串反序列化成 Map
                        Map<String, Object> extra,
                        // 状态：1 正常 / 2 已撤回
                        Integer status,
                        // 客户端消息 ID，前端用它把本地乐观消息替换成服务端消息
                        String clientMsgId,
                        // 服务端落库时间
                        LocalDateTime createdAt) {
}
