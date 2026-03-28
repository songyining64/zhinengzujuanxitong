package com.example.exam.common.enums;

public enum QuestionTypeEnum {
    SINGLE,
    MULTIPLE,
    TRUE_FALSE,
    FILL,
    SHORT;

    public static boolean isValid(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        try {
            QuestionTypeEnum.valueOf(name.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
