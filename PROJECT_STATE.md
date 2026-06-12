# PROJECT_STATE — 项目状态快照

> 用途：接手者 / 后续会话快速理解"项目到了哪一步、还差什么、踩过哪些坑"。
> 最后更新：2026-06-12 (Phase 1.5 收尾)

---

## 1. 基本信息

- **项目**：仪器类产品研发管理系统（Instrument R&D Management System）
- **本地路径**：`E:\Project\RD`
- **来源**：从 `E:\Project\Task`（企业任务协同管理系统）升级合并而来
- **技术栈**：
  - 后端：Spring Boot 3.2.0 + MyBatis-Plus 3.5.5 + **Flowable 7.1.0** + Spring Security 6 + JWT + Redis 7 + MySQL 8.0
  - 前端：Vue 3.4 + TypeScript 5 + Vite 5 + Element Plus 2 + ECharts 5 + Pinia 2
  - 部署：Docker Compose（mysql:8.0 + redis:7-alpine + backend + frontend）
  - Java 17，Maven 3.9，Node 20+

---

## 2. 已完成

### Phase 1.5 (R&D 业务模块) ✅
- [x] **后端 R&D 业务包** —— `com.RD.rd.common.RdConstants`（枚举常量）+ `com.RD.rd.project.*`（10 文件: Project / Milestone / ProjectVO / MilestoneVO / CreateProjectRequest / CreateMilestoneRequest / ProjectMapper / MilestoneMapper / ProjectService / ProjectController）+ `com.RD.rd.equipment.*`（10 文件: LabEquipment / EquipmentReservation / LabEquipmentVO / EquipmentReservationVO / CreateLabEquipmentRequest / CreateReservationRequest / LabEquipmentMapper / EquipmentReservationMapper / LabEquipmentService / LabEquipmentController）
- [x] **4 个 V1 SQL 表** —— `project` / `milestone` / `lab_equipment` / `equipment_reservation` + 22 个复合索引
- [x] **前端 4 个 R&D 业务视图** —— `ProjectListView` (302L) / `ProjectDetailView` (431L) / `EquipmentListView` (272L) / `EquipmentCalendarView` (472L)
- [x] **前端 2 个 API 模块 + 1 个 constants** —— `api/project.ts` (8 endpoints) + `api/equipment.ts` (8 endpoints) + `api/constants.ts` (5 个枚举常量集中化)
- [x] **路由指向真实组件** —— `rd/projects` / `rd/projects/:id` / `rd/equipment` / `rd/equipment/calendar` 指向 4 个真实视图
- [x] **构建验证** —— `npm run build` 0 错,7.46s,4 个 R&D chunk 正常 code-split
- [x] **静态检查** —— `com.task` 残留仅在 PROJECT_STATE.md 语义出现，代码层 0 残留

### Phase 1 骨架 ✅
- [x] Spring Boot 工程骨架（根包 `com.RD`）
- [x] V1 全量建表 SQL（25 张表 + 22 个复合索引）— sys_user / sys_department / sys_sync_log 字段对齐 Task 旧表
- [x] V2 Flyway 占位（Flowable ACT_* 表交给 `flowable.database-schema-update=true` 自动建）
- [x] FlowableConfig 占位（启动期日志，Phase 2 接业务时扩展）
- [x] 统一响应 `Result<T>` / 业务异常 `BusinessException` / 全局异常处理（吸收 Task 的 BindException / AuthenticationException / Throwable 兜底）
- [x] `application.yml` 全局配置（Flowable / WeWork / JWT / Storage / Archive / Sensitive）
- [x] Flyway 启用（`baseline-on-migrate: true`）
- [x] Docker Compose 4 服务编排
- [x] 前端 Vue 3 骨架（HelloView 欢迎页 + WorkbenchLayout 双分组导航 + 12 个子路由 + Pinia + 路由占位 PlaceholderView）
- [x] 顶栏：当前页标题、角色标签、用户下拉（个人中心 + 退出登录）
- [x] 移动端：侧边栏抽屉、响应式断点 768px

### 认证 + 安全（Phase 1 关键交付）✅
- [x] `com.RD.config.SecurityConfig` —— Spring Security 6 配置（JAF 注入 + CORS + 公开端点）
- [x] `com.RD.controller.AuthController` —— 5 个端点（token / callback / refresh / logout + dev-login 移到独立 Controller）
- [x] `com.RD.controller.DevAuthController` —— 类级 `@Profile("dev")`，prod 完全不加载
- [x] 前端 `utils/crypto.ts` —— AES-256 加解密
- [x] 前端 `utils/request.ts` 升级 —— 真实 Token 加密 + 401 自动跳登录
- [x] 前端 `router/index.ts` 升级 —— 完整路由守卫（Token + ADMIN + pcOnly）

### 后端测试基础设施 ✅
- [x] **`pom.xml`** 加 H2 test-scope 依赖
- [x] **`application-test.yml`** —— H2 + MySQL 兼容 + Flyway off + 排除 Redis/Flowable
- [x] **`schema-h2.sql`** —— 8 张核心表的 H2 兼容版本
- [x] **`AbstractIntegrationTest`** —— 集成测试基类（H2 + Mock Redis）
- [x] **JwtSecretValidatorTest**（7 case）
- [x] **JwtTokenProviderTest**（5 case）—— 直接 `new` 不依赖 Spring Context
- [x] **AuthControllerMockMvcTest**（8 case）—— 4 个生产端点
- [x] **DesensitizationUtilTest**（6 case）
- [x] **SensitiveDataAspectTest**（6 case）—— 含本人豁免
- [x] **IdempotencyServiceTest**（7 case）—— extractKey + tryReserve
- **总计**:9 个测试文件 / ~40 case / 1 个基类

### 后端公共包（从 Task 项目搬入，包名 `com.task.*` → `com.RD.*`）✅
- [x] `com.RD.auth` —— JwtTokenProvider / JwtAuthenticationFilter / JwtSecretValidator（3 文件）
- [x] `com.RD.audit` —— AuditLog 注解 / Aspect / Context / Service（4 文件）
- [x] `com.RD.privacy` —— SensitiveData / SensitiveType / Properties / DesensitizationUtil / Aspect（5 文件）
- [x] `com.RD.idempotency` —— Idempotent / Aspect / Service / CleanupScheduler（4 文件）
- [x] `com.RD.wework` —— WeWorkApiClient / WeWorkApiConfig / WeWorkAuthService / SyncScheduler（4 文件，Phase 2 补 WeWorkSyncService / WeWorkMessageService / FakeWeWorkApiClient）

### 后端 Entity + Mapper（V1 SQL 配套）✅
- [x] `com.RD.entity` —— SysUser / SysDepartment / SysSyncLog / SysAuditLog / SysIdempotencyKey（5 文件）
- [x] `com.RD.mapper` —— SysUserMapper / SysDepartmentMapper / SysSyncLogMapper / SysAuditLogMapper / SysIdempotencyKeyMapper（5 文件）

### 关键调整
- **V1 SQL 字段调整**：`sys_user` 字段名（`user_id` / `name` / `department_id` / `is_manual_role` / `last_sync_time`）与 Task 项目的 `users` 表对齐，方便 WeWorkAuthService 等包搬入时**零 setter/getter 修改**
- **dev-login 拆到独立 Controller**：方法级 `@Profile("dev")` 在 `@RestController` 上不可靠（Spring 5+ 仍不保证），改为类级 `@Profile("dev")` 的 `DevAuthController`，prod 环境完全不加载

### 数据库（V1 已建表，**全部空表**）✅
- [x] 系统基础：6 张（`wework_config` / `sys_user` / `sys_department` / `sys_sync_log` / `sys_audit_log` / `sys_idempotency_key`）
- [x] 任务管理：3 张（`task` / `task_status_history` / `tasks_history_archive`）
- [x] 项目管理：4 张（`project` / `milestone` / `sprint` / `sprint_task`）
- [x] 实验室资源：3 张（`lab_equipment` / `equipment_reservation` / `equipment_maintenance`）
- [x] BOM & ECN：4 张（`bom_header` / `bom_item` / `ecn_change` / `ecn_approval`）
- [x] 合规审计：3 张（`document_approval` / `electronic_signature` / `defect_tracking`）
- [x] **V1 末尾追加 22 个业务复合索引**（项目/设备/BOM/ECN/合规 5 类）
- [x] **Flowable 7.1.0 ACT_* 表**：让 `flowable.database-schema-update=true` 自动建（避免版本错位）

---

## 3. 待完成（Phase 2-4，按优先级）

### Phase 2：业务深化 + Flowable 接入（预计 1-2 周）
- [x] ~~**后端业务包**：`com.RD.project` / `equipment`~~ ✅ Phase 1.5 已完成
- [x] ~~**前端 API 封装**：`api/{project,equipment}.ts`~~ ✅ Phase 1.5 已完成（还差 auth/user/task/wework）
- [ ] **FlowableConfig** 配置类（用户身份映射、监听器注册）
- [ ] **第一个 BPMN 流程定义** `resources/processes/ecn-default.bpmn20.xml`
- [ ] **后端业务包**：`com.RD.tasklegacy`（Task 业务代码搬入，包名 `com.task.*` → `com.RD.tasklegacy.*`）
- [ ] **后端公共包补完**：`com.RD.storage` / `com.RD.common`（已部分）
- [ ] **WorkbenchLayout** 导航分组：左侧加"任务" + "研发"两个分组
- [ ] **前端 API 补完**：`api/{auth,user,task,wework}.ts`
- [ ] **JWT 认证 + 企微 OAuth 回调**：`api/auth/wework/callback`

### Phase 3：业务功能（预计 2-3 周）
- [ ] 看板（vue-draggable-plus）+ Sprint 管理
- [ ] 甘特图（ECharts 里程碑）
- [ ] 设备预约日历（el-calendar + 冲突检测）
- [ ] BOM 多阶树（el-tree）
- [ ] ECN 审批流（Flowable Task + 部门负责人动态分配 + 企微通知）
- [ ] 电子签名（signature_pad + SHA256 验证）

### Phase 4：合规 + 高级（预计 2 周）
- [ ] 缺陷跟踪（根因 + 帕累托图）
- [ ] 仪表盘自定义
- [ ] K8s 部署清单
- [ ] Spring Boot Actuator + Prometheus + Grafana
- [ ] 限流（Sentinel）

---

## 4. 关键决策记录

| 决策 | 决定 | 原因 |
|------|------|------|
| 项目路径 | `E:\Project\RD` | 与 Task 项目并列，目录清晰 |
| 根包名 | `com.RD` | 与项目名 Instrument R&D 对应 |
| Flowable | Phase 1 引入 7.1.0 | ECN 审批流需要可配置 |
| Task 表保留 `task` | 不改名 | spec 看板改 `sprint_task` 避重名 |
| 模块关系 | WorkbenchLayout 加分组 | 任务/研发并列，用户自由切换 |
| Phase 1 范围 | 只搭骨架 + V1 SQL | 风险最小，后续 Phase 渐进 |

---

## 5. 已知风险

1. **环境缺 jdk/mvn/docker** —— 本机只有 Node，无法 `mvn compile` 验证
   - **应对**：所有 Java 代码静态编写，import 严格对照 Spring Boot 3 + MyBatis-Plus 3.5.5 + Flowable 7.1.0 API
   - **强烈建议**：部署环境第一件事跑 `mvn clean compile -DskipTests`

2. **Flowable 7 与 Spring Boot 3 兼容性** —— 7.1.0 官方声明支持，但社区反馈偶有 JPA 冲突
   - **应对**：用 MyBatis-Plus 而非 JPA，且禁用 `flowable.rest.app/admin`

3. **数据库外键与中文 ENUM** —— `ecn_change.change_type` 等用 ENUM 类型，扩展性差
   - **应对**：Phase 2 评估是否改为 VARCHAR + CHECK 约束

4. **V1 SQL 一次建 25 张表** —— 启动时间长，DDL 错误难定位
   - **应对**：Flyway baseline-on-migrate=true 支持从空库自动初始化

---

## 6. 重要文件索引

| 文件 | 说明 |
|------|------|
| `backend/pom.xml` | 依赖 + Java 17 + Maven 3.9 + Flowable 7.1.0 |
| `backend/src/main/resources/application.yml` | 全局配置 |
| `backend/src/main/resources/db/migration/V1__init_all_tables.sql` | 25 张表 + 22 个复合索引 |
| `backend/src/main/resources/db/migration/V2__flowable_indexes_placeholder.sql` | 占位空文件，Phase 2 可删 |
| `backend/src/main/java/com/RD/config/FlowableConfig.java` | Flowable 启动期日志 + Phase 2 占位 Bean |
| `backend/src/main/java/com/RD/RdApplication.java` | 启动类（`@EnableAsync` + `@EnableScheduling`） |
| `backend/src/main/java/com/RD/common/` | Result / BusinessException / GlobalExceptionHandler |
| `docker-compose.yml` | 4 服务编排 |
| `.env.example` | 环境变量模板（必填项：JWT_SECRET / VITE_TOKEN_SECRET / REDIS_PASSWORD） |
| `frontend/src/views/HelloView.vue` | Phase 1 首页（确认骨架跑通） |
| `frontend/src/views/rd/ProjectListView.vue` | Phase 1.5 项目列表 |
| `frontend/src/views/rd/ProjectDetailView.vue` | Phase 1.5 项目详情 |
| `frontend/src/views/rd/EquipmentListView.vue` | Phase 1.5 设备列表 |
| `frontend/src/views/rd/EquipmentCalendarView.vue` | Phase 1.5 设备预约日历 |
| `frontend/src/api/{project,equipment,constants}.ts` | Phase 1.5 R&D 业务 API + 常量 |
| `backend/src/main/java/com/RD/rd/{project,equipment,common}/*` | Phase 1.5 R&D 业务后端 |

---

## 7. 启动命令

```bash
# 方式 A：Docker（推荐）
cp .env.example .env
# 编辑 .env：必须设置 JWT_SECRET（≥32 字符）
docker-compose up -d

# 方式 B：本地开发
# 终端 1
cd backend
mvn spring-boot:run

# 终端 2
cd frontend
npm install
cp ../.env.example .env
npm run dev
```

---

## 8. 下一步优先级建议（Phase 2 kickoff 待用户确认）

| 优先级 | 工作 | 原因 |
|-------|------|------|
| **P0** | 跑一次 `mvn clean compile` 验证 R&D 业务后端编译 | Phase 1.5 新增 20 个 R&D Java 文件需验证 |
| **P0** | 跑一次 `docker-compose up -d` 端到端 | 验证 Flyway V1 + 4 个 R&D 表 + Flowable 表创建 |
| **P1** | Flowable Config + 第一个 ECN 流程定义 | ECN 审批流是核心差异化功能 |
| **P1** | WorkbenchLayout 加"任务"+"研发"分组 | 让 P1.5 业务视图可触达 |
| **P1** | `api/{auth,user,task,wework}.ts` 补完 | 配齐前端 API 拼图 |
| **P2** | JWT 认证 + 企微 OAuth 回调接通 | 没有认证什么都做不了 |
| **P2** | 看板（vue-draggable-plus）+ Sprint 管理 | Phase 3 起步 |
