package com.xcw.picturebackend.model.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 互动目标类型
 * 1-图片  2-帖子（预留）  3-空间
 */
@Getter
public enum TargetTypeEnum {

    PICTURE(1, "图片"),
    POST(2, "帖子"),
    SPACE(3, "空间");

    private final int value;
    private final String text;

    TargetTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static TargetTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (TargetTypeEnum e : TargetTypeEnum.values()) {
            if (Objects.equals(e.value, value)) {
                return e;
            }
        }
        return null;
    }

    public static boolean isValid(Integer value) {
        return getEnumByValue(value) != null;
    }
}
