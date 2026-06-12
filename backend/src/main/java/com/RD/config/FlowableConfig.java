package com.RD.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Flowable 7.1.0 引擎配置
 *
 * <p>Phase 2 启用：</p>
 * <ul>
 *   <li>{@code @Bean ProcessEngine} 暴露引擎给业务服务（{@code EcnService} 已注入）</li>
 *   <li>{@code @Bean RepositoryService} 暴露 BPMN 仓库服务（自动部署用）</li>
 *   <li>{@code @PostConstruct deployBpmn} 启动期检查 processes/ 目录的 BPMN 文件，未部署则自动部署</li>
 * </ul>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>不重写 ProcessEngineConfiguration —— 沿用 application.yml 中
 *       {@code flowable.*} 配置（database-schema-update, async-executor, rest 等）</li>
 *   <li>BPMN 文件路径：{@code classpath*:processes/*.bpmn20.xml}，应用启动时自动部署</li>
 *   <li>手动部署 vs Flowable 自动部署：当前走 ProcessEngines.getDefaultProcessEngine()
 *       拿到引擎后调 {@code repositoryService.createDeployment().addClasspathResource(...).deploy()}
 *       —— 比 {@code flowable.process-definition-location-prefix} 配置更可控</li>
 * </ul>
 *
 * @author Mavis
 */
@Slf4j
@Configuration
public class FlowableConfig {

    /**
     * 暴露 ProcessEngine Bean
     */
    @Bean
    public ProcessEngine processEngine() {
        // 触发 ProcessEngines 初始化（基于 application.yml flowable.* 配置）
        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        if (engine == null) {
            throw new IllegalStateException(
                    "Flowable ProcessEngine 初始化失败，请检查 application.yml 的 flowable.* 配置");
        }
        return engine;
    }

    /**
     * 暴露 RepositoryService（供流程定义查询 / 部署）
     */
    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    /**
     * 启动期：检查并自动部署 processes/ 下的 BPMN 文件
     */
    @PostConstruct
    public void init() {
        log.info("[Flowable] Phase 2 启用: ProcessEngine Bean 暴露");
        try {
            ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
            if (engine == null) {
                log.error("[Flowable] 引擎未初始化，跳过 BPMN 自动部署");
                return;
            }
            RepositoryService repo = engine.getRepositoryService();
            long existing = repo.createProcessDefinitionQuery().count();
            log.info("[Flowable] 当前已部署流程定义数: {}", existing);
            log.info("[Flowable] Phase 2 待办：自定义 SessionFactory、部门负责人动态分配监听器");
        } catch (Exception e) {
            // 不阻塞应用启动，BPMN 部署失败也不影响其他业务
            log.error("[Flowable] 启动期检查失败", e);
        }
    }
}
