package com.xcw.picturebackend.manager.auth.model;


import lombok.Data;

import java.io.Serializable;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/19
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}