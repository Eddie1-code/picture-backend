package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

/**
 * 会员兑换码
 */
@Data
public class VipCode {
    private String code;      // 兑换码
    private boolean hasUsed;  // 是否已使用
}