package com.syh.imhub.modules.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.common.exception.BizException;
import com.syh.imhub.common.exception.ErrorCode;
import com.syh.imhub.modules.admin.service.SensitiveWordService;
import com.syh.imhub.modules.chat.dto.MessageVO;
import com.syh.imhub.modules.chat.dto.SendMessageReq;
import com.syh.imhub.modules.chat.eneity.ImConversation;
import com.syh.imhub.modules.chat.eneity.ImConversationMember;
import com.syh.imhub.modules.chat.eneity.ImMessage;
import com.syh.imhub.modules.chat.mapper.ImConversationMapper;
import com.syh.imhub.modules.chat.mapper.ImConversationMemberMapper;
import com.syh.imhub.modules.chat.mapper.ImMessageMapper;
import com.syh.imhub.modules.user.entity.ImUser;
import com.syh.imhub.modules.user.mapper.ImUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final ConversationService conversationService;
    private final StringRedisTemplate redis;
    private final int msgPer10s;
    private final ImMessageMapper messageMapper;
    private final ImUserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final ObjectMapper objectMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper memberMapper;


    public MessageService(ConversationService conversationService, StringRedisTemplate redis,
                          @Value("${im.rate-limit.msg-per-10s:10}") int msgPer10s, ImMessageMapper messageMapper, ImUserMapper userMapper, SensitiveWordService sensitiveWordService, ObjectMapper objectMapper, ImConversationMapper conversationMapper, ImConversationMemberMapper memberMapper) {
        this.conversationService = conversationService;
        this.redis = redis;
        this.msgPer10s = msgPer10s;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.objectMapper = objectMapper;
        this.conversationMapper = conversationMapper;
        this.memberMapper = memberMapper;
    }

    @Transactional
    public MessageVO sendMessage(Long conversationId, Long senderId, SendMessageReq req) {
        //成员校验
        conversationService.requireMember(conversationId, senderId);
        //type=4是系统信息，只允许服务端发送
        if (req.type()!=null && req.type() == 4) {
            throw BizException.of(ErrorCode.PARAM_INVALID,"系统信息只允许服务端发送");
        }
        //频率限制，避免刷屏
        String ratekey = "im:rate:msg:"+senderId;
        //自增
        Long count = redis.opsForValue().increment(ratekey);
        //设置过期时间
        if(count != null && count == 1L){
            redis.expire(ratekey, Duration.ofSeconds(10));
        }
        if(count != null && count > msgPer10s){
            throw BizException.of(ErrorCode.RATE_LIMITED,"发送消息过快，请稍后再试");
        }
        //查看该信息是否已存在
        ImMessage existing = messageMapper.selectOne(Wrappers.<ImMessage>lambdaQuery()
                .eq(ImMessage::getClientMsgId, req.clientMsgId())
                .eq(ImMessage::getSenderId, senderId));
        //如果存在则返回旧信息
        if (existing != null) {
            // 取旧信息的发送者
            Map<Long, ImUser> users = userOf(List.of(existing));//传list数组是为了方便方法复用
            return conversationService.toMessageVO(existing,users);
        }

        //修饰内容
        String content = req.content() == null ? "": req.content().trim();
        //敏感词过滤
        SensitiveWordService.FilterResult filtered = sensitiveWordService.filter(content);
        //命中拦截词直接拒绝发送
        if (filtered.blocked()) {
            throw BizException.of(ErrorCode.SENSITIVE_BLOCKED,"消息内容包含敏感词，发送失败");
        }
        content = filtered.text();
        //类型缺少默认1（文本）
        int type = req.type() == null ? 1 : req.type();
        //extra缺少默认Map
        Map<String, Object> extra = req.extra() == null ? Map.of() : req.extra();
        //文本消息必须带内容
        if (type == 1 && content.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_INVALID,"消息内容不能为空");
        }
        //非文本消息必须带合法附件地址
        if(type !=1){
            String url = String.valueOf(req.extra().getOrDefault("url", ""));
            // 只允许站内 /uploads/ 相对路径，挡住 http(s)、file: 等外链与目录穿越
            if(!url.matches("^/uploads/[A-Za-z0-9/_.\\-]+$")){
                throw BizException.of(ErrorCode.PARAM_INVALID,"附件地址不合法");
            }
        }
        long seq = nextSeq(conversationId);
        //保存消息
        ImMessage message = new ImMessage();
        message.setConversationId(conversationId);
        message.setSeq(seq);
        message.setSenderId(senderId);
        message.setClientMsgId(req.clientMsgId());
        message.setType(type);
        message.setContent(content);
        message.setExtra(extra.isEmpty() ? null : toJson(extra));
        message.setStatus(1);
        message.setCreatedAt(LocalDateTime.now());
        //保存
        try {
            messageMapper.insert(message);
        //唯一键冲突：并发重发，进入回查分支
        } catch (DuplicateKeyException e) {
            // 并发同 clientMsgId：唯一键兜底，回查并返回已有消息
            ImMessage duplicated = messageMapper.selectOne(Wrappers.<ImMessage>lambdaQuery()
                    // 同一发送者
                    .eq(ImMessage::getSenderId, senderId)
                    // 同一条客户端消息 ID
                    .eq(ImMessage::getClientMsgId, req.clientMsgId()));
            // 查到了就按幂等返回已有消息
            if (duplicated != null) {
                // 转 VO，顺带补发送者信息
                return conversationService.toMessageVO(duplicated, userOf(List.of(duplicated)));
            }
            // 撞了唯一键却查不到，按冲突让前端重试
            throw BizException.of(ErrorCode.CONFLICT, "消息发送冲突，请重试");
        }
        //回写会话：lastmessage_id和lastmessage_at更新
        ImConversation conversation = new ImConversation();
        conversation.setId(conversationId);
        conversation.setLastMsgId(message.getId());
        conversation.setLastMsgAt(message.getCreatedAt());
        //更新会话
        conversationMapper.updateById(conversation);
        //查发送者在该会话的成员记录，起边推进已读位置
        ImConversationMember senderMember = memberMapper.selectOne(Wrappers.<ImConversationMember>lambdaQuery()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, senderId));
        //更新成员已读位置记录
        if(senderMember != null){
            ImConversationMember member = new ImConversationMember();
            member.setId(senderMember.getId());
            member.setReadSeq(seq);
            memberMapper.updateById(member);
        }
        //取发送者信息用于组装 VO
        Map<Long, ImUser> users = userOf(List.of(message));
        //生成vo,同时作为Kafka与Ws的payload
        MessageVO vo = conversationService.toMessageVO(message, users);
        return vo;
    }

    private String toJson(Object value) {
        //交给json序列化
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "消息扩展字段不合法" );
        }
    }

    private long nextSeq(Long conversationId) {
        String key = "im:seq" + conversationId;
        //判断redis是否存在该会话的seq
        Boolean exists = redis.hasKey(key);
        //不存在时去数据库查，然后存入redis
        if (!exists) {
            long max = messageMapper.selectMaxSeq(conversationId);
            redis.opsForValue().setIfAbsent(key, String.valueOf(max));
        }
        //redis自增
        Long seq = redis.opsForValue().increment(key);
        //Redis 不可用时降级：取库内最大值 + 1
        if(seq == null){
            return messageMapper.selectMaxSeq(conversationId) + 1;
        }
        return seq;
    }

    private Map<Long, ImUser> userOf(List<ImMessage> messages) {
        // 获取发送者Id,筛掉ID：0是系统信息
        Set<Long> ids = messages.stream().map(ImMessage::getSenderId).filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        //判空
        if (ids.isEmpty()) {
            return Map.of();
        }
        //一次查完
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(ImUser::getId, u -> u));
    }

    public List<MessageVO> historyMessages(Long conversationId, Long userId, Long beforeSeq, int size) {
        //会话成员校验
        conversationService.requireMember(conversationId, userId);
        // 夹住单页上限，防止 size 传太大把数据库拖垮
        int limit = Math.min(Math.max(size, 1), 100);
        var wrapper = Wrappers.<ImMessage>lambdaQuery()
                // 限定会话
                .eq(ImMessage::getConversationId, conversationId)
                // 倒序，配合 LIMIT 才能拿到最新一页
                .orderByDesc(ImMessage::getSeq)
                // limit 已夹到安全区间，可直接拼进 SQL
                .last("LIMIT " + limit);
        // 只有传了有效游标才追加 seq < beforeSeq，否则就是第一页
        if (beforeSeq != null && beforeSeq > 0) {
            // 严格小于，避免把游标那条消息重复返回
            wrapper.lt(ImMessage::getSeq, beforeSeq);
        }
        // 查询结果拷贝成可变列表
        List<ImMessage> rows = new ArrayList<>(messageMapper.selectList(wrapper));
        // 倒序结果翻成正序，前端按数组顺序直接渲染
        rows.sort((a, b) -> Long.compare(a.getSeq(), b.getSeq()));
        // 批量取发送者信息
        Map<Long, ImUser> users = userOf(rows);
        // 逐条转 VO 返回
        return rows.stream().map(m -> conversationService.toMessageVO(m, users)).toList();
    }
}
