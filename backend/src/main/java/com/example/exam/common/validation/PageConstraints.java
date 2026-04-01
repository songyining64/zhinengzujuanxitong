package com.example.exam.common.validation;

import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;

public final class PageConstraints {

    public static final int MAX_PAGE_SIZE = 200;

    private PageConstraints() {
    }

    public static long validatePage(long page) {
        if (page < 1) {
            throw new BizException(ErrorCode.PARAM_OUT_OF_RANGE, "page 必须 >= 1");
        }
        return page;
    }

    public static long validateSize(long size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BizException(ErrorCode.PARAM_OUT_OF_RANGE,
                    "size 必须在 1 与 " + MAX_PAGE_SIZE + " 之间");
        }
        return size;
    }

    public static void validate(long page, long size) {
        validatePage(page);
        validateSize(size);
    }
}
