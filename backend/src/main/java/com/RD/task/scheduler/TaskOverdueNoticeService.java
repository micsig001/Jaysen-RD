package com.RD.task.scheduler;

import com.RD.entity.Task;
import com.RD.wework.WeWorkMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 任务超时通知服务
 *
 * <p>封装企微应用消息推送：</p>
 * <ul>
 *   <li>{@code notifyTimeoutWarning} — 即将超时预警（执行方），纯文本</li>
 *   <li>{@code notifyOverdue} — 已超时告警（执行方），纯文本</li>
 * </ul>
 *
 * <p>dev/test 环境自动走 Fake WeWorkApiClient（log 模拟），prod 走真实企微 API。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOverdueNoticeService {

    private final WeWorkMessageService weWorkMessageService;

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
        String content = String.format(
                "您负责的任务即将超时%n任务: %s%n标题: %s%n截止: %s",
                task.getTaskNo(), task.getTitle(), task.getActualDeadline()
        );
        boolean ok = weWorkMessageService.sendText(assigneeUserId, content);
        log.info("[任务超时-预警] 任务={} 编号={} 接收方={} 发送结果={}",
                task.getId(), task.getTaskNo(), assigneeUserId, ok ? "成功" : "失败");
    }

    /**
     * 已超时严重告警（执行方 + 后续可加发起方）
     */
    public void notifyOverdue(Task task, String operatorUserId, long overdueHours) {
        if (!weworkMessageEnabled) {
            log.debug("[任务超时-通知关闭] 跳过告警 任务={}", task.getId());
            return;
        }
        String content = String.format(
                "您负责的任务已超时 %d 小时%n任务: %s%n标题: %s",
                overdueHours, task.getTaskNo(), task.getTitle()
        );
        boolean ok = weWorkMessageService.sendText(operatorUserId, content);
        log.warn("[任务超时-已超时] 任务={} 编号={} 超时{}小时 操作人={} 发送结果={}",
                task.getId(), task.getTaskNo(), overdueHours, operatorUserId, ok ? "成功" : "失败");
    }
}
