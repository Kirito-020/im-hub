package com.syh.imhub.modules.chat.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.chat.dto.ConversationVO;
import com.syh.imhub.modules.chat.dto.CreatConversationReq;
import com.syh.imhub.modules.chat.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/")
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
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


}
