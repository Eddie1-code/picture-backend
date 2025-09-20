package com.xcw.picturebackend.manager.auth.model;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/19
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;

    private static final long serialVersionUID = 1L;
}