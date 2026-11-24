package com.syh.imhub.modules.chat.eneity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ImConversationMember {

    // 主键策略：数据库自增
    @TableId(type = IdType.AUTO)
    // 成员记录主键
    private Long id;
    // 所属会话
    private Long conversationId;
    // 成员用户 ID
    private Long userId;
    // 会话内角色：0 普通成员 / 1 群成员 / 2 群主
    private Integer memberRole;
    // 已读位置：该成员已读到的最大 seq
    private Long readSeq;
    // 是否置顶：1 置顶 / 0 普通，直接影响会话列表排序
    private Integer pinned;
    // 是否免打扰：1 免打扰 / 0 正常
    private Integer muted;
    // 加入时间；重新入群时会刷新
    private LocalDateTime joinedAt;
    // 退出时间：null 表示仍在会话中
    private LocalDateTime leftAt;
}
