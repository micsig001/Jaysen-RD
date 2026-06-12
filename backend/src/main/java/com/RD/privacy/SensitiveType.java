package com.RD.privacy;

/**
 * 敏感数据类型枚举
 *
 * <p>与 {@link SensitiveData} 注解配合使用，决定 {@link DesensitizationUtil} 走哪条脱敏规则。</p>
 *
 * @author Mavis
 */
public enum SensitiveType {

    /** 姓名 */
    NAME,

    /** 身份证号 */
    ID_CARD,

    /** 银行卡号 */
    BANK_CARD,

    /** 手机号 */
    MOBILE,

    /** 邮箱 */
    EMAIL,

    /** 薪资 */
    SALARY,

    /** 自定义（默认全 *，业务可在 DesensitizationUtil 中扩展） */
    CUSTOM
}
