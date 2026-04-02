package com.xcw.picturebackend.manager;

import cn.hutool.core.util.StrUtil;
import com.xcw.picturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

/**
 * AI 任务超时管理器：
 * - 在创建任务时记录提交时间（Redis）
 * - 查询任务状态时，如果超过最大执行时间，则强制标记 FAILED
 */
@Slf4j
@Component
public class AiTaskTimeoutManager {

    private static final Duration OUT_PAINTING_MAX_EXEC_TIME = Duration.ofMinutes(10);

    private static final String OUT_PAINTING_TASK_SUBMIT_TIME_KEY_PREFIX = "out_painting:task_submit_time:";

    // 给 Redis key 预留一点缓冲（避免刚好超时后 key 过期导致无法判定）
    private static final long OUT_PAINTING_TASK_SUBMIT_TIME_TTL_MINUTES = OUT_PAINTING_MAX_EXEC_TIME.toMinutes() + 5;

    private static final DateTimeFormatter SUBMIT_TIME_FORMATTER_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter SUBMIT_TIME_FORMATTER_NO_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void recordOutPaintingTaskSubmitTime(String taskId, long submitTimeMillis) {
        if (StrUtil.isBlank(taskId)) {
            return;
        }
        try {
            String key = buildOutPaintingSubmitTimeKey(taskId);
            stringRedisTemplate.opsForValue().set(key, String.valueOf(submitTimeMillis));
            stringRedisTemplate.expire(key, OUT_PAINTING_TASK_SUBMIT_TIME_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 记录失败不影响核心业务：只会导致超时判定退化到使用 submitTime（或不判定）
            log.warn("记录扩图任务提交时间失败, taskId={}", taskId, e);
        }
    }

    /**
     * 若任务超过最大执行时间，则强制把任务标记为 FAILED（覆盖阿里云返回状态）。
     */
    public void applyOutPaintingTimeoutIfNeeded(String taskId, GetOutPaintingTaskResponse task, long nowMillis) {
        if (task == null || task.getOutput() == null) {
            return;
        }
        if (isTerminalStatus(task.getOutput().getTaskStatus())) {
            return;
        }

        Long submitMillis = getRecordedSubmitTimeMillis(taskId);
        if (submitMillis == null) {
            submitMillis = parseSubmitTimeMillis(task.getOutput().getSubmitTime());
        }
        if (submitMillis == null) {
            return; // 无法判定，保持原返回值
        }

        long maxExecMillis = OUT_PAINTING_MAX_EXEC_TIME.toMillis();
        if (nowMillis - submitMillis < maxExecMillis) {
            return;
        }

        // 超时：覆盖为 FAILED，给前端明确错误码/错误信息
        task.getOutput().setTaskStatus("FAILED");
        task.getOutput().setCode("TIMEOUT");
        task.getOutput().setMessage("任务执行超时（超过 10 分钟），已自动标记为失败");
        task.getOutput().setEndTime(formatEndTime(nowMillis));
        task.getOutput().setOutputImageUrl(null);

        log.info("扩图任务超时强制标记 FAILED, taskId={}, submitMillis={}, nowMillis={}",
                taskId, submitMillis, nowMillis);
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status);
    }

    private String buildOutPaintingSubmitTimeKey(String taskId) {
        return OUT_PAINTING_TASK_SUBMIT_TIME_KEY_PREFIX + taskId;
    }

    private Long getRecordedSubmitTimeMillis(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        try {
            String key = buildOutPaintingSubmitTimeKey(taskId);
            String value = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(value)) {
                return null;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseSubmitTimeMillis(String submitTime) {
        if (StrUtil.isBlank(submitTime)) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(submitTime, SUBMIT_TIME_FORMATTER_MS);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {
            // ignore
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(submitTime, SUBMIT_TIME_FORMATTER_NO_MS);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private String formatEndTime(long nowMillis) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), java.time.ZoneId.systemDefault());
        return dateTime.format(SUBMIT_TIME_FORMATTER_MS);
    }
}

