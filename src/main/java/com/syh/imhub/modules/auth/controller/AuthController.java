package com.syh.imhub.modules.auth.controller;

import com.syh.imhub.common.context.UserContext;
import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.auth.dto.LoginReq;
import com.syh.imhub.modules.auth.dto.LoginResp;
import com.syh.imhub.modules.auth.dto.RegisterReq;
import com.syh.imhub.modules.auth.service.AuthService;
import com.syh.imhub.modules.auth.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public R<LoginResp> register(@Valid @RequestBody RegisterReq req){
        return R.ok(authService.register(req));

    }

    @PostMapping("/login")
    public R<LoginResp> login(@Valid @RequestBody LoginReq req){
        return R.ok(authService.login(req));
    }

    public record RefreshReq(@NotBlank String refreshToken) {}

    @PostMapping("/refresh")
    public R<Map<String, Object>> refresh(@Valid @RequestBody RefreshReq req){
        JwtService.TokenPair pair = jwtService.refresh(req.refreshToken());
        return R.ok(Map.of(
                //新的访问令牌
                "accessToken", pair.accessToken(),
                //新的刷新令牌（每一次刷新accessToken都会一并生成新的refreshToken）
                "refreshToken", pair.refreshToken(),
                "expiresIn", pair.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public R logout(){
        //获取当前用户
        UserContext.Principal principal = UserContext.require();
        jwtService.logout(principal.userId(), principal.jti(), principal.expiresAt());
        return R.ok();
    }

    @GetMapping("/me")
    public R<LoginResp.UserInfo> me(){
        return R.ok(authService.me(UserContext.userId()));
    }

}
