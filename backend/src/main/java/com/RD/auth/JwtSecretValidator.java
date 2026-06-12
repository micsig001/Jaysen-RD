package com.RD.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * JWT Secret 启动校验器
 *
 * <p>生产环境（{@code prod} profile）必须显式注入强 JWT secret：</p>
 * <ul>
 *   <li>secret 长度 &lt; 32 字符时启动失败（HS256 要求 key ≥ 32 字节）</li>
 *   <li>secret 等于开发占位符时启动失败</li>
 * </ul>
 * <p>非 prod profile 仅打印警告，不阻塞启动，方便本地开发使用默认 secret。</p>
 *
 * @author Mavis
 */
@Component
public class JwtSecretValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);

    /** HS256 / jjwt Keys.hmacShaKeyFor 要求的最小字节数（256 bit = 32 字节） */
    private static final int MIN_SECRET_LENGTH = 32;

    /** 占位符 secret 长度恰好为 32，故意不补足，避免误判 */
    private static final String DEV_PLACEHOLDER =
            "dev-secret-CHANGE-ME-in-production-min-32-chars";

    private final Environment environment;
    private final String jwtSecret;

    public JwtSecretValidator(
            Environment environment,
            @Value("${jwt.secret}") String jwtSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isProd = isProdProfile();
        int length = jwtSecret == null ? 0 : jwtSecret.length();
        boolean isPlaceholder = DEV_PLACEHOLDER.equals(jwtSecret);

        log.info("JWT secret loaded: length={} bytes, profile={}",
                length, Arrays.toString(environment.getActiveProfiles()));

        if (!isProd) {
            // 非生产环境：只打印警告，不阻塞启动
            if (length < MIN_SECRET_LENGTH) {
                log.warn("[JWT] secret too short ({} < {}). "
                                + "OK for dev/test, will fail in prod profile.",
                        length, MIN_SECRET_LENGTH);
            }
            if (isPlaceholder) {
                log.warn("[JWT] secret is the dev placeholder. "
                        + "OK for dev/test, will fail in prod profile.");
            }
            return;
        }

        // 生产环境：任何违规都启动失败
        if (length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "[JWT] secret too short for prod profile: "
                            + length + " < " + MIN_SECRET_LENGTH
                            + " bytes. Set JWT_SECRET env var to a strong secret.");
        }
        if (isPlaceholder) {
            throw new IllegalStateException(
                    "[JWT] secret is the dev placeholder. "
                            + "Prod profile requires a real secret via JWT_SECRET env var.");
        }
    }

    private boolean isProdProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        // 兜底：default profile 算 prod 也算
        for (String profile : environment.getDefaultProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
