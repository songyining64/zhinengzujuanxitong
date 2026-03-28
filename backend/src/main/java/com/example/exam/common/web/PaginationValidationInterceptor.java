package com.example.exam.common.web;

import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.validation.PageConstraints;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PaginationValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        validateOptionalLongParam(request, "page", PageConstraints::validatePage);
        validateOptionalLongParam(request, "current", PageConstraints::validatePage);
        validateOptionalLongParam(request, "size", PageConstraints::validateSize);
        return true;
    }

    private static void validateOptionalLongParam(
            HttpServletRequest request,
            String name,
            LongValidator validator
    ) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            long value = Long.parseLong(raw.trim());
            validator.validate(value);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数 " + name + " 必须为数字");
        }
    }

    @FunctionalInterface
    private interface LongValidator {
        long validate(long value);
    }
}
