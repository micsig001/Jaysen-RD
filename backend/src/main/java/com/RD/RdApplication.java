package com.RD;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 仪器类产品研发管理系统 - 启动类
 *
 * <p>本系统是面向频谱分析仪等精密仪器研发团队的协同管理平台，
 * 涵盖双模计划调度（甘特图 + 敏捷看板）、实验室资源统筹、
 * 多阶 BOM 与 ECN 工程变更、合规审计与电子签名等核心能力。</p>
 *
 * <p>架构要点：</p>
 * <ul>
 *   <li>前后端分离：Vue 3 SPA + Spring Boot 3 REST API</li>
 *   <li>企业微信深度集成：OAuth2.0 静默登录 + 通讯录增量同步 + 应用消息推送</li>
 *   <li>Flowable 7 工作流引擎：ECN 审批流可配置（BPMN 2.0）</li>
 *   <li>三 AOP 基础设施：{@code @Idempotent} / {@code @AuditLog} / {@code @SensitiveData}</li>
 * </ul>
 *
 * <p>根包：{@code com.RD}，业务子包：{@code com.RD.{auth,project,equipment,bom,ecn,workflow,defect,document,wework,...}}</p>
 *
 * @author Mavis
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RdApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdApplication.class, args);
    }
}
