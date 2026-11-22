package com.syh.imhub.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterReq(@NotBlank(message = "用户名不能为空")
                          @Size(min = 3, max = 24, message = "用户名长度需为 3-24 位")
                          String username,
                          @NotBlank(message = "密码不能为空")
                          @Size(min = 6, max = 24, message = "密码长度需为 6-24 位")
                          String password,
                          @Size(max = 24, message = "昵称最长为 24 位")
                          String nickname,
                          String role,
                          String skillGroup) {
}
