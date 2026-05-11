package com.strava.shared.security;

public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        UserContext ctx = HOLDER.get();
        return ctx != null ? ctx : UserContext.anonymous();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
