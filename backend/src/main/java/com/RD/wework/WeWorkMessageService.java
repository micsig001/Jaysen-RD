package com.RD.wework;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 企微应用消息发送服务
 *
 * <p>对 {@link WeWorkApiClient} 的 sendMessage + buildXxxMessage 做业务级封装，
 * 给业务模块（ECN 通知 / 任务超时预警）提供更友好的调用接口。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li>内部用 {@code @Value("\${wework.message.enabled:true}")} 读开关,
 *       dev 环境设 {@code WEWORK_MESSAGE_ENABLED=false} 即可全局静默</li>
 *   <li>调用 {@code WeWorkApiClient.sendMessage} 而非直接发 HTTP, 由 Spring 注入的
 *       {@code WeWorkApiClient} 决定走真实 API 还是 Fake (dev/test 自动走 Fake)</li>
 *   <li>不抛异常: 发送失败只 log, 不影响业务流程</li>
 * </ul>
 *
 * @author Mavis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeWorkMessageService {

    private final WeWorkApiClient weWorkApiClient;

    @Value("${wework.message.enabled:true}")
    private boolean weworkMessageEnabled;

    /**
     * 发送文本消息
     *
     * @param toUser  接收人 UserID（多个用 '|' 分隔, ' @all' 表示全员）
     * @param content 文本内容
     * @return 发送成功 / 失败
     */
    public boolean sendText(String toUser, String content) {
        if (!weworkMessageEnabled) {
            log.debug("[WeWork-消息] 开关关闭, 跳过发送 toUser={} content={}", toUser, content);
            return false;
        }
        try {
            Map<String, Object> body = weWorkApiClient.buildTextMessage(toUser, content);
            return weWorkApiClient.sendMessage(body);
        } catch (Exception e) {
            log.error("[WeWork-消息] 发送文本失败 toUser={}", toUser, e);
            return false;
        }
    }

    /**
     * 发送文本卡片（带标题+描述+跳转链接+按钮）
     *
     * <p>适用场景: 审批待办、变更通知（点击卡片跳详情页）</p>
     */
    public boolean sendTextcard(String toUser, String title, String description, String url, String buttonText) {
        if (!weworkMessageEnabled) {
            log.debug("[WeWork-消息] 开关关闭, 跳过发送卡片 toUser={} title={}", toUser, title);
            return false;
        }
        try {
            Map<String, Object> body = weWorkApiClient.buildCardMessage(toUser, title, description, url, buttonText);
            return weWorkApiClient.sendMessage(body);
        } catch (Exception e) {
            log.error("[WeWork-消息] 发送卡片失败 toUser={}", toUser, e);
            return false;
        }
    }

    /**
     * 发送待办卡片（template_card，特殊审批场景）
     */
    public boolean sendTodo(String toUser, String title, String description) {
        if (!weworkMessageEnabled) {
            log.debug("[WeWork-消息] 开关关闭, 跳过发送待办 toUser={} title={}", toUser, title);
            return false;
        }
        try {
            Map<String, Object> body = weWorkApiClient.buildTodoMessage(toUser, title, description);
            return weWorkApiClient.sendMessage(body);
        } catch (Exception e) {
            log.error("[WeWork-消息] 发送待办失败 toUser={}", toUser, e);
            return false;
        }
    }
}
