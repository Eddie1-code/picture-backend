package com.xcw.picturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑请求消息
 * 用于表示用户在图片编辑器中的操作请求(客户端发送到服务器)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION", "INFO", "ERROR"
     */
    private String type;

    /**
     * 执行的编辑动作（放大、缩小、旋转）
     */
    private String editAction;
}