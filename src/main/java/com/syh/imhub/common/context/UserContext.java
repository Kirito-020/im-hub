package com.syh.imhub.common.context;

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
}
