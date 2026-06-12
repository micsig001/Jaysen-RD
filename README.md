# 仪器类产品研发管理系统

<p align="center">
  <strong>双模计划调度 · 实验室资源统筹 · BOM &amp; ECN · 合规审计</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Flowable-7.1.0-brightgreen" alt="Flowable"/>
  <img src="https://img.shields.io/badge/Vue-3.4-brightgreen" alt="Vue"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License"/>
</p>

---

## 项目简介

本系统专为频谱分析仪等精密仪器的研发团队设计，深度集成**企业微信**工作台。覆盖硬件打样的长周期（EVT/DVT/PVT 甘特图）和软件迭代的敏捷性（固件/上位机冲刺看板），同时管理实验室稀缺资源、多阶 BOM 与工程变更（ECN）审批流、合规审计与电子签名。

> 🎯 **本项目起步中**：Phase 1 骨架已完成，V1 全量 SQL 已建表（25 张表 + Flowable 占位）。业务模块将在 Phase 2-4 逐步实现。

## ✨ 核心特性（规划）

- 🏗 **双模计划调度** —— 硬件甘特图（EVT/DVT/PVT 里程碑卡点）+ 敏捷看板（冲刺管理）
- 🔬 **实验室资源统筹** —— 设备电子台账、日历预约、维修/校准状态锁定
- 📋 **多阶 BOM & ECN** —— 版本追溯 + Flowable 7 跨部门审批流
- ✍️ **合规审计** —— 文档审批电子签名（Canvas + SHA256 哈希验证）
- 🏢 **企业微信深度集成** —— OAuth2.0 静默登录 + 通讯录增量同步 + 应用消息推送
- 🛡 **三 AOP 基础设施** —— `@Idempotent` / `@AuditLog` / `@SensitiveData`
- 🔒 **三级权限** —— EMPLOYEE / MANAGER / ADMIN 自动按部门隔离

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | Web 框架 |
| MyBatis-Plus | 3.5.5 | ORM |
| Flowable | 7.1.0 | 工作流引擎（ECN 审批流） |
| Spring Security | 6.x | 认证授权 + JWT |
| JJWT | 0.12.3 | JWT 签发/校验 |
| Spring Data Redis | - | 缓存 + 分布式锁 |
| MySQL | 8.0 | 主存储 |
| Flyway | - | 数据库迁移 |
| Lombok | - | 减少样板 |
| springdoc-openapi | 2.3.0 | Swagger UI |
| Java | 17 | 运行时 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | 主框架（Composition API） |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建 |
| Element Plus | 2.x | UI |
| Pinia | - | 状态 |
| Vue Router | 4.x | 路由 |
| ECharts | 5.x | 甘特图/图表 |
| Axios | - | HTTP |
| CryptoJS | - | Token 加密 |

### 部署

Docker Compose 编排 MySQL + Redis + Backend + Frontend。

---

## 📁 目录结构

```
E:\Project\RD
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/RD/
│   │   ├── RdApplication.java        # 启动类
│   │   ├── common/                   # Result / BusinessException / GlobalExceptionHandler
│   │   └── config/                   # Security / Swagger / Flowable 配置（待写）
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/V1__init_all_tables.sql
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 封装（待写）
│   │   ├── views/HelloView.vue       # 骨架首页
│   │   ├── router/index.ts
│   │   ├── stores/                   # Pinia stores（待写）
│   │   ├── utils/                    # request / crypto（待写）
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
├── docs/                             # 文档（待补充）
├── k8s/                              # K8s 部署清单（待写）
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
├── CHANGELOG.md
└── PROJECT_STATE.md
```

---

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker Desktop（推荐，可一键启动 MySQL + Redis）

### 方式 A：Docker 一键启动

```bash
# 1. 复制环境变量
cp .env.example .env
# 编辑 .env：必须修改 JWT_SECRET（≥32 字符）

# 2. 启动所有服务
docker-compose up -d

# 3. 验证
# 浏览器：http://localhost:5173
# Swagger UI：http://localhost:8080/swagger-ui.html
# MySQL：mysql -h 127.0.0.1 -u root -pDevPass2026rd rd_system
```

### 方式 B：本地开发

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端（另开终端）
cd frontend
npm install
cp ../.env.example .env
# 修改 .env 的 VITE_API_URL 和 VITE_TOKEN_SECRET
npm run dev
```

---

## 🗄 数据库

V1 全量建表（`V1__init_all_tables.sql`），共 **25 张业务表** + **22 个复合索引**：

| 分类 | 表 | 数量 |
|------|-----|------|
| 系统基础 | `wework_config` / `sys_user` / `sys_department` / `sys_sync_log` / `sys_audit_log` / `sys_idempotency_key` | 6 |
| 任务管理（合并自 Task 项目） | `task` / `task_status_history` / `tasks_history_archive` | 3 |
| 项目管理 | `project` / `milestone` / `sprint` / `sprint_task` | 4 |
| 实验室资源 | `lab_equipment` / `equipment_reservation` / `equipment_maintenance` | 3 |
| BOM & ECN | `bom_header` / `bom_item` / `ecn_change` / `ecn_approval` | 4 |
| 合规审计 | `document_approval` / `electronic_signature` / `defect_tracking` | 3 |

**Flowable 7.1.0 表**：
- ACT_RE_* / ACT_RU_* / ACT_HI_* / ACT_GE_* / ACT_ID_* 由 `application.yml` 的 `flowable.database-schema-update=true` 在 Spring 启动时**自动创建**（保证与 Flowable 7.1.0 严格匹配）
- 业务级 ACT_* 索引放到 `FlowableConfig` 的 `@PostConstruct`（Phase 2 实现）

**V2 Flyway**（`V2__flowable_indexes_placeholder.sql`）—— 当前为占位空文件，保证 Flyway 版本连续。Phase 2 引入 FlowableConfig 后可删除。

**说明**：
- `task` 表保留 Task 项目原名，spec 里的敏捷看板 task 改名为 `sprint_task`，避免重名

---

## 🗺 开发路线

| Phase | 目标 | 状态 |
|-------|------|------|
| **Phase 1** | 骨架 + V1 SQL + 认证 + 基础三 AOP + ECN 流程定义 | 🚧 进行中 |
| Phase 2 | 项目管理（甘特图）/ 设备预约 / 看板 / BOM | ⏳ 待启动 |
| Phase 3 | 文档审批 / 电子签名 / 缺陷跟踪 / 流程归档 | ⏳ |
| Phase 4 | K8s / 监控 / 安全加固 / 压测 | ⏳ |

详见 [`PROJECT_STATE.md`](PROJECT_STATE.md)。

---

## 🤝 贡献

Conventional Commits 规范（feat / fix / docs / refactor / test / chore）。

## 📄 许可证

Apache License 2.0
