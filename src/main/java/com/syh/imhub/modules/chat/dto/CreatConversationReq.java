package com.syh.imhub.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatConversationReq(@NotNull(message = "类型不能为空") Integer type,
                                   Long targetUserId,
                                   @Size(max = 128) String title,
                                   List<Long> memberIds) {
}
