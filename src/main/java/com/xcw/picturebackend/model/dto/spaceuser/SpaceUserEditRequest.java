package com.xcw.picturebackend.model.dto.spaceuser;


import lombok.Data;

import java.io.Serializable;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/19
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}