package com.syh.imhub.infra.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.exception.BizException;
import com.syh.imhub.common.exception.ErrorCode;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.auth.service.JwtService;
import com.syh.imhub.modules.user.entity.ImUser;
import com.syh.imhub.modules.user.mapper.ImUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final ImUserMapper userMapper;
    private final ObjectMapper objectMapper;


    public AuthInterceptor(JwtService jwtService, ImUserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //跨域检查
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            return true;
        }
        //获取请求头
        String header = request.getHeader("Authorization");
        //检查请求头
        if (header == null || !header.startsWith("Bearer ")) {
            //手写401响应：拦截器阶段的异常不走全局异常处理
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
        try {
            //获取token并解析
            String token = header.substring(7);
            JwtService.AuthedUser authed = jwtService.authenticate(token);
            //回查用户信息
            ImUser user = userMapper.selectById(authed.userId());
            //用户不存在或被禁用status=0
            if(user == null || (user.getStatus() != null && user.getStatus() == 0)){
                //手写401响应：拦截器阶段的异常不走全局异常处理
                writeError(response, ErrorCode.UNAUTHORIZED);
                return false;
            }
            //校验通过，把用户信息放入threadlocal，供后续方法调用
            UserContext.set(new UserContext.Principal(authed.userId(), user.getUsername(), user.getNickname(),
                    user.getRole(), authed.jti(),authed.expiresAt()));
            //放行到controller
            return true;
        //令牌过期，签名错误，等等都在这里抛出异常
        } catch (BizException e) {
            //用异常自带的信息决定响应码401/403
            writeError(response, e.getErrorCode());
            return false;
        }

    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        //未登录，401，其余（被禁用、密码错误）403
        response.setStatus(code == ErrorCode.UNAUTHORIZED ? 401 : 403);
        // 声明 JSON 响应，前端按统一结构解析
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 指定 UTF-8，避免中文提示乱码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 序列化 R 结构写入响应体
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code.code(), code.msg())));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        //移除threadlocal
        UserContext.clear();
    }
}
