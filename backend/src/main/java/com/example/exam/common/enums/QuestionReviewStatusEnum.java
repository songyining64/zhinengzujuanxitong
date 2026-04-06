package com.example.exam.common.enums;

/**
 * 试题审核状态：仅 {@link #PUBLISHED} 参与自动组卷。
 */
public enum QuestionReviewStatusEnum {
    /** 教师草稿 */
    DRAFT,
    /** 待管理员审核 */
    PENDING,
    /** 已入库，可组卷 */
    PUBLISHED,
    /** 已驳回 */
    REJECTED;

    public static boolean isValid(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            QuestionReviewStatusEnum.valueOf(name.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
