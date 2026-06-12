package com.RD.task.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 任务编号生成器
 *
 * <p>格式：{@code T + yyyyMMdd + 4 位随机数字}，例如 {@code T202606121389}。</p>
 *
 * <p>为什么不用雪花/Snowflake：单库单服务场景下，日期+随机数已足够业务去重；
 * 真正高并发分布式场景可后续切 Snowflake（Phase 3 起步时统一升级）。</p>
 *
 * <p>为什么不用数据库自增：{@code task.task_no} 是 {@code UNIQUE} 业务编号，
 * 与主键 {@code id} 分离，便于对外引用（企微消息、日志追踪）。</p>
 */
@Component
public class TaskNoGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int RANDOM_BOUND = 10_000;

    /**
     * 生成下一个任务编号
     */
    public String generate() {
        String datePart = LocalDate.now().format(DATE_FMT);
        int randomPart = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        return "T" + datePart + String.format("%04d", randomPart);
    }
}
