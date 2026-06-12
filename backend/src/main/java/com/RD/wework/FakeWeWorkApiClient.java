package com.RD.wework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 假的 WeWorkApiClient —— dev / test 环境用
 *
 * <p>不真发网络请求,所有接口返回模拟数据。覆盖 {@link WeWorkApiClient} 的网络方法,
 * 业务侧 {@link WeWorkAuthService} / {@link WeWorkMessageService} 无感知。</p>
 *
 * <p>激活方式: spring.profiles.active = dev 或 test (在 {@link WeWorkApiConfig} 里通过
 * {@code @Profile("dev | test")} 限定注册)。</p>
 *
 * <p>预置测试用户 (userId → name):</p>
 * <ul>
 *   <li>{@code ZhangSan} → 张三 (EMPLOYEE)</li>
 *   <li>{@code LiSi} → 李四 (MANAGER)</li>
 *   <li>{@code admin} → 管理员 (ADMIN)</li>
 * </ul>
 *
 * <p>OAuth 流程模拟:</p>
 * <ol>
 *   <li>前端用任意 code 调用 /api/auth/token, FakeWeWorkApiClient.getUserInfoByCode 把 code
 *       当成 userId 解析, 命中预置用户或返回 "guest"</li>
 *   <li>getUserDetail 返回该 userId 的姓名/手机/邮箱/部门 mock 数据</li>
 *   <li>accessToken 用 UUID 模拟, gettoken 接口不调网络, 直接返回 UUID</li>
 * </ol>
 *
 * <p>使用方式: dev 启动时设 {@code -Dspring.profiles.active=dev} (或环境变量
 * {@code SPRING_PROFILES_ACTIVE=dev}),自动注册本类。</p>
 *
 * @author Mavis
 */
@Slf4j
@Profile({"dev", "test"})
public class FakeWeWorkApiClient extends WeWorkApiClient {

    private final ObjectMapper objectMapper;
    private final String fakeAccessToken = "FAKE_ACCESS_TOKEN_" + UUID.randomUUID();

    /** 预置测试用户 userId → 展示名 */
    private static final Map<String, String> PREDEFINED_USERS = Map.of(
            "ZhangSan", "张三",
            "LiSi", "李四",
            "admin", "管理员",
            "WangWu", "王五"
    );

    /** 已发送消息计数器 (仅 log 用) */
    private final java.util.concurrent.atomic.AtomicInteger messageCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    public FakeWeWorkApiClient(WebClient.Builder webClientBuilder,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        super(webClientBuilder, redisTemplate, objectMapper);
        this.objectMapper = objectMapper;
        log.warn("[WeWork-Fake] 初始化 FakeWeWorkApiClient —— 不会真发网络请求, 所有接口返回 mock 数据");
        log.warn("[WeWork-Fake] 预置用户: {}", PREDEFINED_USERS.keySet());
    }

    /**
     * 模拟 access_token —— 走继承的 getAccessToken 父类方法会先查 redis, 我们 override 直接返回 fakeToken
     */
    @Override
    public String getAccessToken() {
        log.debug("[WeWork-Fake] getAccessToken() → 返回固定 fake token");
        return fakeAccessToken;
    }

    /**
     * 模拟 getuserinfo —— 把 code 当成 userId 处理
     */
    @Override
    public JsonNode getUserInfoByCode(String code) {
        log.info("[WeWork-Fake] getUserInfoByCode({})", code);
        try {
            ObjectMapper m = new ObjectMapper();
            // 优先用预置用户, 否则把 code 直接当 userId
            String userId = PREDEFINED_USERS.containsKey(code) ? code : code;
            return m.createObjectNode()
                    .put("errcode", 0)
                    .put("errmsg", "ok")
                    .put("UserId", userId);
        } catch (Exception e) {
            log.error("[WeWork-Fake] getUserInfoByCode 失败", e);
            return null;
        }
    }

    /**
     * 模拟 user/get —— 返回 mock 用户详情
     */
    @Override
    public JsonNode getUserDetail(String userId) {
        log.info("[WeWork-Fake] getUserDetail({})", userId);
        try {
            ObjectMapper m = new ObjectMapper();
            String name = PREDEFINED_USERS.getOrDefault(userId, userId);
            return m.createObjectNode()
                    .put("errcode", 0)
                    .put("errmsg", "ok")
                    .put("userid", userId)
                    .put("name", name)
                    .put("mobile", "13800000000")
                    .put("email", userId.toLowerCase() + "@rd.example.com")
                    .put("avatar", "https://placehold.co/100x100?text=" + name)
                    .putArray("department").add(1L);
        } catch (Exception e) {
            log.error("[WeWork-Fake] getUserDetail 失败", e);
            return null;
        }
    }

    /**
     * 模拟 department/list
     */
    @Override
    public JsonNode getDepartmentList(Long departmentId) {
        log.info("[WeWork-Fake] getDepartmentList({})", departmentId);
        try {
            ObjectMapper m = new ObjectMapper();
            return m.createObjectNode()
                    .put("errcode", 0)
                    .put("errmsg", "ok")
                    .set("department", m.createArrayNode()
                            .add(m.createObjectNode()
                                    .put("id", 1L)
                                    .put("name", "研发部")
                                    .put("parentid", 0L)
                                    .put("order", 1))
                            .add(m.createObjectNode()
                                    .put("id", 2L)
                                    .put("name", "硬件组")
                                    .put("parentid", 1L)
                                    .put("order", 1)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 模拟 department/simplelist
     */
    @Override
    public JsonNode getDepartmentUsers(Long departmentId, Boolean fetchChild) {
        log.info("[WeWork-Fake] getDepartmentUsers({}, {})", departmentId, fetchChild);
        try {
            ObjectMapper m = new ObjectMapper();
            return m.createObjectNode()
                    .put("errcode", 0)
                    .put("errmsg", "ok")
                    .set("userlist", m.createArrayNode()
                            .add(m.createObjectNode().put("userid", "ZhangSan").put("name", "张三"))
                            .add(m.createObjectNode().put("userid", "LiSi").put("name", "李四")));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 模拟 message/send —— 记录到内存 + log
     */
    @Override
    public boolean sendMessage(Map<String, Object> messageBody) {
        int seq = messageCounter.incrementAndGet();
        log.info("[WeWork-Fake] sendMessage #{} → touser={} msgtype={} agentid={}",
                seq, messageBody.get("touser"), messageBody.get("msgtype"), messageBody.get("agentid"));
        if (messageBody.containsKey("text")) {
            @SuppressWarnings("unchecked")
            Map<String, String> text = (Map<String, String>) messageBody.get("text");
            log.info("[WeWork-Fake]   text.content: {}", text.get("content"));
        }
        if (messageBody.containsKey("textcard")) {
            @SuppressWarnings("unchecked")
            Map<String, String> card = (Map<String, String>) messageBody.get("textcard");
            log.info("[WeWork-Fake]   textcard.title: {} description: {} url: {}",
                    card.get("title"), card.get("description"), card.get("url"));
        }
        return true; // 永远成功
    }
}
