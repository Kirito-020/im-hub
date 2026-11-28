package com.syh.imhub.modules.chat.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.chat.dto.ConversationVO;
import com.syh.imhub.modules.chat.dto.CreatConversationReq;
import com.syh.imhub.modules.chat.dto.MessageVO;
import com.syh.imhub.modules.chat.dto.SendMessageReq;
import com.syh.imhub.modules.chat.service.ConversationService;
import com.syh.imhub.modules.chat.service.MessageService;
import com.syh.imhub.modules.chat.service.UnreadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/")
public class ChatController {

    private final ConversationService conversationService;
    private final UnreadService unreadService;
    private final MessageService messageService;


    public ChatController(ConversationService conversationService, UnreadService unreadService, MessageService messageService) {
        this.conversationService = conversationService;
        this.unreadService = unreadService;
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public R<List<ConversationVO>> getConversationList() {
        //获取用户ID
        Long userId = UserContext.userId();
        return R.ok(conversationService.getConversionList(userId));
    }

    @PostMapping("/conversations")
    public R<ConversationVO> createConversation(@Valid @RequestBody CreatConversationReq req) {
        //获取用户ID
        Long userId = UserContext.userId();
        return R.ok(conversationService.createConversation(userId, req));
    }

    //未读消息汇总
    @GetMapping("/conversations/unread")
    public R<Map<String, Object>> getUnreadMessageCount() {
        //获取用户ID
        Long userId = UserContext.userId();
        return R.ok(Map.of("unreadCount", unreadService.getUnreadMessageCount(userId)));
    }

    @GetMapping("/conversations/{id}")
    public R<ConversationVO> getConversation(@PathVariable Long id) {
        //获取用户ID
        Long userId = UserContext.userId();
        return R.ok(conversationService.detail(id, userId));
    }

    private record FlagReq(boolean pinned, boolean muted) {}
    @PutMapping("/conversations/{id}/pin")
    public R<ConversationVO> pinConversation(@PathVariable Long id, @RequestBody(required = false) FlagReq req) {
        //获取用户ID
        Long userId = UserContext.userId();
        //body可省略，没传或没带都视为取消置顶
        boolean pinned = req != null && Boolean.TRUE.equals(req.pinned);
        conversationService.updateMemberFlag(id, userId, pinned,null);
        return R.ok(conversationService.detail(id, userId));
    }

    @PutMapping("/conversations/{id}/mute")
    public R<ConversationVO> muteConversation(@PathVariable Long id, @RequestBody(required = false) FlagReq req) {
        //获取用户ID
        Long userId = UserContext.userId();
        //body可省略，没传或没带都视为取消静音
        boolean muted = req != null && Boolean.TRUE.equals(req.muted);
        conversationService.updateMemberFlag(id, userId, null,muted);
        return R.ok(conversationService.detail(id, userId));
    }

    @PostMapping("/messages")
    public R<MessageVO> sendMessage(@RequestBody Map<String, Object> body) {
        Long conversationId = Long.valueOf(String.valueOf(body.get("conversationId")));
        SendMessageReq req = new SendMessageReq(String.valueOf(body.getOrDefault("clientMsgId", UUID.randomUUID().toString())),
                body.get("type") == null ? 1 : Integer.valueOf(String.valueOf(body.get("type"))),
                body.get("content") == null ? "" : String.valueOf(body.get("content")),
                body.get("extra") instanceof Map<?, ?> map ? castMap(map) : null);
        return R.ok(messageService.sendMessage(conversationId,UserContext.userId(), req));
    }

    private Map<String, Object> castMap(Map<?,?> map) {
        return (Map<String, Object>) map;
    }

    @GetMapping("/messages")
    public R<List<MessageVO>> getMessages(@RequestParam Long conversationId,
                                          @RequestParam(required = false) Long beforeSeq,
                                          @RequestParam(defaultValue = "30") int size) {
        return R.ok(messageService.historyMessages(conversationId, UserContext.userId(), beforeSeq, size));
    }

    @GetMapping("/messages/sync")
    public R<Map<Long,List<MessageVO>>> syncMessages(@RequestParam(required = false) String items) {
        return R.ok(messageService.syncMessages(items, UserContext.userId()));
    }

    @PostMapping("/messages/recall")
    public R<Void> recallMessage(@RequestParam Long id) {
        messageService.recallMessage(id, UserContext.userId());
        return R.ok();
    }




}
