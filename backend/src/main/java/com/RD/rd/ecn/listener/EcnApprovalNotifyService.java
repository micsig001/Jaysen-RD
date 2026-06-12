package com.RD.rd.ecn.listener;

import com.RD.entity.EcnChange;
import com.RD.entity.SysUser;
import com.RD.mapper.EcnChangeMapper;
import com.RD.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ECN 审批通知服务（Phase 2.6 阶段占位）
 *
 * <p>当前仅写日志。后续接入 {@code WeWorkMessageService} 后，把 log.info 替换成实际的企微应用消息推送。</p>
 *
 * <p>为什么用占位：</p>
 * <ul>
 *   <li>不依赖 wework 模块（即使 wework 没配 corpId 也能跑）</li>
 *   <li>{@code @Value} 注入配置 + log 兜底，让本地 dev 立刻能验证通知触发点</li>
 * </ul>
 *
 * <p>开关：{@code wework.message.enabled} — false 时全走 log 跳过通知。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcnApprovalNotifyService {

    private final EcnChangeMapper ecnChangeMapper;
    private final SysUserMapper userMapper;

    @Value("${wework.message.enabled:true}")
    private boolean weworkMessageEnabled;

    /**
     * 任务分配通知（给审批人）
     *
     * @param ecnId          ECN 主键
     * @param approverUserId 审批人 UserID
     * @param taskName       任务名（如 "部门负责人审批" / "技术总工审批"）
     */
    public void notifyTaskAssigned(Long ecnId, String approverUserId, String taskName) {
        if (!weworkMessageEnabled) {
            log.debug("[ECN 通知-关闭] 跳过任务分配通知 ecnId={} approver={}", ecnId, approverUserId);
            return;
        }
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;

        SysUser approver = userMapper.selectByUserId(approverUserId);
        String approverName = approver != null ? approver.getName() : approverUserId;

        // TODO Phase 3+: 接入 WeWorkMessageService.sendAppMessage(approverUserId, content)
        log.info("[ECN 通知-任务分配] 任务={} 审批人={} ECN={} 编号={} 标题='{}' 紧急度={}",
                taskName, approverName, ecn.getId(), ecn.getEcnNumber(),
                ecn.getTitle(), ecn.getUrgency());

        // 实际推送时 content 示例:
        // 您有新的 ECN 待审批
        // 编号: ECN202606120001
        // 标题: 频谱分析仪主板BOM变更
        // 紧急度: 紧急
        // 截止: 2026-06-25
        // 点击查看详情: https://rd.example.com/rd/ecn/{id}
    }

    /**
     * 审批结果通知（给发起人）
     *
     * @param ecnId          ECN 主键
     * @param approverName   审批人姓名
     * @param approved       true=通过 / false=驳回
     * @param comment        审批意见
     */
    public void notifyApprovalResult(Long ecnId, String approverName, boolean approved, String comment) {
        if (!weworkMessageEnabled) {
            log.debug("[ECN 通知-关闭] 跳过结果通知 ecnId={}", ecnId);
            return;
        }
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;

        String resultText = approved ? "通过" : "驳回";
        log.info("[ECN 通知-审批结果] ECN={} 编号={} 标题='{}' {} 审批人={} 意见='{}'",
                ecn.getId(), ecn.getEcnNumber(), ecn.getTitle(),
                resultText, approverName, comment);
    }

    /**
     * 实施完成通知（给发起人）
     */
    public void notifyImplemented(Long ecnId) {
        if (!weworkMessageEnabled) return;
        EcnChange ecn = ecnChangeIdSafe(ecnId);
        if (ecn == null) return;
        log.info("[ECN 通知-实施完成] ECN={} 编号={} 标题='{}'",
                ecn.getId(), ecn.getEcnNumber(), ecn.getTitle());
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
