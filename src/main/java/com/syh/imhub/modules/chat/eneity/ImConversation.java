package com.syh.imhub.modules.chat.eneity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("im_conversation")
@Getter
@Setter
public class ImConversation {

    // 主键策略：数据库自增
    @TableId(type = IdType.AUTO)
    // 会话主键
    private Long id;
    // 1 单聊 / 2 群聊
    private Integer type;
    // 业务唯一键：单聊为「小ID:大ID」，群聊为 group:UUID
    private String bizKey;
    // 群名称；单聊留空，展示时取对端昵称
    private String title;
    // 群主用户 ID；单聊为 0
    private Long ownerId;
    // 会话状态：1 正常
    private Integer status;
    // 最后一条消息 ID，用于会话列表摘要
    private Long lastMsgId;
    // 最后一条消息时间，会话列表按它倒序
    private LocalDateTime lastMsgAt;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间；没有消息时降级作为列表排序时间
    private LocalDateTime updatedAt;
}
