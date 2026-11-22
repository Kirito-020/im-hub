package com.syh.imhub.modules.auth.controller;

import com.syh.imhub.common.result.R;
import com.syh.imhub.modules.auth.dto.LoginResp;
import com.syh.imhub.modules.auth.dto.RegisterReq;
import com.syh.imhub.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public R<LoginResp> register(@Valid @RequestBody RegisterReq req){
        return R.ok(authService.register(req));

    }

}
