package com.RD.task.scheduler;

import com.RD.entity.Task;
import com.RD.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务超时扫描定时任务
 *
 * <p>两个 cron：</p>
 * <ul>
 *   <li>每小时整点：扫描"即将超时"任务（1 小时内到期），发预警给接收方</li>
 *   <li>每天凌晨 1 点：扫描"已超时"任务（IN_PROGRESS / PENDING_VERIFY），记日志 + 告警</li>
 * </ul>
 *
 * <p>注：</p>
 * <ul>
 *   <li>cron 时区跟随 JVM 默认（生产环境 JVM 应设为 Asia/Shanghai）</li>
 *   <li>单实例部署下 cron 不会重复执行；多实例部署需加 ShedLock 分布式锁（Phase 3+）</li>
 *   <li>通知发送失败不阻塞扫描主流程，单任务失败 try-catch 隔离</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTimeoutScheduler {

    private final TaskMapper taskMapper;
    private final TaskOverdueNoticeService noticeService;

    @Value("${wework.message.timeout-warning-minutes:60}")
    private int timeoutWarningMinutes;

    /**
     * 每小时整点：扫描即将超时的任务（1 小时内到期）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkTimeoutTasks() {
        log.info("[任务超时扫描] 开始 - 即将超时检查");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningThreshold = now.plusMinutes(timeoutWarningMinutes);

            // 条件: IN_PROGRESS + 有截止时间 + 截止时间在 [now, now+warningMinutes] 内
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Task::getStatus, "IN_PROGRESS")
                    .isNotNull(Task::getActualDeadline)
                    .le(Task::getActualDeadline, warningThreshold)
                    .gt(Task::getActualDeadline, now);

            List<Task> tasks = taskMapper.selectList(wrapper);
            if (tasks.isEmpty()) {
                log.info("[任务超时扫描] 没有即将超时的任务");
                return;
            }
            log.info("[任务超时扫描] 发现 {} 个即将超时的任务 (阈值: {} 分钟)",
                    tasks.size(), timeoutWarningMinutes);

            for (Task task : tasks) {
                try {
                    noticeService.notifyTimeoutWarning(task, task.getAssigneeId());
                } catch (Exception e) {
                    log.error("[任务超时扫描] 发送任务 {} 的预警通知失败", task.getId(), e);
                }
            }
            log.info("[任务超时扫描] 即将超时检查完成");
        } catch (Exception e) {
            log.error("[任务超时扫描] 即将超时检查异常", e);
        }
    }

    /**
     * 每天凌晨 1 点：扫描已超时任务
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdueTasks() {
        log.info("[任务超时扫描] 开始 - 已超时检查");

        try {
            LocalDateTime now = LocalDateTime.now();
            LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Task::getStatus, "IN_PROGRESS", "PENDING_VERIFY")
                    .isNotNull(Task::getActualDeadline)
                    .lt(Task::getActualDeadline, now);

            List<Task> overdueTasks = taskMapper.selectList(wrapper);
            if (overdueTasks.isEmpty()) {
                log.info("[任务超时扫描] 没有已超时的任务");
                return;
            }
            log.warn("[任务超时扫描] 发现 {} 个已超时的任务", overdueTasks.size());

            for (Task task : overdueTasks) {
                long overdueHours = Duration.between(task.getActualDeadline(), now).toHours();
                log.warn("[任务超时扫描] 任务 {} 已超时 {} 小时, 状态: {}, 接收方: {}",
                        task.getId(), overdueHours, task.getStatus(), task.getAssigneeId());

                try {
                    // 告警发接收方（让其知道超时）+ 发起方（让其知道要催/驳回）
                    noticeService.notifyOverdue(task, task.getAssigneeId(), overdueHours);
                } catch (Exception e) {
                    log.error("[任务超时扫描] 发送任务 {} 的超时告警失败", task.getId(), e);
                }
            }
            log.info("[任务超时扫描] 已超时检查完成");
        } catch (Exception e) {
            log.error("[任务超时扫描] 已超时检查异常", e);
        }
    }
}
