package com.syh.imhub.modules.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationVO(Long id, Integer type,
                             String title,
                             // 群主用户 ID；单聊固定为 0
                             Long ownerId,
                             // 会话状态：1 正常
                             Integer status,
                             // 最后一条消息，空会话为 null
                             MessageVO lastMessage,
                             // 当前查看者在该会话的未读数
                             Integer unread,
                             // 当前查看者是否置顶
                             boolean pinned,
                             // 当前查看者是否免打扰
                             boolean muted,
                             // 单聊对端用户 ID，群聊为 null
                             Long peerId,
                             // 单聊对端昵称，群聊为 null
                             String peerName,
                             // 单聊对端头像，群聊为 null
                             String peerAvatar,
                             // 单聊对端是否在线（PresenceService 实时判定）
                             boolean peerOnline,
                             // 成员总数（含已退出成员，按成员表行数统计）
                             int memberCount,
                             // 成员明细，仅详情接口返回，列表接口为 null
                             List<MemberVO> members,
                             // 最后活跃时间：优先 last_msg_at，没有消息时退化为 updated_at
                             LocalDateTime updatedAt) {

    public record MemberVO(Long userId, String nickname, String avatar, String role,
                           // memberRole：0 普通成员 / 1 群成员 / 2 群主
                           boolean online, Integer memberRole) {
    }


}
