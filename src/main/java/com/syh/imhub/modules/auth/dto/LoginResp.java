package com.syh.imhub.modules.auth.dto;

import java.util.List;

public record LoginResp(String accessToken,
                        String refreshToken,
                        long expiresIn,
                        UserInfo user) {

    public record UserInfo(Long id,String username, String nickname, String avatar,String role,
                           String skillGroup, Integer maxSessions, List<String> permissions) {
    }
}
