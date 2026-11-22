package com.syh.imhub.modules.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class HealthController {

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;

    @Value("${im.instance-id}")
    private String instanceId;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("instanceId", instanceId);
        try {
            body.put("db", jdbcTemplate.queryForObject("SELECT 1", Integer.class)==1? "on" : "off");
        } catch (Exception e) {
            body.put("db", "off");
        }
        try {
            String pong = redis.execute((connection) -> connection.ping(),true);
            body.put("redis", "pong".equalsIgnoreCase(pong)? "on" : "off");
        } catch (Exception e) {
            body.put("redis", "off");
        }
        return body;
    }
}
