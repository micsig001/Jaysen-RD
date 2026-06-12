package com.RD.wework;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 企业微信数据同步定时任务
 *
 * <p>每天凌晨 2 点执行全量同步，每小时执行一次增量同步。</p>
 *
 * <p>Phase 1 状态：仅日志占位，{@link WeWorkSyncService} 待 Phase 2 搬入后启用。</p>
 *
 * @author Mavis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    /**
     * 每天凌晨 2 点执行全量同步
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledFullSync() {
        log.info("[企微同步] 触发定时全量同步任务（Phase 2 启用 WeWorkSyncService.fullSync）");
    }

    /**
     * 每小时执行一次增量同步
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledIncrementalSync() {
        log.info("[企微同步] 触发定时增量同步任务（Phase 2 启用 WeWorkSyncService.syncUsers）");
    }
}
