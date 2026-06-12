package com.RD.privacy;

import com.RD.common.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SensitiveDataAspect 单元测试
 *
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>{@code currentUserId()} 必须从 {@link org.springframework.security.core.Authentication#getName()} 取值，
 *       等于 {@code JwtAuthenticationFilter} 写入 SecurityContext 的 userId</li>
 *   <li>SecurityContext 为空时 {@code currentUserId()} 返回 null</li>
 *   <li>admin 角色默认不脱敏（{@code maskForAdmin=false}）</li>
 *   <li>非 admin 访问他人数据 → 脱敏</li>
 *   <li>本人访问自己数据 → 不脱敏</li>
 * </ul>
 *
 * @author Mavis
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensitiveDataAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature signature;

    private SensitiveDataAspect aspect;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        DesensitizationUtil desensitizationUtil = new DesensitizationUtil();
        SensitiveDataProperties properties = new SensitiveDataProperties();
        properties.setEnabled(true);
        aspect = new SensitiveDataAspect(desensitizationUtil, properties);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(sampleControllerMethod());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("currentUserId() 等于 SecurityContext 中 Authentication.getName()（userId）")
    void currentUserId_equalsAuthenticationName() {
        String expectedUserId = "user-001";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        expectedUserId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));

        String actual = invokeCurrentUserId();

        assertThat(actual)
                .as("currentUserId() 应等于 SecurityContext 中 Authentication.getName()")
                .isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("即使 principal 是复杂对象（非 String），getName() 仍能返回 userId")
    void currentUserId_worksWhenPrincipalIsComplexObject() {
        String expectedUserId = "user-002";
        Object customPrincipal = new Object() {
            @Override
            public String toString() {
                return "WRONG_TO_STRING_VALUE";
            }
        };
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        customPrincipal,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {
                    @Override
                    public String getName() {
                        return expectedUserId;
                    }
                });

        String actual = invokeCurrentUserId();

        assertThat(actual)
                .as("即使 principal.toString() 是错的，getName() 才是真相")
                .isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("未登录时（SecurityContext 为空）currentUserId() 返回 null")
    void currentUserId_nullWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        String actual = invokeCurrentUserId();

        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("admin 角色不受脱敏影响（默认 maskForAdmin=false）")
    void admin_shouldSeePlaintext() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        UserVO user = new UserVO("alice", "13800000001");
        when(pjp.proceed()).thenReturn(Result.success(user));

        Object result = aspect.around(pjp);

        Result<?> r = (Result<?>) result;
        UserVO resultUser = (UserVO) r.getData();
        assertThat(resultUser.getMobile())
                .as("ADMIN 默认可见明文")
                .isEqualTo("13800000001");
    }

    @Test
    @DisplayName("非 admin 用户访问他人数据 → mobile 字段被脱敏")
    void nonAdmin_otherUser_shouldBeMasked() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "bob", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));

        UserVO alice = new UserVO("alice", "13800000001");
        when(pjp.proceed()).thenReturn(Result.success(alice));

        Object result = aspect.around(pjp);

        Result<?> r = (Result<?>) result;
        UserVO resultUser = (UserVO) r.getData();
        assertThat(resultUser.getMobile())
                .as("非 admin 访问他人数据应被脱敏")
                .isEqualTo("138****0001");
    }

    @Test
    @DisplayName("本人访问自己数据 → mobile 字段不脱敏（userId 匹配豁免）")
    void self_shouldSeePlaintext() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));

        UserVO alice = new UserVO("alice", "13800000001");
        when(pjp.proceed()).thenReturn(Result.success(alice));

        Object result = aspect.around(pjp);

        Result<?> r = (Result<?>) result;
        UserVO resultUser = (UserVO) r.getData();
        assertThat(resultUser.getMobile())
                .as("本人访问自己数据应豁免脱敏")
                .isEqualTo("13800000001");
    }

    // ============================================
    // 工具方法
    // ============================================

    private String invokeCurrentUserId() {
        try {
            Method m = SensitiveDataAspect.class.getDeclaredMethod("currentUserId");
            m.setAccessible(true);
            return (String) m.invoke(aspect);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke currentUserId()", e);
        }
    }

    private Method sampleControllerMethod() throws NoSuchMethodException {
        return SampleController.class.getMethod("getUser", String.class);
    }

    @SuppressWarnings("unused")
    static class SampleController {
        public Result<UserVO> getUser(String userId) {
            return Result.success(new UserVO(userId, "13800000000"));
        }
    }

    @SuppressWarnings("unused")
    public static class UserVO {
        private String userId;
        @SensitiveData(type = SensitiveType.MOBILE)
        private String mobile;

        public UserVO() {}
        public UserVO(String userId, String mobile) {
            this.userId = userId;
            this.mobile = mobile;
        }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
    }
}
