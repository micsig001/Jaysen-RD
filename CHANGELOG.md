# 变更日志 (CHANGELOG)

> 记录项目所有重要变更

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## [Unreleased] — 2026-06-12 (Phase 1.5 Final - R&D 业务模块前端落地)

### Added（新增）

#### 前端 R&D 业务视图（4 个真实页面 + 2 API 模块 + 1 constants）
- **`views/rd/ProjectListView.vue`**（302 行）—— 项目列表（CRUD + 状态/优先级/搜索/分页）
- **`views/rd/ProjectDetailView.vue`**（431 行）—— 项目详情（基本信息 + 里程碑时间轴 + 任务清单）
- **`views/rd/EquipmentListView.vue`**（272 行）—— 设备列表（CRUD + 状态/类型/搜索/分页）
- **`views/rd/EquipmentCalendarView.vue`**（472 行）—— 设备预约日历（el-calendar + 冲突检测 + 预约表单）
- **`api/project.ts`** —— 8 个端点（项目 + 里程碑 CRUD）
- **`api/equipment.ts`** —— 8 个端点（设备 + 预约 CRUD）
- **`api/constants.ts`** —— 5 个业务枚举常量（项目状态/优先级、里程碑状态、设备状态/类型）—— 业务模块共享 + 后续 Web/小程序端可复用
- **`router/index.ts`** 路由指向 4 个真实 R&D 视图（`rd/projects` / `rd/projects/:id` / `rd/equipment` / `rd/equipment/calendar`）

#### 前端工程化
- **`api/constants.ts` 集中化业务枚举** —— 业务枚举常量从各 `api/*.ts` 抽到独立 `api/constants.ts`，`project.ts` / `equipment.ts` 通过 re-export 暴露，避免循环依赖 + 后续多端共享
- **`ProjectDetailView.vue`** 修复 TS2322 —— `el-timeline-item` 的 `type` 字段显式 `as 'primary' | 'success' | 'warning' | 'danger' | 'info'` 联合 + `?? 'info'` fallback，解决 `Record<string, X>[key]` 在 TypeScript 里宽化为 `string` 的问题

### 关键决策
- **`api/constants.ts` 集中化** —— 业务枚举与 API 请求分离：常量放 `constants.ts`，API 端点放 `project.ts` / `equipment.ts`，通过 re-export 保持调用方 import 路径不变
- **显式字面量联合 + fallback** —— 处理 `Record<string, X>` 在 Element Plus prop 校验时的类型宽化问题
- **`rd/milestones` 路由仍指向 PlaceholderView** —— P3 甘特图暂不做，预留位
- **vue-tsc build 副产品**：每次 `npm run build` 会在 `src/**/*.js` 生成 `<file>.vue.js` / `.vue.js.map` 旁车，build 后用 `mavis-trash` 清理（`.gitignore` 已配）

### 构建验证
- **`npm run build`** 0 错,7.46s —— dist/ 4 个 R&D chunk 正常 code-split（`ProjectListView` / `ProjectDetailView` / `EquipmentListView` / `EquipmentCalendarView`），各 8-10KB gzipped
- **静态检查** —— `com.task` 残留仅在 PROJECT_STATE.md 语义出现，代码层 0 残留

### 统计
- 新增前端文件:7 个（4 视图 + 2 API + 1 constants）
- 修改前端文件:3 个（`api/project.ts` / `api/equipment.ts` / `views/rd/ProjectDetailView.vue`）
- 修改前端文件(路由):1 个（`router/index.ts`）

### 配合的后端 Phase 1.5
- **10 个 R&D 业务 Java 文件**: `com.RD.rd.common.RdConstants` + `com.RD.rd.project.*`（10 个）+ `com.RD.rd.equipment.*`（10 个）
- **4 个 V1 SQL 表**: `project` / `milestone` / `lab_equipment` / `equipment_reservation`
- **22 个复合索引**: 项目/设备/BOM/ECN/合规 5 类

---

## [Unreleased] — 2026-06-12 (Phase 1 骨架续 3 - 测试基础设施)

### Added（新增）

#### 后端测试
- **`pom.xml`** 加 `com.h2database:h2` test-scope 依赖
- **`application-test.yml`** —— 测试配置（H2 + MySQL 兼容模式 + Flyway off + 排除 Redis/Flowable）
- **`schema-h2.sql`** —— H2 兼容的 8 张核心表（wework_config / sys_user / sys_department / sys_sync_log / sys_audit_log / sys_idempotency_key / task / task_status_history / tasks_history_archive）
- **`AbstractIntegrationTest`** —— 集成测试基类（`@SpringBootTest` + `@ActiveProfiles("test")` + `@MockBean StringRedisTemplate`）
- **`JwtSecretValidatorTest`**（7 case）—— 从 Task 搬入（包名 com.RD.auth）
- **`JwtTokenProviderTest`**（5 case）—— 新增（token 生成/解析/篡改/空/不同 secret 验签）
- **`AuthControllerMockMvcTest`**（8 case）—— 4 个生产端点（token / wework-callback / refresh / logout）
- **`DesensitizationUtilTest`**（6 case）—— 从 Task 搬入
- **`SensitiveDataAspectTest`**（6 case）—— 从 Task 搬入 + 加 1 个本人豁免测试
- **`IdempotencyServiceTest`**（7 case）—— 新增（extractKey 校验 + tryReserve 路径）

### 关键决策
- **测试 schema 命名沿用生产表名**（sys_user / sys_department 等），H2 兼容模式支持 ENUM 用 VARCHAR + CHECK 替代
- **Redis 在测试中 Mock 排除**：RedisAutoConfiguration 在 application-test.yml exclude + @MockBean 占位 StringRedisTemplate
- **JwtTokenProvider 单元测试不依赖 Spring Context**：直接 `new JwtTokenProvider(secret, expiration, refreshExpiration)`，跑得快

### 统计
- 单元 + MockMvc 测试文件:8 个
- 集成测试基类:1 个
- 测试用例总计:约 40+ case

---

## [Unreleased] — 2026-06-12 (Phase 1 骨架续 2)

### Added（新增）

#### 后端认证 + 安全
- **SecurityConfig** —— Spring Security 6 配置
  - 禁用 CSRF（CORS 替代）
  - 无状态 Session（JWT 模式）
  - 公开端点：`/api/auth/**` + `/api/auth/wework/callback` (GET) + Swagger + Actuator
  - JwtAuthenticationFilter 注入到 UsernamePasswordAuthenticationFilter 之前
  - CORS 按环境配置（dev 默认 localhost，prod 通过 `ALLOWED_ORIGINS` 配）
  - 启用 `@EnableMethodSecurity` 支持 `@PreAuthorize`
- **AuthController** —— 5 个端点
  - `POST /api/auth/token` —— 用企微 code 换 JWT
  - `GET  /api/auth/wework/callback` —— 企微 OAuth 回调（GET 方式，重定向回前端）
  - `POST /api/auth/refresh` —— 刷新 Token
  - `POST /api/auth/logout` —— 退出登录（Token 进 Redis 黑名单）
- **DevAuthController** —— 【dev only】本地绕过 OAuth
  - 类级 `@Profile("dev")`，prod 环境完全不加载
  - 端点：`POST /api/auth/dev-login`，支持任意 username + role 直接签发 JWT

#### 前端认证
- **utils/crypto.ts** —— AES 加解密（CryptoJS），Token 本地加密存储
- **utils/request.ts** 升级 —— 真实实现
  - `getToken` / `setToken` / `setRefreshToken` / `removeToken`（基于 crypto.ts 加密 localStorage）
  - 请求拦截器自动附 `Authorization: Bearer <token>`
  - 响应拦截器：业务 401 跳登录、HTTP 状态码 401 跳登录、403/404/500 警告
- **router/index.ts** 升级 —— 完整守卫
  - 公开页面（isPublic）直接放行
  - 未登录访问受保护页面 → `/login?redirect=...`
  - 已登录访问 `/login` → `/`
  - `requiresAdmin` 检查（不是 ADMIN → `/403`）
  - `pcOnly` 检查（<768px 移动端 → 跳走）
  - 新增 `/login/callback`（企微回调接收页）和 `/403` 路由

### 关键调整
- **dev-login 拆到独立 Controller**：方法级 `@Profile("dev")` 在 `@RestController` 上不可靠，改为类级 `@Profile("dev")` 的 `DevAuthController`，prod 环境完全不加载
- **AuthController 移除 `SysUser` 直接依赖**：dev-login 用到，迁到 `DevAuthController`

---

## [Unreleased] — 2026-06-12 (Phase 1 骨架续)

### Added（新增）

#### 前端骨架
- **WorkbenchLayout.vue** —— 左侧导航布局，**双分组**：
  - **任务管理**（合并自 Task 项目，Phase 2 接入）：我的任务 / 历史记录 / 关系图谱 / 管理后台（仅 ADMIN）
  - **研发管理**（Phase 2 接入真实页面）：项目管理 / 里程碑甘特图 / 实验室设备 / 敏捷看板 / BOM 管理 / 工程变更 ECN / 缺陷跟踪 / 研发仪表盘
  - 顶栏：当前页标题（从路由 meta 读）、角色标签（EMPLOYEE/MANAGER/ADMIN）、用户下拉（个人中心 + 退出登录）
  - 移动端：抽屉式菜单、768px 响应式断点、隐藏用户名/角色标签
  - 路由切换淡入过渡
- **PlaceholderView.vue** —— 统一占位页面，根据路由 `meta` 自动展示：标题、所属模块、计划阶段、权限要求
- **router/index.ts** 重写 —— 嵌套路由结构（`/` → `WorkbenchLayout` → 12 个子路由）
- **stores/user.ts** —— Pinia 用户状态（userInfo / isAdmin / isManager / setUserInfo / clearUserInfo）
- **utils/request.ts** —— 占位 axios 实例（Phase 2 接入 JWT + 企微 OAuth）

#### 后端
- **FlowableConfig.java** —— Flowable 启动期日志，Phase 2 占位 Bean
- **V2__flowable_indexes_placeholder.sql** —— Flyway V2 占位（保证版本连续）
- `application.yml` 调整：`flowable.database-schema-update: false` → `true`（让 Flowable 自动建 ACT_* 表，避免版本错位）

### 关键决策
- V1 SQL 末尾追加 **22 个业务复合索引**（项目 / 设备 / BOM / ECN / 合规 5 类）
- 业务级 Flowable 索引放到 `FlowableConfig` 的 `@PostConstruct`（Phase 2 实现）
- 占位页面统一为 `PlaceholderView.vue`，避免 12 个重复页面
- 路由 `meta` 携带 `title` / `module` / `phase` / `requiresAdmin` 信息，WorkbenchLayout 自动渲染

---

## [Unreleased] — 2026-06-12 (Phase 1 骨架)

### 🎉 重大变更

**项目从零启动** —— 本仓库是新建的"仪器类产品研发管理系统",由 E:\Project\Task 项目合并而来。后续将把 Task 项目的功能模块逐步搬入本仓库(根包 `com.RD`)。

### Added（新增）

#### 基础架构
- **Spring Boot 3.2.0 工程骨架**（`E:\Project\RD\backend\`）
  - 启动类 `RdApplication.java`，根包 `com.RD`，启用 `@EnableAsync` / `@EnableScheduling`
  - 统一异常处理 `GlobalExceptionHandler`
  - 业务异常 `BusinessException`（含 status() 通用工厂）
  - 统一响应封装 `Result<T>`

#### 依赖
- 后端：`spring-boot-starter-web` / `security` / `data-redis` / `validation` / `aop` / `websocket` / `webflux`
- MyBatis-Plus 3.5.5 / MySQL Connector / Flyway（core + mysql）
- JJWT 0.12.3（api + impl + jackson）
- **Flowable 7.1.0** `flowable-spring-boot-starter` —— ECN 审批流引擎
- Lombok / springdoc-openapi 2.3.0 / spring-boot-starter-test
- 前端：Vue 3.4 + Vite 5 + TypeScript 5 + Element Plus 2 + Pinia 2 + ECharts 5 + vue-router 4 + Axios + CryptoJS + dayjs

#### 数据库
- **V1 全量建表 SQL**（`V1__init_all_tables.sql`）—— 25 张表：
  - 系统基础：`wework_config` / `sys_user` / `sys_department` / `sys_sync_log` / `sys_audit_log` / `sys_idempotency_key`
  - 任务管理（合并自 Task 项目）：`task` / `task_status_history` / `tasks_history_archive`
  - 项目管理：`project` / `milestone` / `sprint` / `sprint_task`
  - 实验室资源：`lab_equipment` / `equipment_reservation` / `equipment_maintenance`
  - BOM & ECN：`bom_header` / `bom_item` / `ecn_change` / `ecn_approval`
  - 合规审计：`document_approval` / `electronic_signature` / `defect_tracking`
- **命名约定**：
  - 系统表用 `sys_` 前缀
  - 业务主表无前缀
  - 保留 Task 项目的 `task` 表名，spec 里的敏捷看板 task 改名 `sprint_task`，避免重名

#### 部署
- `docker-compose.yml` —— MySQL 8.0 + Redis 7 + Backend + Frontend 四服务编排
- `backend/Dockerfile` —— 多阶段构建（maven → temurin JRE）
- `frontend/Dockerfile` —— 多阶段构建（node build → nginx）
- `frontend/nginx.conf` —— SPA fallback + gzip
- `.env.example` —— 完整环境变量模板

#### 前端骨架
- `HelloView.vue` —— Phase 1 首页（技术栈 / 已交付 / 系统状态三张卡片 + 提示横幅）
- `App.vue` / `main.ts` / `router/index.ts` / `style.css` / `vite-env.d.ts`
- Pinia 集成 / Element Plus 全局注册 + 图标组件化
- 路由懒加载（`@/views/HelloView.vue`）

#### 配置
- `application.yml` —— 全局配置（含 `flowable.*` 工作流引擎调优、wework 消息、jwt、storage、archive、sensitive-data）
- `flowable.database-schema-update=false`（避免 Flowable 启动时自动建表，由 Flyway V2 接管）
- `flowable.rest.{app,admin}.enabled=false`（不用 Flowable 自带 REST，自定义 Controller）
- Flowable 异步执行器配置（core-pool=8 / max-pool=16 / queue=200）

### 后续工作（Phase 2 起）

- [ ] Flyway V2：Flowable 7 核心表 DDL（`ACT_RE_*` / `ACT_HI_*` / `ACT_RU_*`）+ 关键复合索引
- [ ] 后端：`com.RD.auth` / `audit` / `privacy` / `idempotency` / `wework` / `storage` 公共包
- [ ] 后端：`com.RD.tasklegacy`（搬入 Task 项目业务代码，包名重命名）
- [ ] 后端：`com.RD.project` / `equipment` / `bom` / `ecn` / `workflow` / `defect` / `document` 业务包
- [ ] 前端：`views/{task,project,equipment,bom,ecn,workflow}` + `WorkbenchLayout` 导航分组
- [ ] 第一个 ECN 流程定义（`resources/processes/ecn-default.bpmn20.xml`）

---

## 历史里程碑

| 时间 | 事件 |
|------|------|
| 2026-06-12 | **Phase 1 骨架启动** —— 25 张表 + Spring Boot 3 + Flowable 7 + Vue 3 |
| 2026-06-10 | E:\Project\Task 项目代码成熟（30+ commits, 99% 完成度） |
