package com.syh.imhub.modules.chat.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ws")
public class WsTicketController {

    private final StringRedisTemplate redis;
    private final Duration ticketTtl;
    public WsTicketController(StringRedisTemplate redis,
                              @Value("${im.ws.ticket-ttl}") Duration ticketTtl) {
        this.redis = redis;
        this.ticketTtl = ticketTtl;
    }


    @GetMapping("/ticket")
    public R<Map<String,Object>> getTicket() {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("ws:ticket:" + ticket,
                UserContext.userId()+"|"+UserContext.role(),ticketTtl);
        return R.ok(Map.of("ticket", ticket,"expireIn", ticketTtl.toSeconds()));
    }
}
