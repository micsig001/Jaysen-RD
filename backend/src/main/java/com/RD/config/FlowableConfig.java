package com.RD.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Flowable 7.1.0 引擎配置
 *
 * <p>当前 Phase 1 阶段：</p>
 * <ul>
 *   <li>{@code flowable.database-schema-update=true} 启动时自动建 ACT_* 表</li>
 *   <li>{@code flowable.rest.{app,admin}.enabled=false} 不暴露 Flowable 自带 REST</li>
 *   <li>暂不提供 {@link ProcessEngine} Bean（后续 Phase 2 引入 ECN 业务时按需暴露）</li>
 * </ul>
 *
 * <p>Phase 2 待办：</p>
 * <ol>
 *   <li>暴露 {@code ProcessEngine} / {@code RepositoryService} / {@code TaskService} 等核心服务 Bean</li>
 *   <li>实现 {@code @PostConstruct} 业务索引兜底（带 IF NOT EXISTS）</li>
 *   <li>配置自定义 SessionFactory，复用 {@code sys_user.wework_userid} 作为 Flowable 用户身份</li>
 *   <li>注册部门负责人动态分配监听器 {@code DepartmentLeaderAssignmentListener}</li>
 * </ol>
 *
 * @author Mavis
 */
@Slf4j
@Configuration
public class FlowableConfig {

    /**
     * 启动期诊断日志：检查 Flowable 是否能被加载
     */
    @PostConstruct
    public void init() {
        log.info("[Flowable] Phase 1 骨架模式：database-schema-update=true, REST API 关闭");
        log.info("[Flowable] Phase 2 待办：暴露 ProcessEngine Bean、注册自定义 SessionFactory");
    }

    /**
     * Phase 2 占位：暴露 ProcessEngine Bean（当前未启用，避免 Flowable 提前实例化）
     *
     * <p>启用方式：去掉 {@code @Bean} 注释即可。Phase 2 接 ECN 流程时再开。</p>
     */
    // @Bean
    public ProcessEngine processEngine() {
        return ProcessEngines.getDefaultProcessEngine();
    }
}
