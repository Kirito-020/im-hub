package com.syh.imhub.modules.auth.service;

import com.syh.imhub.common.exception.BizException;
import com.syh.imhub.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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



    public AuthedUser authenticate(String token) {
        //先做签名、有效期、黑名单的校验
        Claims claims = parse(token);
        //判断令牌类型
        if(!"access".equals(claims.get("typ", String.class))){
            throw BizException.of(ErrorCode.UNAUTHORIZED, "token类型错误");
        }
        //返回认证用户信息
        return new AuthedUser(Long.valueOf(claims.getSubject()), claims.get("role", String.class),
                claims.get("jti", String.class), claims.getExpiration().getTime());
    }

    public record AuthedUser(Long userId, String role, String jti, long expiresAt) {
    }
    private Claims parse(String token) {
        try {
            //验签+校验exp
            Claims claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            //校验jti
            String jti = claims.get("jti", String.class);
            if (jti != null && redis.hasKey("auth:jti:blacklist:" + jti)) {
                throw  BizException.of(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
            }
            //检验通过
            return claims;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }
    }
}
