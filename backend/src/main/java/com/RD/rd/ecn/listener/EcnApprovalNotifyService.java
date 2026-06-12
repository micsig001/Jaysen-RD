package com.RD.rd.ecn.listener;

import com.RD.entity.EcnChange;
import com.RD.entity.SysUser;
import com.RD.mapper.EcnChangeMapper;
import com.RD.mapper.SysUserMapper;
import com.RD.wework.WeWorkMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ECN 审批通知服务
 *
 * <p>封装企微应用消息推送：</p>
 * <ul>
 *   <li>{@code notifyTaskAssigned} — 任务分配通知（给审批人），用文本卡片带详情链接</li>
 *   <li>{@code notifyApprovalResult} — 审批结果通知（给发起人），纯文本</li>
 *   <li>{@code notifyImplemented} — 实施完成通知（给发起人），纯文本</li>
 * </ul>
 *
 * <p>dev/test 环境（{@code wework.message.enabled=false} 或 {@code @Profile("dev | test")} 下），
 * 自动走 {@code WeWorkApiClient} 的 Fake 实现，log 模拟消息发送但不发真网络请求。</p>
 *
 * <p>关闭开关: {@code wework.message.enabled=false}（仅 log 不发）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcnApprovalNotifyService {

    private final EcnChangeMapper ecnChangeMapper;
    private final SysUserMapper userMapper;
    private final WeWorkMessageService weWorkMessageService;

    @Value("${wework.message.enabled:true}")
    private boolean weworkMessageEnabled;

    /**
     * 任务分配通知（给审批人）—— 文本卡片，点击跳详情页
     */
    public void notifyTaskAssigned(Long ecnId, String approverUserId, String taskName) {
        if (!weworkMessageEnabled) {
            log.debug("[ECN 通知-关闭] 跳过任务分配 ecnId={} approver={}", ecnId, approverUserId);
            return;
        }
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;

        SysUser approver = userMapper.selectByUserId(approverUserId);
        String approverName = approver != null ? approver.getName() : approverUserId;

        String title = "您有新的 ECN 待审批";
        String description = String.format(
                "编号: %s%n标题: %s%n紧急度: %s%n任务: %s",
                ecn.getEcnNumber(),
                ecn.getTitle(),
                ecn.getUrgency(),
                taskName
        );
        // 详情链接: 企微应用内打开 (后续可拼上 wework.oauth 配置的真实 URL)
        String url = "https://example.com/rd/ecn/" + ecn.getId();

        boolean ok = weWorkMessageService.sendTextcard(approverUserId, title, description, url, "查看详情");
        log.info("[ECN 通知-任务分配] ecnId={} approver={} task={} 发送结果={}",
                ecnId, approverName, taskName, ok ? "成功" : "失败");
    }

    /**
     * 审批结果通知（给发起人）—— 纯文本
     */
    public void notifyApprovalResult(Long ecnId, String approverName, boolean approved, String comment) {
        if (!weworkMessageEnabled) {
            log.debug("[ECN 通知-关闭] 跳过结果 ecnId={}", ecnId);
            return;
        }
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;

        String resultText = approved ? "通过" : "驳回";
        String content = String.format(
                "您的 ECN 已%s%n编号: %s%n标题: %s%n审批人: %s%s",
                resultText,
                ecn.getEcnNumber(),
                ecn.getTitle(),
                approverName,
                comment != null && !comment.isBlank() ? "%n意见: " + comment : ""
        );
        boolean ok = weWorkMessageService.sendText(ecn.getRequesterUserid(), content);
        log.info("[ECN 通知-审批结果] ecnId={} result={} approver={} 发送结果={}",
                ecnId, resultText, approverName, ok ? "成功" : "失败");
    }

    /**
     * 实施完成通知（给发起人）
     */
    public void notifyImplemented(Long ecnId) {
        if (!weworkMessageEnabled) {
            log.debug("[ECN 通知-关闭] 跳过实施完成 ecnId={}", ecnId);
            return;
        }
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;

        String content = String.format(
                "您的 ECN 已标记为实施完成%n编号: %s%n标题: %s",
                ecn.getEcnNumber(), ecn.getTitle()
        );
        boolean ok = weWorkMessageService.sendText(ecn.getRequesterUserid(), content);
        log.info("[ECN 通知-实施完成] ecnId={} 发送结果={}", ecnId, ok ? "成功" : "失败");
    }

    /**
     * 安全查 ecn（null 不抛异常，仅记 warn）
     */
    private EcnChange ecnChangeIdSafe(Long id) {
        if (id == null) {
            log.warn("[ECN 通知] ecnId 为空，跳过");
            return null;
        }
        EcnChange ecn = ecnChangeMapper.selectById(id);
        if (ecn == null) {
            log.warn("[ECN 通知] ecn({}) 不存在，跳过", id);
        }
        return ecn;
    }
}
