package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.interaction.FavoriteQueryRequest;
import com.xcw.picturebackend.model.entity.FavoriteRecord;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.FavoriteVO;

import java.util.Collection;
import java.util.Set;

/**
 * 通用收藏服务
 */
public interface FavoriteRecordService extends IService<FavoriteRecord> {

    /**
     * 切换收藏状态
     */
    boolean toggleFavorite(Long targetId, Integer targetType, User loginUser);

    /**
     * 批量查询当前用户已收藏的 targetId 集合
     */
    Set<Long> listFavoriteTargetIds(Long userId, Integer targetType, Collection<Long> targetIds);

    /**
     * 「我的收藏」分页
     */
    Page<FavoriteVO> listMyFavorites(FavoriteQueryRequest request, User loginUser);
}
