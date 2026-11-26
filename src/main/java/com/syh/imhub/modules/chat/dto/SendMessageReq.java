package com.syh.imhub.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SendMessageReq(
        // 客户端消息 ID：重发时保持不变即可命中幂等，长度上限 64
        @NotBlank(message = "clientMsgId 不能为空") @Size(max = 64) String clientMsgId,
        // 消息类型：1 文本 / 2 图片 / 3 文件 / 4 系统（4 只允许服务端产生）
        @NotNull(message = "消息类型不能为空") Integer type,
        // 文本正文，最多 2000 字；非文本类型时可为空
        @Size(max = 2000, message = "单条消息最多 2000 字") String content,
        // 扩展字段：非文本类型放 url、文件名、尺寸等，序列化后存 im_message.extra
        Map<String, Object> extra) {
}
