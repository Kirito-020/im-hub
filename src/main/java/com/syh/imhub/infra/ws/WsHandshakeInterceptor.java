package com.syh.imhub.infra.ws;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate redisTemplate;

    public WsHandshakeInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //从查询参数中获取ticket
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("ticket");
        //验证ticket
        if (ticket == null || ticket.isBlank()) {
            //连接失败
            return false;
        }
        String key = "ws:ticket:" + ticket;
        //从Redis中获取ticket
        String value = redisTemplate.opsForValue().get(key);
        //验证value
        if(value == null){
            return false;
        }
        //删除Redis中的ticket,一次性
        redisTemplate.delete(key);
        //获取用户ID和role
        String[] split = value.split("\\|", 2);
        attributes.put("userId",Long.valueOf(split[0]));
        //获取role:如果缺省则默认USER
        attributes.put("role", split.length >1?split[1]:"USER");
        //允许连接
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
