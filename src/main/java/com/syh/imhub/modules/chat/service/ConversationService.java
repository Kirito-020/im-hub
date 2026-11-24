package com.syh.imhub.modules.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.infra.ws.PresenceService;
import com.syh.imhub.modules.chat.dto.ConversationVO;
import com.syh.imhub.modules.chat.dto.MessageVO;
import com.syh.imhub.modules.chat.eneity.ImConversation;
import com.syh.imhub.modules.chat.eneity.ImConversationMember;
import com.syh.imhub.modules.chat.eneity.ImMessage;
import com.syh.imhub.modules.chat.mapper.ImConversationMapper;
import com.syh.imhub.modules.chat.mapper.ImConversationMemberMapper;
import com.syh.imhub.modules.chat.mapper.ImMessageMapper;
import com.syh.imhub.modules.user.entity.ImUser;
import com.syh.imhub.modules.user.mapper.ImUserMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper memberMapper ;
    private final ImUserMapper userMapper;
    private final ImMessageMapper messageMapper;
    private final UnreadService unreadService;
    private final ObjectMapper objectMapper;
    //在线状态
    private final PresenceService presence;



    public ConversationService(ImConversationMapper conversationMapper, ImConversationMemberMapper memberMapper,
                               ImUserMapper userMapper, ImMessageMapper messageMapper, UnreadService unreadService,
                               ObjectMapper objectMapper, PresenceService presence) {
        this.conversationMapper = conversationMapper;
        this.userMapper = userMapper;
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.unreadService = unreadService;
        this.objectMapper = objectMapper;
        this.presence = presence;
    }

    public List<ConversationVO> getConversionList(Long userId) {
        //  获取会话列表
        List<ImConversation> conversations = conversationMapper.selectByUser(userId);
        //判断
        if(conversations.isEmpty()){
            return List.of();
        }
        //收集ID，用于一次性把成员查出来
        Set<Long> convIds = conversations.stream().map(ImConversation::getId).collect(Collectors.toSet());
        //一次性把成员查出来
        List<ImConversationMember> allMembers = memberMapper.selectList(Wrappers.<ImConversationMember>lambdaQuery()
                // 限定会话集合
                .in(ImConversationMember::getConversationId, convIds));
        //按会话ID分组
        Map<Long, List<ImConversationMember>> memberByConv = allMembers.stream().
                collect(Collectors.groupingBy(ImConversationMember::getConversationId));
        //收集所有用户id
        Set<Long> userIds = allMembers.stream().map(ImConversationMember::getUserId).collect(Collectors.toSet());
        //一次性把用户查出来
        Map<Long, ImUser> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(ImUser::getId, u -> u));
        List<ConversationVO> result = new ArrayList<>(conversations.size());
        for (ImConversation conversation : conversations){
            //取改会话的成员
            List<ImConversationMember> members = memberByConv.getOrDefault(conversation.getId(), List.of());
            result.add(toVO(conversation,userId,members,users,false));
        }
        return result;
    }
    /*
    * withmembers 是否需要返回成员详细信息
    * */

    private ConversationVO toVO(ImConversation conversation, Long viewerId,
                                List<ImConversationMember> members, Map<Long, ImUser> users, boolean withmembers) {
        //找到自己
        ImConversationMember viewer = members.stream()
                .filter(m -> Objects.equals(m.getUserId(), viewerId)).findFirst().orElse(null);
        //单聊：对面信息
        ImUser peer = null;
        //判断是单聊还是群聊
        if (conversation.getType() == 1) {
            //取第一个不是自己的成员
            peer = members.stream().filter(m -> !Objects.equals(m.getUserId(), viewerId))
                    .findFirst().map(m -> users.get(m.getUserId())).orElse(null);
        }
        //取最后一条信息作为摘要
        ImMessage lastMessage = messageMapper.selectByLast(conversation.getId());
        //没有消息时为null
        MessageVO lastVo = lastMessage == null ? null : toMessageVO(lastMessage, users);
        //查看已读位置
        long readSeq = viewer == null || viewer.getReadSeq() == null ? 0: viewer.getReadSeq();
        //会话标题：群聊名或成员昵称
        String title = conversation.getType() == 1
                ?(peer == null ? "已注销用户": peer.getNickname())
                :(conversation.getTitle() == null || conversation.getTitle().isBlank() ? "未命名群聊": conversation.getTitle());
        //成员明细列表
        List<ConversationVO.MemberVO> memberVOS = null;
        //withmembers为true时才需要返回成员详细信息
        if (withmembers) {
            //将成员逐一转换为VO
            memberVOS = members.stream().map(m -> {
                ImUser user = users.get(m.getUserId());
                return new ConversationVO.MemberVO(m.getUserId(),
                        // 昵称
                        user == null ? "已注销用户" : user.getNickname(),
                        // 头像
                        user == null ? "" : user.getAvatar(),
                        // 角色缺失时按普通用户展示
                        user == null ? "USER" : user.getRole(),
                        // 在线状态实时查 Redis
                        presence.isOnline(m.getUserId()),
                        // 会话内角色（群主/成员）
                        m.getMemberRole());
            }).toList();
        }
        //未读消息数
        int unread = viewer == null ? 0 : unreadService.get(viewerId, conversation.getId(), readSeq);
        //封装并返回
        return new ConversationVO(conversation.getId(),conversation.getType(),title,
                // 基础字段：会话 ID、类型、标题、群主、状态、最后一条消息、未读数
                conversation.getOwnerId(), conversation.getStatus(), lastVo, unread,
                // 查看者的置顶标记，null 视为未置顶
                viewer != null && viewer.getPinned() != null && viewer.getPinned() == 1,
                // 查看者的免打扰标记，null 视为未设置
                viewer != null && viewer.getMuted() != null && viewer.getMuted() == 1,
                // 单聊对端 ID
                peer == null ? null : peer.getId(),
                peer == null ? null : peer.getNickname(),
                peer == null ? "" : peer.getAvatar(),
                // 单聊对端在线状态，群聊恒为 false
                peer != null && presence.isOnline(peer.getId()),
                // 成员总数与成员明细（列表接口 members 为 null）
                members.size(), memberVOS,
                // 最后活跃时间：优先最后消息时间，没有消息时退回会话更新时间
                conversation.getLastMsgAt() == null ? conversation.getUpdatedAt() : conversation.getLastMsgAt());
    }

    private MessageVO toMessageVO(ImMessage lastMessage, Map<Long, ImUser> users) {
        //获取用户
        ImUser sender = users.get(lastMessage.getSenderId());
        //senderId=0即系统消息
        boolean system = lastMessage.getSenderId() != null && lastMessage.getSenderId() == 0;
        //昵称
        String nickname = system ? "系统" : (sender == null ? "已注销用户" : sender.getNickname());
        //头像
        String avatar = system ? "" : (sender == null ? "" : sender.getAvatar());
        //默认空扩展字段
        Map<String, Object> extra = Map.of();
        //只有非空字符串才需要解析
        if (lastMessage.getExtra() != null && !lastMessage.getExtra().isBlank()) {
            //解析扩展字段为JSON
            try {
                extra = objectMapper.readValue(lastMessage.getExtra(), new TypeReference<LinkedHashMap<String, Object>>() {});
            //解析失败不影响消息本体，退化成Map
            } catch (Exception ignored) {
                // 用空Map代替
                extra = Collections.emptyMap();
            }
        }

        //封装并返回
        return new MessageVO(lastMessage.getId(), lastMessage.getConversationId(), lastMessage.getSeq(),
                lastMessage.getSenderId(), nickname, avatar, lastMessage.getType(), lastMessage.getContent(),
                extra, lastMessage.getStatus(),lastMessage.getClientMsgId(), lastMessage.getCreatedAt());
    }
}
