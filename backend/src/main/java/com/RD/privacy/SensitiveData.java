package com.RD.privacy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据脱敏注解
 *
 * <p>标注在实体字段上，由 {@link SensitiveDataAspect} 自动脱敏。
 * 配合 application.yml 中的 {@code sensitive-data.enabled} 开关使用。</p>
 *
 * <p>脱敏规则（详见 {@link DesensitizationUtil}）：</p>
 * <ul>
 *   <li>NAME       姓名 → "张*"（保留首字）</li>
 *   <li>ID_CARD    身份证 → "1101234**********"（保留前 7 位）</li>
 *   <li>BANK_CARD  银行卡 → "****5678"（保留后 4 位）</li>
 *   <li>MOBILE     手机号 → "138****8000"（中间 4 位 *）</li>
 *   <li>EMAIL      邮箱 → "a***@example.com"（@前保留首字）</li>
 *   <li>SALARY     薪资 → "****"（完全脱敏）</li>
 * </ul>
 *
 * @author Mavis
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {

    /**
     * 敏感数据类型
     */
    SensitiveType type();

    /**
     * 是否对超级管理员（ADMIN）也脱敏
     * 默认 false：ADMIN 可见明文（便于运维）
     */
    boolean maskForAdmin() default false;
}
