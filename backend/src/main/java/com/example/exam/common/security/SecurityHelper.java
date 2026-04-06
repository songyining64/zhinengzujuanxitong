package com.example.exam.common.security;

import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityHelper {

    private SecurityHelper() {
    }

    public static UserDetails requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "无效的用户身份");
        }
        return (UserDetails) principal;
    }

    public static String requireUsername() {
        return requireCurrentUser().getUsername();
    }
}
