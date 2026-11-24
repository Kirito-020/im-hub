package com.syh.imhub.modules.chat.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.chat.dto.ConversationVO;
import com.syh.imhub.modules.chat.service.ConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
