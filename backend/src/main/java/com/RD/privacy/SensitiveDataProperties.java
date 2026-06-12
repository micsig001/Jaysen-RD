package com.RD.privacy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 敏感数据脱敏配置
 *
 * <p>通过 application.yml 中的 {@code sensitive-data.enabled} 控制全局开关。</p>
 *
 * @author Mavis
 */
@Data
@Component
@ConfigurationProperties(prefix = "sensitive-data")
public class SensitiveDataProperties {

    /**
     * 是否启用脱敏
     * true：所有标了 @SensitiveData 的字段都脱敏
     * false：不脱敏（开发环境可关）
     */
    private Boolean enabled = true;
}
