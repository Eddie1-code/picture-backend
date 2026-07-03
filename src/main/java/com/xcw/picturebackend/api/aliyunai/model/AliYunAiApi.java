package com.xcw.picturebackend.api.aliyunai.model;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/15
 */
@Slf4j
@Component
public class AliYunAiApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 图片审核（使用多模态模型）
    public static final String IMAGE_MODERATION_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest 创建扩图任务的请求参数对象
     * @return CreateOutPaintingTaskResponse 创建扩图任务的响应对象
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 图片审核（使用多模态模型判断内容安全）
     *
     * @param imageUrl 图片 URL
     * @return "pass" / "review" / "block" / null（API 失败）
     */
    public String moderateImage(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return null;
        }
        String body = String.format(
            "{\"model\":\"qwen-vl-plus\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"image_url\",\"image_url\":{\"url\":\"%s\"}},{\"type\":\"text\",\"text\":\"判断这张图片是否包含色情、暴力、恐怖、血腥等违规内容。仅回复一个词：safe（安全）、review（不确定）、或 block（违规）。\"}]}]}",
            imageUrl);
        HttpRequest httpRequest = HttpRequest.post(IMAGE_MODERATION_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(body);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.warn("图片审核请求失败: {}", httpResponse.body());
                return null;
            }
            ImageModerationResponse response = JSONUtil.toBean(httpResponse.body(), ImageModerationResponse.class);
            if (response == null || CollUtil.isEmpty(response.getChoices())) {
                log.warn("图片审核响应无有效内容: {}", httpResponse.body());
                return null;
            }
            String content = response.getChoices().get(0).getMessage().getContent();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            String result = content.trim().toLowerCase();
            log.info("图片审核 AI 判定: imageUrl={}, result={}", imageUrl, result);
            if (result.contains("safe")) return "pass";
            if (result.contains("block")) return "block";
            if (result.contains("review")) return "review";
            return null;
        } catch (Exception e) {
            log.warn("图片审核异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId 任务 id
     * @return GetOutPaintingTaskResponse 查询任务的响应对象
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
