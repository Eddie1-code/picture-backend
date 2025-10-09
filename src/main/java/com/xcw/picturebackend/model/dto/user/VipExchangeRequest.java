package com.xcw.picturebackend.model.dto.user;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/25
 */

import lombok.Data;

import java.io.Serializable;

/**
 * 会员兑换请求
 */
@Data
public class VipExchangeRequest implements Serializable {
    // 用户 id
    private Long userId;
    // 兑换码
    private String vipCode;

    private static final long serialVersionUID = 1L;
}
