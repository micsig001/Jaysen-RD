package com.RD.wework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 企业微信 API 客户端
 *
 * <p>封装企微接口调用，包括 AccessToken 管理、用户信息获取、消息推送等。</p>
 *
 * <p>本身不是 {@code @Component}，由 {@link WeWorkApiConfig} 注册为 Bean。</p>
 *
 * @author Mavis
 */
@Slf4j
public class WeWorkApiClient {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com";
    private static final String ACCESS_TOKEN_KEY = "wework:access_token";
    private static final int ACCESS_TOKEN_TTL = 7000; // 略小于官方 7200 秒有效期

    private final WebClient webClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wework.corp-id:}")
    private String corpId;

    @Value("${wework.secret:}")
    private String secret;

    @Value("${wework.agent-id:0}")
    private String agentId;

    public WeWorkApiClient(WebClient.Builder webClientBuilder,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper) {
        this.webClient = (webClientBuilder != null) ? webClientBuilder.baseUrl(BASE_URL).build() : null;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 Access Token（带缓存）
     */
    public String getAccessToken() {
        String cachedToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_KEY);
        if (cachedToken != null) {
            log.debug("使用缓存的 Access Token");
            return cachedToken;
        }

        String token = fetchAccessTokenFromApi();
        if (token != null) {
            redisTemplate.opsForValue().set(ACCESS_TOKEN_KEY, token, ACCESS_TOKEN_TTL, TimeUnit.SECONDS);
            log.info("成功获取新的 Access Token");
        }
        return token;
    }

    private String fetchAccessTokenFromApi() {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/gettoken")
                            .queryParam("corpid", corpId)
                            .queryParam("corpsecret", secret)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                return jsonNode.get("access_token").asText();
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("获取 Access Token 失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("获取 Access Token 异常", e);
            return null;
        }
    }

    /**
     * 通过授权码获取用户信息
     */
    public JsonNode getUserInfoByCode(String code) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("无法获取 Access Token");
            return null;
        }

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/auth/getuserinfo")
                            .queryParam("access_token", accessToken)
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                return jsonNode;
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("通过授权码获取用户信息失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("通过授权码获取用户信息异常", e);
            return null;
        }
    }

    /**
     * 获取用户详细信息
     */
    public JsonNode getUserDetail(String userId) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("无法获取 Access Token");
            return null;
        }

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/user/get")
                            .queryParam("access_token", accessToken)
                            .queryParam("userid", userId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                return jsonNode;
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("获取用户详细信息失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("获取用户详细信息异常", e);
            return null;
        }
    }

    /**
     * 获取部门列表
     */
    public JsonNode getDepartmentList(Long departmentId) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            return null;
        }

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/department/list")
                            .queryParam("access_token", accessToken)
                            .queryParam("id", departmentId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                return jsonNode;
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("获取部门列表失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("获取部门列表异常", e);
            return null;
        }
    }

    /**
     * 获取部门成员详情
     */
    public JsonNode getDepartmentUsers(Long departmentId, Boolean fetchChild) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            return null;
        }

        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/user/simplelist")
                            .queryParam("access_token", accessToken)
                            .queryParam("department_id", departmentId)
                            .queryParam("fetch_child", fetchChild ? 1 : 0)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                return jsonNode;
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("获取部门成员失败: errcode={}, errmsg={}", errCode, errMsg);
                return null;
            }
        } catch (Exception e) {
            log.error("获取部门成员异常", e);
            return null;
        }
    }

    /**
     * 发送应用消息
     */
    public boolean sendMessage(Map<String, Object> messageBody) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            return false;
        }

        try {
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cgi-bin/message/send")
                            .queryParam("access_token", accessToken)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(messageBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            int errCode = jsonNode.get("errcode").asInt();

            if (errCode == 0) {
                log.info("消息发送成功: {}", messageBody.get("touser"));
                return true;
            } else {
                String errMsg = jsonNode.get("errmsg").asText();
                log.error("消息发送失败: errcode={}, errmsg={}", errCode, errMsg);
                return false;
            }
        } catch (Exception e) {
            log.error("消息发送异常", e);
            return false;
        }
    }

    /**
     * 构建文本消息体
     */
    public Map<String, Object> buildTextMessage(String toUser, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("touser", toUser);
        message.put("msgtype", "text");
        message.put("agentid", parseAgentId());

        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        message.put("text", text);

        return message;
    }

    /**
     * 构建卡片消息体
     */
    public Map<String, Object> buildCardMessage(String toUser, String title, String description,
                                                 String url, String buttonText) {
        Map<String, Object> message = new HashMap<>();
        message.put("touser", toUser);
        message.put("msgtype", "textcard");
        message.put("agentid", parseAgentId());

        Map<String, String> card = new HashMap<>();
        card.put("title", title);
        card.put("description", description);
        card.put("url", url);
        card.put("btntxt", buttonText != null ? buttonText : "查看详情");
        message.put("textcard", card);

        return message;
    }

    /**
     * 构建待办消息体
     */
    public Map<String, Object> buildTodoMessage(String toUser, String title, String description) {
        Map<String, Object> message = new HashMap<>();
        message.put("touser", toUser);
        message.put("msgtype", "template_card");
        message.put("agentid", parseAgentId());

        Map<String, Object> templateCard = new HashMap<>();
        templateCard.put("card_type", "todo");

        Map<String, Object> todo = new HashMap<>();
        todo.put("title", title);
        todo.put("description", description);
        templateCard.put("todo", todo);

        message.put("template_card", templateCard);

        return message;
    }

    private int parseAgentId() {
        try {
            return Integer.parseInt(agentId);
        } catch (NumberFormatException e) {
            log.warn("wework.agent-id 解析失败: {}", agentId);
            return 0;
        }
    }
}
