package com.imgvault.app.service;

import com.imgvault.common.enums.AsyncTaskStatus;
import com.imgvault.common.enums.AsyncTaskType;
import com.imgvault.domain.entity.AsyncTaskEntity;
import com.imgvault.domain.repository.AsyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * F23: 异步任务调度服务
 * 定时扫描任务表，执行待处理任务，失败自动重试（最多3次）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskRepository asyncTaskRepository;
    private final ImageAppService imageAppService;

    private static final int BATCH_SIZE = 10;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时扫描待处理任务（每 30 秒执行一次）
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processPendingTasks() {
        try {
            List<AsyncTaskEntity> pendingTasks = asyncTaskRepository.findPendingTasks(BATCH_SIZE);
            if (pendingTasks.isEmpty()) {
                return;
            }

            log.info("开始处理异步任务: {} 个", pendingTasks.size());
            for (AsyncTaskEntity task : pendingTasks) {
                processTask(task);
            }

        } catch (Exception e) {
            log.error("异步任务调度异常", e);
        }
    }

    /**
     * 定时扫描失败可重试任务（每 60 秒执行一次）
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void retryFailedTasks() {
        try {
            List<AsyncTaskEntity> failedTasks = asyncTaskRepository.findFailedRetryableTasks(BATCH_SIZE);
            if (failedTasks.isEmpty()) {
                return;
            }

            log.info("开始重试失败任务: {} 个", failedTasks.size());
            for (AsyncTaskEntity task : failedTasks) {
                asyncTaskRepository.incrementRetryCount(task.getId());
                asyncTaskRepository.updateStatus(task.getId(),
                        AsyncTaskStatus.PENDING.getCode(), null);
                processTask(task);
            }

        } catch (Exception e) {
            log.error("异步任务重试异常", e);
        }
    }

    /**
     * 执行单个任务
     */
    private void processTask(AsyncTaskEntity task) {
        try {
            asyncTaskRepository.updateStatus(task.getId(),
                    AsyncTaskStatus.PROCESSING.getCode(), null);
            asyncTaskRepository.updateExecutedAt(task.getId(),
                    LocalDateTime.now().format(FORMATTER));

            AsyncTaskType taskType = AsyncTaskType.fromCode(task.getTaskType());

            switch (taskType) {
                case EXIF_EXTRACT:
                    imageAppService.asyncExtractExif(task.getImageId());
                    break;
                case THUMBNAIL_GENERATE:
                    // imgproxy 实时生成，此处无需处理
                    log.info("缩略图由 imgproxy 实时生成，跳过: imageId={}", task.getImageId());
                    break;
                case FORMAT_CONVERT:
                    // imgproxy 实时转换，此处无需处理
                    log.info("格式转换由 imgproxy 实时处理，跳过: imageId={}", task.getImageId());
                    break;
                case IMAGE_HASH:
                    // 感知哈希计算（Phase 4 F26 扩展）
                    log.info("感知哈希计算功能待实现: imageId={}", task.getImageId());
                    break;
                default:
                    log.warn("未知的任务类型: {}", task.getTaskType());
                    break;
            }

            asyncTaskRepository.updateStatus(task.getId(),
                    AsyncTaskStatus.SUCCESS.getCode(), null);
            log.debug("任务执行成功: id={}, type={}", task.getId(), task.getTaskType());

        } catch (Exception e) {
            log.error("任务执行失败: id={}, type={}, error={}",
                    task.getId(), task.getTaskType(), e.getMessage());
            asyncTaskRepository.updateStatus(task.getId(),
                    AsyncTaskStatus.FAILED.getCode(), e.getMessage());
        }
    }

    /**
     * 获取各状态任务统计
     */
    public java.util.Map<String, Integer> getTaskStats() {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        for (AsyncTaskStatus status : AsyncTaskStatus.values()) {
            stats.put(status.getCode(), asyncTaskRepository.countByStatus(status.getCode()));
        }
        return stats;
    }
}
