package com.xcw.picturebackend.model.dto.space.analyze;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/17
 */

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用排行分析 （仅管理员）
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
