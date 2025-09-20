package com.xcw.picturebackend.model.vo;


import com.xcw.picturebackend.model.entity.SpaceUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/19
 */

/**
 * 空间成员的视图包装类
 */
@Data
public class SpaceUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 空间信息
     */
    private SpaceVO space;

    private static final long serialVersionUID = 1L;

    /**
     * 将封装类 SpaceUserVO 转换为实体对象 SpaceUser
     *
     * @param spaceUserVO 需要转换的 SpaceUserVO 对象
     * @return 转换后的 SpaceUser 实体对象，若参数为 null 则返回 null
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }

    /**
     * 将实体对象 SpaceUser 转换为封装类 SpaceUserVO
     *
     * @param spaceUser 需要转换的 SpaceUser 实体对象
     * @return 转换后的 SpaceUserVO 对象，若参数为 null 则返回 null
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }
}