package com.syh.imhub.common.context;

import com.syh.imhub.common.exception.BizException;
import com.syh.imhub.common.exception.ErrorCode;
import com.syh.imhub.modules.user.entity.ImUser;

public class UserContext {




    public record Principal(Long userId, String username, String nickName, String role,
                            String jti, long expiresAt) {
    }




    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Principal require() {
        Principal principal = HOLDER.get();
        // 如果没有当前用户，说明用户未经过AuthInterceptor，则抛出异常
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }

    public static Long userId() {
        return require().userId();
    }
}
