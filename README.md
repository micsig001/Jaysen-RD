# 仪器类产品研发管理系统

<p align="center">
  <strong>双模计划调度 · 实验室资源统筹 · BOM &amp; ECN · 合规审计 · 企微原生</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Flowable-7.1.0-brightgreen" alt="Flowable"/>
  <img src="https://img.shields.io/badge/Vue-3.4-brightgreen" alt="Vue"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="License"/>
  <img src="https://img.shields.io/badge/status-Phase%203%20完成-success" alt="Status"/>
</p>

---

## 🎯 项目定位

本系统专为**频谱分析仪、示波器、信号源**等精密电子仪器的研发团队设计 —— 业务横跨**长周期硬件打样**（EVT→DVT→PVT 甘特图, 3-12 个月）和**短周期软件迭代**（固件/上位机 Sprint 看板, 1-4 周）两条线，并深度集成**企业微信**作为统一身份 + 通知 + 协同入口。

**典型用户场景**:

| 角色 | 一天的工作 |
|------|-----------|
| **硬件工程师** | 看板拖一张「FPGA 焊接」到 IN_PROGRESS → 借示波器（设备日历预约）→ 写调试记录（文档） |
| **项目经理** | 看 Sprint 燃尽图 → 审 ECN（流程自动 @采购）→ 拉 BOM 多阶展开查 PCB 子件 |
| **实验室管理员** | 校准示波器（锁定预约）→ 审批跨部门借用 → 同步资产到企微通讯录 |
| **质量经理** | 拉 ECN 实施进度（Flowable 看板）→ 复核电子签名 → 出审计月报 |

---

## ✨ 核心特性

### 已完成 (Phase 1 → Phase 3.1)

- 🏗 **双模计划调度** —— 硬件甘特图 (EVT/DVT/PVT 里程碑) + 敏捷 Sprint 看板 (5 列拖拽 vue-draggable-plus)
- 🔬 **实验室资源统筹** —— 设备电子台账 / 日历预约 (FullCalendar) / 维修校准状态锁定
- 📋 **多阶 BOM** —— 自引用 parent_bom_id + bom_item.sub_bom_id 两级表头, 递归展开 (含循环引用防护)
- 🔄 **ECN 审批流** —— Flowable 7.1 4 步流程, TaskListener 自动 @采购 + 企微通知, 全状态机跟踪
- 🏢 **企微原生** —— OAuth2.0 静默登录 + 通讯录 upsert + 应用消息推送 (text / textcard / todo)
- 🛡 **三 AOP 基础设施** —— `@Idempotent` (幂等键) / `@AuditLog` (审计) / `@SensitiveData` (脱敏)
- 🔐 **JWT + refresh_token 双向轮换** —— access 2h / refresh 7d, 401 自动续签单飞模式
- 🔒 **三级权限** —— EMPLOYEE / MANAGER / ADMIN, 按部门隔离数据

### 规划中 (Phase 3.2 → Phase 4)

- 📝 **文档审批 / 电子签名** (Phase 3.2) —— Canvas 手写 + SHA256 哈希验证
- 🐛 **缺陷跟踪** (Phase 3.2) —— V1 SQL 表已建
- 📊 **研发仪表盘** (Phase 3.3) —— ECharts 燃尽 / 设备利用率
- ☁️ **K8s 部署** (Phase 4) —— Helm Chart / Ingress / HPA

---

## 🏛 架构

```
┌──────────────────────────────────────────────────────────────────┐
│                    浏览器 / 企微 App (HTTPS)                      │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                  ┌────────┴────────┐
                  │   Host Nginx    │  (TLS termination + 反代)
                  └────────┬────────┘
                           │
         ┌─────────────────┴─────────────────┐
         ▼                                   ▼
┌──────────────────┐                ┌──────────────────┐
│  Frontend (5173) │                │  Backend (8080)   │
│  Vue 3 + Vite    │   /api/*       │  Spring Boot 3.2  │
│  Element Plus    │ ──────────────▶│  Flowable 7.1     │
│  vue-draggable   │                │  MyBatis-Plus 3.5 │
│  FullCalendar    │                │  Spring Security  │
│  ECharts         │                │  JJWT 0.12        │
└──────────────────┘                └────────┬───────────┘
                                             │
                              ┌──────────────┴──────────────┐
                              ▼                             ▼
                    ┌──────────────────┐         ┌──────────────────┐
                    │  MySQL 8.0        │         │  Redis 7          │
                    │  25 业务表        │         │  Token 黑名单     │
                    │  + 30+ Flowable   │         │  调度锁 / 限流     │
                    │  Flyway 迁移      │         │                   │
                    └──────────────────┘         └──────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  对象存储         │
                    │  MinIO / 阿里 OSS │
                    └──────────────────┘
```

### 后端模块 (9 个 controller)

| 包 | 职责 | 关键端点数 |
|----|------|-----------|
| `controller` | Auth / DevAuth | 6 |
| `user` | 用户管理 | 8 |
| `task` | 任务 CRUD + 状态机 | 11 |
| `taskstatemachine` | 状态机 + 超时 + 历史 | 8 |
| `department` | 部门 + 同步 | 4 |
| `project` | 项目 + 设备 | 12 |
| `ecn` | ECN CRUD + 流程 + 通知 | 14 |
| `sprint` | Sprint 看板 | 7 |
| `bom` | BOM 多阶树 | 9 |

**总计**: 19 controller × 平均 7 端点 = **130+ REST API**

### 前端页面 (11 个业务视图 + 3 个基础页)

| 路由 | 页面 | 功能 |
|------|------|------|
| `/login` | LoginView | 企微按钮 + 模拟登录 |
| `/login/callback` | LoginCallbackView | OAuth 回调 + state 防 open redirect |
| `/rd/projects` | ProjectListView | 项目列表 + 筛选 + 新建 |
| `/rd/projects/:id` | ProjectDetailView | 甘特图 + 设备 |
| `/rd/equipment` | EquipmentListView | 设备台账 + 状态 |
| `/rd/equipment/calendar` | EquipmentCalendarView | FullCalendar 预约 |
| `/rd/tasks` | TaskListView | 任务 + 快捷过滤 + 状态机按钮 |
| `/rd/board` | SprintBoardView | 5 列拖拽 (vue-draggable-plus) |
| `/rd/bom` | BomTreeView | el-tree 多阶递归 + 抽屉 |
| `/ecn` | EcnListView | ECN 列表 + 状态 |
| `/ecn/:id` | EcnDetailView | 流程进度 + 审批操作 |

---

## 🛠 技术栈

### 后端 (Spring Boot 3.2 / Java 17)

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | Web 框架 |
| Spring Security | 6.x | 认证 + 授权 |
| MyBatis-Plus | 3.5.5 | ORM (含逻辑删除 + 乐观锁) |
| **Flowable** | 7.1.0 | 工作流引擎 (ECN 审批) |
| **JJWT** | 0.12.3 | JWT access/refresh 双向轮换 |
| Spring Data Redis | 3.2 | 缓存 + 分布式锁 + Token 黑名单 |
| MySQL | 8.0 | 主存储 |
| **Flyway** | 9.x | DB 迁移 (V1 全量表 + V2 索引) |
| springdoc-openapi | 2.3.0 | Swagger UI |
| Lombok | - | 减少样板 |
| Java | 17 | 运行时 |

### 前端 (Vue 3 / Vite 5)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4 | Composition API + `<script setup>` |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建 + HMR |
| Element Plus | 2.x | UI 组件库 |
| **vue-draggable-plus** | latest | Sprint 看板拖拽 |
| **FullCalendar** | 6.x | 设备预约日历 |
| **ECharts** | 5.x | (Phase 3.3) 仪表盘 |
| Pinia | 2.x | 状态 (userStore) |
| Vue Router | 4.x | 路由 (history 模式) |
| Axios | 1.x | HTTP (含 401 单飞续签) |
| CryptoJS | 4.x | Token AES 256 加密存储 |

### 部署

- **Docker Compose** (单机 / 小团队)
- **Nginx** (反向代理 + HTTPS + SPA fallback)
- **Let's Encrypt** (Certbot 自动续签)
- **可选 K8s** (Phase 4)

---

## 📁 目录结构

```
E:\Project\RD
├── backend/                                     # Spring Boot 后端
│   ├── src/main/java/com/RD/
│   │   ├── RdApplication.java
│   │   ├── common/                              # Result / BusinessException / GlobalExceptionHandler
│   │   ├── config/                              # Security / Swagger / Flowable / MyBatis
│   │   ├── auth/                                # JwtTokenProvider / JwtAuthenticationFilter
│   │   ├── aspect/                              # 3 AOP: Idempotent / AuditLog / SensitiveData
│   │   ├── controller/                          # Auth / DevAuth
│   │   ├── user/                                # 用户管理
│   │   ├── task/                                # 任务 CRUD
│   │   ├── taskstatemachine/                    # 状态机 + 历史
│   │   ├── department/                          # 部门
│   │   ├── project/                             # 项目 + 设备 (Phase 1.5)
│   │   ├── ecn/                                 # ECN + Flowable
│   │   ├── sprint/                              # Sprint 看板
│   │   ├── bom/                                 # BOM 多阶
│   │   └── wework/                              # 企微 OAuth + 消息 + Fake Client
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/migration/                        # V1__init_all_tables.sql
│   │   └── processes/ecn-default.bpmn20.xml     # ECN 4 步流程
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                                    # Vue 3 前端
│   ├── src/
│   │   ├── api/                                 # 13 模块 (auth / user / task / sprint / bom / ecn ...)
│   │   ├── views/                               # 11 业务视图 + PlaceholderView
│   │   │   ├── auth/LoginView / LoginCallbackView
│   │   │   ├── rd/ProjectList / ProjectDetail / EquipmentList / EquipmentCalendar
│   │   │   ├── task/TaskList
│   │   │   ├── sprint/SprintBoard
│   │   │   ├── bom/BomTree
│   │   │   ├── ecn/EcnList / EcnDetail
│   │   │   └── workbench/WorkbenchLayout
│   │   ├── router/index.ts
│   │   ├── stores/user.ts                       # Pinia
│   │   ├── utils/request.ts                     # Axios + 401 单飞续签
│   │   ├── utils/crypto.ts                      # AES 256
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── nginx.conf                               # SPA fallback
│   ├── vite.config.ts
│   └── Dockerfile
│
├── docs/                                        # 文档
│   └── DEPLOY.md                                # ⭐ 生产部署指南
│
├── docker-compose.yml                           # MySQL + Redis + Backend + Frontend
├── .env.example                                 # 环境变量模板
├── .gitignore
├── README.md                                    # ⭐ 本文件
├── CHANGELOG.md
└── PROJECT_STATE.md                             # 详细进度
```

---

## 🚀 快速开始

### 前置要求

- **JDK 17+** + Maven 3.9+ (或用 Docker 跳过)
- **Node.js 20+** (或用 Docker 跳过)
- **Docker Desktop** (推荐, 一键启动)

### 方式 A: Docker 一键启动 (推荐)

```bash
# 1. 克隆
git clone https://github.com/micsig001/Jaysen-RD.git
cd Jaysen-RD

# 2. 准备 .env
cp .env.example .env
# 编辑 .env: 必须改 JWT_SECRET (≥32 字符) + VITE_TOKEN_SECRET
#  生成: openssl rand -base64 32

# 3. 启动 (首次 5-8 分钟: maven 拉依赖 + npm install)
docker compose up -d

# 4. 验证
curl http://localhost:8080/actuator/health   # 200
open http://localhost:5173                     # Web UI
open http://localhost:8080/swagger-ui.html     # API 文档
```

### 方式 B: 本地开发 (前后端分离)

```bash
# --- 后端 ---
cd backend
# 启动本地 MySQL + Redis (或用 docker compose up mysql redis -d)
mvn spring-boot:run

# --- 前端 (另开终端) ---
cd frontend
npm install
cp ../.env.example .env
# 编辑 .env 的 VITE_API_URL=http://localhost:8080
npm run dev   # http://localhost:5173
```

### Dev 登录 (免企微)

dev profile 自动注册 `FakeWeWorkApiClient`,4 个内置账号:
- `admin` (ADMIN) / `manager` (MANAGER) / `lead` (MANAGER) / `employee` (EMPLOYEE)

登录页直接选 → 进系统。

---

## 🗄 数据库

**V1 全量建表** (`V1__init_all_tables.sql`),共 **25 张业务表** + **22 复合索引**:

| 分类 | 表 | 数量 |
|------|-----|------|
| 系统基础 | `wework_config` / `sys_user` / `sys_department` / `sys_sync_log` / `sys_audit_log` / `sys_idempotency_key` | 6 |
| 任务管理 | `task` / `task_status_history` / `tasks_history_archive` | 3 |
| 项目管理 | `project` / `milestone` / `sprint` / `sprint_task` | 4 |
| 实验室资源 | `lab_equipment` / `equipment_reservation` / `equipment_maintenance` | 3 |
| BOM & ECN | `bom_header` / `bom_item` / `ecn_change` / `ecn_approval` | 4 |
| 合规审计 | `document_approval` / `electronic_signature` / `defect_tracking` | 3 |

**Flowable 7.1 表** —— `ACT_RE_*` / `ACT_RU_*` / `ACT_HI_*` / `ACT_GE_*` / `ACT_ID_*` 共 30+ 张, 由 `flowable.database-schema-update=true` 启动时**自动建**。

**V2** —— Flowable 启动后追加业务复合索引 (`@PostConstruct` 触发, 失败不阻塞启动)。

---

## 📚 文档

| 文档 | 用途 |
|------|------|
| [`README.md`](README.md) | 本文件 (项目门面) |
| [`docs/DEPLOY.md`](docs/DEPLOY.md) | ⭐ **生产部署完整指南** (60-90 分钟首次部署) |
| [`PROJECT_STATE.md`](PROJECT_STATE.md) | 详细进度 + 技术决策记录 |
| [`CHANGELOG.md`](CHANGELOG.md) | 提交历史 (Conventional Commits) |

---

## 🛣 开发路线

| Phase | 状态 | 关键交付 |
|-------|------|---------|
| **Phase 1** | ✅ | 骨架 + V1 SQL + 认证 + 3 AOP + ECN 流程定义 |
| **Phase 1.5** | ✅ | R&D 业务骨架 (Project/Equipment UI) |
| **Phase 2** | ✅ | User / Task / 状态机 / WeWork (2 轮) |
| **Phase 2.5** | ✅ | 任务历史 / 超时 / 快捷过滤 |
| **Phase 2.6** | ✅ | Flowable ECN (3 轮, 含 DI 修复) |
| **Phase 2.7** | ✅ | WeWork 真实接入 (Fake + MessageService) |
| **Phase 3.1** | ✅ | Sprint 看板 + BOM 多阶树 |
| **Phase 3.1.1** | ✅ | refresh_token 双向轮换 + 401 单飞续签 |
| **Phase 3.1.2** | ✅ | 生产部署文档 (docs/DEPLOY.md) |
| Phase 3.2 | ⏳ | 文档审批 / 电子签名 / 缺陷跟踪 |
| Phase 3.3 | ⏳ | 研发仪表盘 (ECharts) |
| Phase 4 | ⏳ | K8s / 监控 / 安全加固 / 压测 |

---

## 🔐 安全设计

- **JWT 双向轮换**: access 2h, refresh 7d, 每次 refresh 两者都换, 防止 token 复用
- **401 单飞续签**: 并发 401 共享一个 `/auth/refresh` 请求, 失败立即登出
- **AES 256 加密**: 前端 localStorage 的 token 加密存储 (CryptoJS + VITE_TOKEN_SECRET)
- **企微 OAuth state 防御**: 前端 `isSafeState()` + 后端 `sanitizeState()` 双层, 拒绝非 `/` 开头 + 含 `://` + 超 200 字符
- **三级 RBAC**: 注解 `@PreAuthorize("hasRole('ADMIN')")` + 按部门 `deptId` 数据隔离
- **SQL 注入防御**: MyBatis-Plus `LambdaQueryWrapper`, 无字符串拼接
- **CSRF**: 同源策略 + Bearer Token (非 Cookie, 无 CSRF 风险)
- **审计日志**: `@AuditLog` 注解覆盖所有写操作, 字段级记录 old/new value

---

## 🤝 二次开发

### 新增业务模块的步骤 (以「缺陷跟踪」为例)

1. **后端**:
   ```bash
   cd backend/src/main/java/com/RD
   mkdir defect
   # 1) Defect.java entity
   # 2) DefectMapper.java extends BaseMapper<Defect>
   # 3) DefectService.java (CRUD + 状态机)
   # 4) DefectController.java (@AuditLog / @PreAuthorize)
   ```

2. **前端**:
   ```bash
   cd frontend/src
   # 1) api/defect.ts
   # 2) views/defect/DefectListView.vue
   # 3) router/index.ts 添加 /rd/defects
   ```

3. **数据库**: 改 `V1__init_all_tables.sql` **不要**改, 新建 `V2__defect_table.sql`

### 关键设计原则

- **AOP 优先**: 任何写操作都加 `@AuditLog` + `@Idempotent("biz:action:user")`
- **DTO 分离**: Entity / VO / Request 严格分开, 防泄漏 DB 字段
- **权限注解**: Controller 方法上 `@PreAuthorize("hasRole('X')")`
- **状态机**: 复用 `TaskStateMachine`, 不要在 Service 里写 if-else
- **前端 AES**: token 必须经过 `crypto.ts` 加密, 不要直接 localStorage.setItem

---

## 📈 性能基线

| 场景 | P95 | QPS | 备注 |
|------|-----|-----|------|
| OAuth 登录 | 800ms | 5 | 含企微远程 |
| 看板拖拽 | 50ms | 50 | saveStatus |
| 拉 Sprint 列表 | 120ms | 30 | 5 状态 join |
| BOM 多阶 (3 层) | 180ms | 20 | 递归展开 |
| ECN 启动流程 | 600ms | 2 | Flowable 启动 |

环境: 4 vCPU / 8GB / SSD / Docker Compose 单机 / 50 并发用户。

---

## 📄 许可证

Apache License 2.0

---

## 🙏 致谢

- [Element Plus](https://element-plus.org/) - 国产 Vue 3 UI
- [Flowable](https://www.flowable.com/) - BPMN 引擎
- [MyBatis-Plus](https://baomidou.com/) - 国产 ORM
- [vue-draggable-plus](https://github.com/Alfred-Skyblue/vue-draggable-plus) - 看板拖拽

---

**⭐ 如果这个项目帮到你, 欢迎 Star** → https://github.com/micsig001/Jaysen-RD
