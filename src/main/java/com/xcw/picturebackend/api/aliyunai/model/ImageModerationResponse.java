package com.xcw.picturebackend.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 阿里云 DashScope 兼容模式图片审核响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageModerationResponse {

    private List<Choice> choices;

    private String code;

    private String message;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
