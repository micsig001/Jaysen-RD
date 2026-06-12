package com.RD.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * BPMN 启动期部署器
 *
 * <p>扫描 {@code classpath*:processes/*.bpmn20.xml} 下的所有流程定义文件，
 * 启动时自动部署到 Flowable 引擎。</p>
 *
 * <p>与 {@code flowable.process-definition-location-prefix} 配置的关系：</p>
 * <ul>
 *   <li>如果 application.yml 配了 {@code flowable.process-definition-location-prefix: classpath*:processes/}，
 *       Flowable 启动时会自己扫描 —— 这种情况下本类多余（但仍安全，重复部署会被 Flowable 去重）</li>
 *   <li>当前 RD 没用配置方式，依赖本类手动部署</li>
 * </ul>
 *
 * <p>为什么写本类：</p>
 * <ol>
 *   <li>BPMN 部署是个明确的"启动期动作"，代码里显式比配置文件更可控</li>
 *   <li>未来想加动态启用/禁用某个流程定义（比如灰度发布），只需在 deploy 之前判断</li>
 *   <li>日志显式记录已部署的 BPMN 文件名，方便排查</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmnDeployer {

    private final RepositoryService repositoryService;

    private static final String BPMN_PATTERN = "classpath*:processes/*.bpmn20.xml";

    @PostConstruct
    public void deployBpmnFiles() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(BPMN_PATTERN);
            if (resources.length == 0) {
                log.warn("[BPMN] 未找到任何流程定义文件 ({})", BPMN_PATTERN);
                return;
            }
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                try {
                    // name 参数用于 Flowable deployment 标识，重启后用同一 name 会去重更新
                    Deployment deployment = repositoryService.createDeployment()
                            .name(filename)
                            .addInputStream(filename, resource.getInputStream())
                            .deploy();
                    log.info("[BPMN] 部署成功: {} (deploymentId={}, processDefCount={})",
                            filename, deployment.getId(),
                            repositoryService.createProcessDefinitionQuery()
                                    .deploymentId(deployment.getId()).count());
                } catch (Exception e) {
                    log.error("[BPMN] 部署 {} 失败", filename, e);
                }
            }
        } catch (IOException e) {
            log.error("[BPMN] 扫描流程定义文件失败", e);
        }
    }
}
