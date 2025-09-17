package com.xcw.picturebackend.model.dto.space.analyze;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/17
 */

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间用户上传行为分析请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}
