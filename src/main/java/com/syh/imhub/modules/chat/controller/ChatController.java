package com.syh.imhub.modules.chat.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.chat.dto.ConversationVO;
import com.syh.imhub.modules.chat.dto.CreatConversationReq;
import com.syh.imhub.modules.chat.service.ConversationService;
import com.syh.imhub.modules.chat.service.UnreadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/")
public class ChatController {

    private final ConversationService conversationService;
    private final UnreadService unreadService;


    public ChatController(ConversationService conversationService, UnreadService unreadService) {
        this.conversationService = conversationService;
        this.unreadService = unreadService;
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


}
