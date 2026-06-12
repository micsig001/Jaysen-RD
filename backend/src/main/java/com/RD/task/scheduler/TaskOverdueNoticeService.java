package com.RD.task.scheduler;

import com.RD.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 任务超时通知服务（Phase 2.5 阶段占位）
 *
 * <p>当前仅写日志。后续接入 {@code WeWorkMessageService} 后，
 * 把 log.info 替换成实际的企微应用消息推送。</p>
 *
 * <p>为什么用占位而不是直接 null：</p>
 * <ul>
 *   <li>Scheduler 不强依赖 wework 模块（即使 wework 没配 corpId 也能跑）</li>
 *   <li>{@code @Value} 注入配置 + log 兜底，让本地 dev 立刻能验证 scheduler 行为</li>
 *   <li>替换为企微通知时，只需本文件新增 1 个方法 + Scheduler 改 1 行调用</li>
 * </ul>
 *
 * <p>开关：{@code wework.message.enabled} — false 时全走 log 跳过通知。</p>
 */
@Slf4j
@Service
public class TaskOverdueNoticeService {

    @Value("${wework.message.enabled:true}")
    private boolean weworkMessageEnabled;

    /**
     * 即将超时预警（执行方）
     */
    public void notifyTimeoutWarning(Task task, String assigneeUserId) {
        if (!weworkMessageEnabled) {
            log.debug("[任务超时-通知关闭] 跳过预警 任务={} 接收方={}", task.getId(), assigneeUserId);
            return;
        }
        // TODO Phase 3: 接入 WeWorkMessageService.sendAppMessage(assigneeUserId, content)
        log.info("[任务超时-预警] 任务={} 编号={} 标题='{}' 接收方={} 截止={}",
                task.getId(), task.getTaskNo(), task.getTitle(),
                assigneeUserId, task.getActualDeadline());
    }

    /**
     * 已超时严重告警（发起方 + ADMIN）
     */
    public void notifyOverdue(Task task, String operatorUserId, long overdueHours) {
        if (!weworkMessageEnabled) {
            log.debug("[任务超时-通知关闭] 跳过告警 任务={}", task.getId());
            return;
        }
        // TODO Phase 3: 接入 WeWorkMessageService.sendAppMessage(...)
        log.warn("[任务超时-已超时] 任务={} 编号={} 标题='{}' 已超时 {} 小时, 操作关注人={}",
                task.getId(), task.getTaskNo(), task.getTitle(),
                overdueHours, operatorUserId);
    }
}
