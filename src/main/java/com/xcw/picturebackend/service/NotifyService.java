package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcw.picturebackend.model.dto.notify.NotifyListRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.NotifyItemVO;
import com.xcw.picturebackend.model.vo.NotifyUnreadVO;

public interface NotifyService {

    /**
     * 获取当前登录用户的未读数聚合
     */
    NotifyUnreadVO getUnreadSummary(User loginUser);

    /**
     * 分页获取某类消息
     */
    IPage<NotifyItemVO> listMessages(NotifyListRequest request, User loginUser);

    /**
     * 标记某类消息全部为已读
     */
    boolean markAllRead(String notifyType, User loginUser);

    /**
     * 标记单条消息为已读
     */
    boolean markOneRead(String notifyType, Long bizId, User loginUser);
}
