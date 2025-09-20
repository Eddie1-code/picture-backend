package com.xcw.picturebackend.manager.auth.model;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/19
 */
@Data
public class SpaceUserRole implements Serializable {

    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 权限键列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}