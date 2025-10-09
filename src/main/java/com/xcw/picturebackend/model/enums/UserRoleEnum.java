package com.xcw.picturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户角色枚举类
 */
@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    VIP("会员", "vip"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据值获取对应的枚举
     *
     * @param value 枚举值
     * @return 对应的枚举，如果没有找到则返回null
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }
}
