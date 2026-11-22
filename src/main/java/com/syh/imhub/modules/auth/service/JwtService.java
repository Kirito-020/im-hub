package com.syh.imhub.modules.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final StringRedisTemplate redis;
    private final String instanceId;

    public JwtService(@Value("${im.jwt.secret}") String secret,
                      @Value("${im.jwt.access-ttl}") Duration accessTtl,
                      @Value("${im.jwt.refresh-ttl}") Duration refreshTtl,
                      StringRedisTemplate redis,
                      @Value("${im.instance-id}") String instanceId) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.redis = redis;
        this.instanceId = instanceId;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds, String jti) {
    }

    /*签发双令牌
     * access: 访问令牌(15分钟有效期)
     * refresh: 刷新令牌(7天有效期)
     * */

    public TokenPair issue(Long userId, String role) {
        //JWT ID:一次性随机串，用于防止JWT被重复使用，登出黑名单以此为准
        String jti = UUID.randomUUID().toString().replace("-", "");
        //签发时间
        Instant now = Instant.now();
        //生成access token
        String access = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("jti", jti)
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claim("role", role)
                .signWith(key)
                //序列化成紧凑的字符串
                .compact();
        //生成refresh token
        String refresh = Jwts.builder()
                .subject(String.valueOf(userId))
                //refresh token的jti和access token的jti不同
                .claim("jti", UUID.randomUUID().toString().replace("-", ""))
                .claim("typ", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .claim("role", role)
                .signWith(key)
                .compact();
        //将refresh token存入redis
        redis.opsForValue().set("auth:refresh:" + userId, refresh, refreshTtl);
        //返回access token,refresh token,expires in,jti,jti方便后续登录时拉黑
        return new TokenPair(access, refresh, accessTtl.toSeconds(), jti);
    }
}
