# 生产部署指南 (Deployment Guide)

> 适用版本: Phase 3.x (commit ≥ `cadef45`)
> 目标读者: 系统管理员 / 运维工程师
> 预计耗时: 首次完整部署 60-90 分钟

---

## 目录

1. [部署架构总览](#1-部署架构总览)
2. [环境要求](#2-环境要求)
3. [前置准备](#3-前置准备)
4. [快速启动（Dev）](#4-快速启动dev)
5. [生产部署（推荐 Docker Compose）](#5-生产部署推荐-docker-compose)
6. [企业微信后台配置](#6-企业微信后台配置)
7. [首次登录与系统初始化](#7-首次登录与系统初始化)
8. [HTTPS / 反向代理（Nginx 范例）](#8-https--反向代理nginx-范例)
9. [数据备份与恢复](#9-数据备份与恢复)
10. [升级与回滚](#10-升级与回滚)
11. [常见问题](#11-常见问题)

---

## 1. 部署架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                       浏览器 / 企微 App                          │
│                  (HTTPS 443 / HTTP 80)                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│   Nginx (反向代理 + 静态资源)                                     │
│   - 80  → 转发 /api/* 到 backend:8080                            │
│   - 80  → 兜底 SPA（vue-router history 模式）                    │
│   - 443 → SSL termination (Let's Encrypt)                        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│   Frontend Container (nginx alpine, ~30MB)                       │
│   静态 dist/ + nginx.conf → 5173 → 80                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ (业务请求 /api/*)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│   Backend Container (eclipse-temurin:17-jre, ~300MB)             │
│   Spring Boot 3.2 + Flowable 7.1 + MyBatis-Plus 3.5.5            │
│   9 业务模块 / 19 REST controller / JWT 鉴权 / 审计日志            │
└────────────────┬──────────────────────┬────────────────────────┘
                 │                      │
                 ▼                      ▼
┌─────────────────────────┐  ┌──────────────────────┐
│   MySQL 8.0             │  │   Redis 7            │
│   10 业务表 (Flyway V1)  │  │   Token 黑名单        │
│   + 2 索引 (Flyway V2)  │  │   调度锁 / 限流        │
│   + 30+ Flowable ACT_*  │  │                      │
└─────────────────────────┘  └──────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │  对象存储       │
        │  (MinIO/OSS)   │
        │  业务文件       │
        └────────────────┘
```

**默认端口分配**:

| 服务 | 容器内 | 宿主机映射 | 用途 |
|------|--------|-----------|------|
| Frontend | 80 | 5173 | Web UI / SPA 静态 |
| Backend | 8080 | 8080 | REST API |
| MySQL | 3306 | 127.0.0.1:3306 | 仅本机 |
| Redis | 6379 | 127.0.0.1:6379 | 仅本机 |
| MinIO (可选) | 9000/9001 | 127.0.0.1:9000 | 对象存储 |

> **生产建议**: MySQL/Redis/MinIO 端口**只绑 127.0.0.1**,由后端内部访问,外网完全不暴露。

---

## 2. 环境要求

### 2.1 服务器最低配置

| 项 | 最低 | 推荐 |
|----|------|------|
| CPU | 2 vCPU | 4 vCPU |
| RAM | 4 GB | 8 GB |
| 磁盘 | 50 GB SSD | 200 GB SSD |
| OS | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| 公网 | 固定 IP + 备案域名 | 同左 |

### 2.2 依赖软件

| 软件 | 版本 | 用途 |
|------|------|------|
| Docker | ≥ 24.0 | 容器引擎 |
| Docker Compose | ≥ v2.20 | 编排 |
| Nginx (host) | ≥ 1.20 | 反向代理 + HTTPS |
| Certbot (可选) | latest | Let's Encrypt 证书 |

### 2.3 网络出口

- HTTPS 443 (Certbot 验证)
- 企微 API: `https://qyapi.weixin.qq.com` (OAuth + 消息推送)
- 阿里云 OSS: `https://oss-cn-*.aliyuncs.com` (如使用对象存储)
- Docker Hub (首次拉镜像)

---

## 3. 前置准备

### 3.1 域名 + 备案

需要 2 个域名 (可同主域):

- `rd.example.com` — 前端 Web
- `api.rd.example.com` — 后端 API

> 企微 OAuth 回调必须用 **HTTPS**,所以生产必须有域名+证书。

### 3.2 准备环境变量

复制 `.env.example` 为 `.env`:

```bash
cp .env.example .env
vim .env
```

**必须修改的项**:

```bash
# === JWT 密钥 (32 字符以上) ===
JWT_SECRET=$(openssl rand -base64 32)
FILE_DOWNLOAD_SECRET=$(openssl rand -base64 32)

# === 数据库密码 (强密码) ===
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24)
DB_PASSWORD=$MYSQL_ROOT_PASSWORD
REDIS_PASSWORD=$(openssl rand -base64 24)

# === 前端 Token 加密 (32 字符以上) ===
VITE_TOKEN_SECRET=$(openssl rand -base64 32)

# === 企微 (后端 + 前端必须 3 个一致) ===
WEWORK_CORP_ID=ww1234567890abcdef
WEWORK_AGENT_ID=1000002
WEWORK_SECRET=abcdef1234567890abcdef1234567890

VITE_WEWORK_CORP_ID=ww1234567890abcdef
VITE_WEWORK_AGENT_ID=1000002
VITE_WEWORK_OAUTH_REDIRECT_URI=https://api.rd.example.com/api/auth/wework/callback

# === 生产 profile ===
SPRING_PROFILE=prod
WEWORK_MESSAGE_ENABLED=true

# === 前端基础 URL (OAuth 回调跳转目标) ===
FRONTEND_BASE_URL=https://rd.example.com
```

> **关键点**: `JWT_SECRET` / `FILE_DOWNLOAD_SECRET` / `VITE_TOKEN_SECRET` / `MYSQL_ROOT_PASSWORD` 全部用 `openssl rand` 生成,绝不复用 dev 值。

### 3.3 修改 docker-compose.yml (生产化)

`docker-compose.yml` 默认 ports 都是 `127.0.0.1:xxxx`,生产建议:

- **后端**改为 `8080:8080` 仍绑本机,由 host Nginx 反代
- **前端**容器其实不需要对外,直接让 host Nginx serve `dist/` 即可;或继续容器化

两种选择 (二选一):

**方案 A**: 全部容器化 + 1 个 host Nginx 反代 (推荐, 易升级)

```yaml
# docker-compose.yml (生产) 改 ports
services:
  backend:
    ports:
      - "127.0.0.1:8080:8080"   # 保持
  frontend:
    ports:
      - "127.0.0.1:5173:80"     # 保持
```

**方案 B**: 只容器化后端,前端在 host 上 npm build + Nginx serve

```bash
cd frontend && npm ci && npm run build
# 把 dist/ 拷到 /var/www/rd/dist
```

---

## 4. 快速启动 (Dev)

> 仅本地开发用,不要在生产用这套!

```bash
# 1. 克隆
git clone https://github.com/micsig001/Jaysen-RD.git
cd Jaysen-RD

# 2. 准备 dev .env
cp .env.example .env
# (dev 值可以不改, 默认 OK)

# 3. 启动
docker compose up -d
# 自动完成: 构建 backend/frontend 镜像 → 启动 mysql/redis/backend/frontend
# 首次启动需 5-8 分钟 (maven 拉依赖 + npm install)

# 4. 查看日志
docker compose logs -f backend
# 等到 "Started Application in X.XX seconds"

# 5. 验证
curl http://localhost:8080/api/health    # 200
浏览器开 http://localhost:5173
# 用 dev 模拟登录: 4 个内置企微用户 (admin/manager/lead/employee)
```

### Dev 登录

dev profile 自动注册 `FakeWeWorkApiClient`,4 个内置账号:
- `admin`  (管理员)
- `manager` (部门经理)
- `lead` (项目负责人)
- `employee` (普通员工)

---

## 5. 生产部署 (推荐 Docker Compose)

### 5.1 部署步骤

```bash
# 1. 拉取代码 (建议打 tag 后)
cd /opt
git clone https://github.com/micsig001/Jaysen-RD.git rd-system
cd rd-system
git checkout v3.0.0  # 选稳定 tag

# 2. 准备生产 .env (见 §3.2)
cp .env.example .env
vim .env
# 设置 SPRING_PROFILE=prod, 改所有密钥, 填企微 3 项

# 3. 修改 docker-compose.yml 取消 dev 默认
# SPRING_PROFILE 已经在 .env 里覆盖
# 不需要改文件本身

# 4. 启动 (production 环境变量已就绪)
docker compose up -d

# 5. 等待健康检查
docker compose ps
# 期望: 4 个服务都 Up + healthy
```

### 5.2 验证部署

```bash
# 后端健康
curl http://127.0.0.1:8080/actuator/health  # 需先开 actuator

# 数据库迁移状态
docker compose exec mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD rd_system \
  -e "SELECT version, description, success FROM flyway_schema_history;"
# 期望 V1__init_all_tables + V2__idx_* 全 success=1

# Flowable 表是否就绪
docker compose exec mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD rd_system \
  -e "SHOW TABLES LIKE 'ACT_%';"  # 至少 30+ 张

# 查看后端日志
docker compose logs --tail=100 backend | grep -E "Started|Boot|ERROR"
```

### 5.3 防火墙

```bash
# Ubuntu UFW
ufw allow 22/tcp     # SSH
ufw allow 80/tcp     # HTTP
ufw allow 443/tcp    # HTTPS
ufw enable

# 禁止外网访问 3306/6379/9000
# (已经在 docker-compose.yml 绑 127.0.0.1, 默认不暴露)
```

---

## 6. 企业微信后台配置

> 企微配置是 OAuth 登录的前提。**先在企微后台改好, 再启动后端**。

### 6.1 创建自建应用

1. 登录 [企业微信管理后台](https://work.weixin.qq.com)
2. 「应用管理」→「自建」→ 点「创建应用」
3. 填写:
   - 应用名称: 研发管理系统
   - 应用 Logo: 上传
   - 应用介绍: 内部 R&D 业务系统
   - 可见范围: 选根部门 (或指定部门)

### 6.2 配置 OAuth 授权

「研发管理系统」详情页 → 「网页授权及 JS-SDK」:

- **可信域名**: `rd.example.com` (前端域, 不带 https://)
- **授权回调域**: `api.rd.example.com` (后端域, 不带 https:// 也不带 /api/...)

> 企微用 host 部分校验回调域,所以填根域即可。

### 6.3 拿到凭证

「我的企业」→「企业信息」:
- **CorpID**: `ww1234567890abcdef` (类似格式, 复制到 .env 的 `WEWORK_CORP_ID`)

「研发管理系统」→「应用详情」:
- **AgentId**: `1000002` (数字, 复制到 .env 的 `WEWORK_AGENT_ID`)
- **Secret**: 点「查看」→ 发送到企业微信 → 复制到 .env 的 `WEWORK_SECRET`

### 6.4 配置消息推送 (可选, 用于通知)

「研发管理系统」→「接收消息」:
- **设置 API 接收** → Token / EncodingAESKey (后端对应, 见 `application.yml` 的 `wework.callback.*`)
- 当前版本消息走 `WeWorkMessageService` 主动调用, **不需要**配置 API 接收

### 6.5 .env 校验清单

```bash
grep -E "^(WEWORK|VITE_WEWORK)" .env
# 预期输出 (示例):
# WEWORK_CORP_ID=ww1234567890abcdef
# WEWORK_AGENT_ID=1000002
# WEWORK_SECRET=abcdef1234...
# VITE_WEWORK_CORP_ID=ww1234567890abcdef
# VITE_WEWORK_AGENT_ID=1000002
# VITE_WEWORK_OAUTH_REDIRECT_URI=https://api.rd.example.com/api/auth/wework/callback
# FRONTEND_BASE_URL=https://rd.example.com
```

### 6.6 验证 OAuth 登录

1. 重启后端: `docker compose restart backend`
2. 浏览器开 `https://rd.example.com`
3. 登录页应看到「企业微信登录」按钮**可点**(不灰)
4. 点 → 跳企微扫码 → 同意 → 自动跳回前端 → 看到 user 名 + 部门

---

## 7. 首次登录与系统初始化

### 7.1 第一次 OAuth 登录

OAuth 自动 upsert 第一个登录用户, **默认 role=EMPLOYEE**, 无 admin 权限。

**需要手动提权为 ADMIN**:

```sql
-- 找到自己
SELECT id, user_id, name, role FROM sys_user;

-- 提权 (假设 user_id = 'ZhangSan')
UPDATE sys_user
SET role = 'ADMIN', manual_role = TRUE, updated_at = NOW()
WHERE user_id = 'ZhangSan';
```

> 第二次该用户登录, 系统保留 manual_role=TRUE, 不会自动降级。

### 7.2 创建部门

```sql
INSERT INTO sys_department (dept_name, parent_id, created_at, updated_at)
VALUES
  ('研发部', 0, NOW(), NOW()),
  ('硬件组', 1, NOW(), NOW()),
  ('软件组', 1, NOW(), NOW());
```

### 7.3 创建项目 (可选, 也可走 Web 界面)

Web 界面入口: `https://rd.example.com/rd/projects` → 点「新建项目」

### 7.4 配置审批流 (可选, ECN)

ECN 默认流程在 `backend/src/main/resources/processes/ecn-default.bpmn20.xml`, Phase 2.6 已就绪, **不需要手动配置**。

---

## 8. HTTPS / 反向代理 (Nginx 范例)

`/etc/nginx/sites-available/rd.conf`:

```nginx
# --- HTTP → HTTPS 重定向 ---
server {
    listen 80;
    server_name rd.example.com api.rd.example.com;
    return 301 https://$host$request_uri;
}

# --- 前端 (rd.example.com) ---
server {
    listen 443 ssl http2;
    server_name rd.example.com;

    ssl_certificate     /etc/letsencrypt/live/rd.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/rd.example.com/privkey.pem;

    # 静态资源 (前端构建产物)
    root /var/www/rd/dist;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|svg|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 安全头
    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-XSS-Protection "1; mode=block";
}

# --- 后端 (api.rd.example.com) ---
server {
    listen 443 ssl http2;
    server_name api.rd.example.com;

    ssl_certificate     /etc/letsencrypt/live/api.rd.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.rd.example.com/privkey.pem;

    # 上传文件大小 (业务 PDF/图纸)
    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

```bash
# 启用
ln -s /etc/nginx/sites-available/rd.conf /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx

# 申请证书
certbot --nginx -d rd.example.com -d api.rd.example.com
```

---

## 9. 数据备份与恢复

### 9.1 MySQL 备份

```bash
# 全量每日 (cron 跑)
docker compose exec -T mysql mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers \
  rd_system | gzip > /backup/rd-$(date +%Y%m%d).sql.gz

# 保留 30 天
find /backup -name 'rd-*.sql.gz' -mtime +30 -delete
```

### 9.2 MySQL 恢复

```bash
# 1. 停后端 (防写)
docker compose stop backend frontend

# 2. 灌数据
gunzip -c /backup/rd-20260101.sql.gz | \
  docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" rd_system

# 3. 启后端
docker compose start backend frontend
```

### 9.3 Redis 备份 (可选)

Redis 只存 token 黑名单 + 调度锁,**丢失不影响业务数据**, 一般不备份。
如要备份:

```bash
docker compose exec -T redis sh -c \
  "redis-cli -a $REDIS_PASSWORD BGSAVE && \
   cat /data/dump.rdb" > /backup/redis-$(date +%Y%m%d).rdb
```

### 9.4 上传文件备份

```bash
# 本地存储
tar czf /backup/uploads-$(date +%Y%m%d).tar.gz /opt/rd-system/uploads

# MinIO
mc mirror minio/rd-files /backup/minio-rd-files
```

---

## 10. 升级与回滚

### 10.1 升级流程

```bash
# 1. 拉新代码
cd /opt/rd-system
git fetch --tags
git checkout v3.1.0  # 新 tag

# 2. 看 CHANGELOG, 确认有 Flyway 新 V* 文件需要跑
git log v3.0.0..v3.1.0 -- backend/src/main/resources/db/migration/

# 3. 重新构建 + 启动
docker compose build backend frontend
docker compose up -d

# 4. 验证 Flyway 自动应用
docker compose logs backend | grep -i "flyway\|migrating"
```

### 10.2 回滚

```bash
# 1. 停服务
docker compose down

# 2. 切回旧 tag
git checkout v3.0.0

# 3. 恢复数据库 (如果新版本已执行了破坏性 V* 迁移)
gunzip -c /backup/rd-before-upgrade.sql.gz | \
  docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" rd_system

# 4. 启动
docker compose up -d
```

> **回滚前必看**: 看新版本 Flyway 是否有 `ALTER TABLE` / `DROP`. 如果只是 `CREATE INDEX` / 新建表, 不需要回滚数据库。

---

## 11. 常见问题

### Q1. 启动后端报错 `JWT secret must be at least 32 bytes`

**原因**: `.env` 里 `JWT_SECRET` 为空或太短。
**解决**: `echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env`

### Q2. 企微登录按钮一直灰的

**原因**: 前端 `.env` 缺 `VITE_WEWORK_*` 3 个变量。
**解决**: 确认 `VITE_WEWORK_CORP_ID` / `VITE_WEWORK_AGENT_ID` / `VITE_WEWORK_OAUTH_REDIRECT_URI` 都有, 然后 `docker compose build frontend` 重新构建 (Vite 是编译时注入, 运行时改 env 无效)。

### Q3. 跳登录页后 OAuth 回调 `state` 报错

**原因**: 企微后台授权回调域填错。
**解决**: 必须填**后端域** (api.rd.example.com), 不能填前端域。

### Q4. 后端连不上 MySQL

```bash
# 检查容器
docker compose ps
docker compose logs mysql | tail -20

# 看健康检查
docker compose exec mysql mysqladmin ping -h localhost
```

**常见**: `MYSQL_ROOT_PASSWORD` 含特殊字符 → 改用 base64 字符集 (字母数字)。

### Q5. ECN 审批流没启动

```bash
# 看 Flowable 错误
docker compose logs backend | grep -iE "flowable|process|ecn"

# 看 BPMN 文件是否在 jar 里
docker compose exec backend sh -c \
  "jar tf /app/app.jar | grep bpmn"
# 期望: backend/src/main/resources/processes/ecn-default.bpmn20.xml
```

### Q6. 前端刷新页面 404

**原因**: 用了 vue-router history 模式, Nginx 没配 SPA fallback。
**解决**: `location / { try_files $uri $uri/ /index.html; }` (本指南 §8 已给)

### Q7. 磁盘涨得快

```bash
# 看 Docker 占盘
docker system df

# 清理悬空镜像
docker image prune -f
# 慎用 docker system prune (会清掉所有未用)
```

### Q8. JWT_SECRET 轮换 (重置所有用户登录)

```bash
# 1. 改 .env 的 JWT_SECRET
# 2. 重启 backend
docker compose restart backend
# 所有 access + refresh token 失效, 用户需重新登录 (符合预期)
```

---

## 附录 A: 完整环境变量清单

| 变量 | 必填 | 用途 | 示例 |
|------|------|------|------|
| `SERVER_PORT` | 否 | Spring Boot 端口 | `8080` |
| `BACKEND_PORT` | 否 | Host 映射端口 | `8080` |
| `FRONTEND_PORT` | 否 | 前端暴露端口 | `5173` |
| `SPRING_PROFILE` | **是** | dev / prod | `prod` |
| `VITE_TOKEN_SECRET` | **是** | 前端 AES 密钥 | base64 32 字符 |
| `DB_HOST` | **是** | MySQL host | `mysql` (容器名) |
| `DB_PORT` | **是** | MySQL port | `3306` |
| `DB_NAME` | **是** | 数据库名 | `rd_system` |
| `DB_USERNAME` | **是** | 用户名 | `root` |
| `DB_PASSWORD` | **是** | 用户密码 | base64 24 字符 |
| `MYSQL_ROOT_PASSWORD` | **是** | MySQL root | 同上 |
| `REDIS_HOST` | **是** | Redis host | `redis` |
| `REDIS_PORT` | **是** | Redis port | `6379` |
| `REDIS_PASSWORD` | **是** | Redis 密码 | base64 24 字符 |
| `JWT_SECRET` | **是** | JWT 签名 | base64 32 字符 |
| `FILE_DOWNLOAD_SECRET` | **是** | 下载 HMAC | base64 32 字符 |
| `WEWORK_CORP_ID` | **是** | 企微 CorpID | `ww1234567890abcdef` |
| `WEWORK_AGENT_ID` | **是** | 企微 AgentId | `1000002` |
| `WEWORK_SECRET` | **是** | 企微 Secret | 32 字符 |
| `WEWORK_MESSAGE_ENABLED` | **是** | 是否发企微消息 | `true` |
| `VITE_WEWORK_CORP_ID` | **是** | 前端 CorpID | 同 WEWORK |
| `VITE_WEWORK_AGENT_ID` | **是** | 前端 AgentId | 同 WEWORK |
| `VITE_WEWORK_OAUTH_REDIRECT_URI` | **是** | OAuth 回调 URL | `https://api.../api/auth/wework/callback` |
| `VITE_API_URL` | **是** | 前端调 API base | `https://api.rd.example.com` |
| `FRONTEND_BASE_URL` | **是** | 后端跳转前端 | `https://rd.example.com` |
| `STORAGE_TYPE` | 否 | local / minio / aliyun-oss | `local` |
| `MINIO_*` | 条件 | MinIO 配置 | — |
| `OSS_*` | 条件 | 阿里云 OSS | — |
| `ALLOWED_ORIGINS` | **是** | CORS origin | `https://rd.example.com` |
| `SENSITIVE_DATA_ENABLED` | 否 | 脱敏开关 | `true` |
| `ARCHIVE_ENABLED` | 否 | 归档开关 | `true` |
| `ARCHIVE_CRON` | 否 | 归档 cron | `0 0 3 1 * ?` |

---

## 附录 B: 健康检查脚本

`/usr/local/bin/rd-health.sh`:

```bash
#!/bin/bash
# 每 5 分钟跑一次, 失败发邮件
BACKEND="http://127.0.0.1:8080/actuator/health"
MYSQL_CONTAINER="rd-mysql"
REDIS_CONTAINER="rd-redis"

# 后端
if ! curl -fs "$BACKEND" > /dev/null; then
  echo "ALERT: backend down" | mail -s "[RD] backend down" admin@example.com
  docker compose -f /opt/rd-system/docker-compose.yml restart backend
fi

# MySQL
if ! docker exec $MYSQL_CONTAINER mysqladmin ping -h localhost --silent; then
  echo "ALERT: mysql down" | mail -s "[RD] mysql down" admin@example.com
fi

# Redis
if ! docker exec $REDIS_CONTAINER redis-cli ping | grep -q PONG; then
  echo "ALERT: redis down" | mail -s "[RD] redis down" admin@example.com
fi
```

```bash
chmod +x /usr/local/bin/rd-health.sh
echo "*/5 * * * * root /usr/local/bin/rd-health.sh" > /etc/cron.d/rd-health
```

---

## 附录 C: 性能基线 (供参考)

| 场景 | P95 响应 | QPS | 备注 |
|------|---------|-----|------|
| 登录 (OAuth) | 800ms | 5 | 含企微远程调用 |
| 看板拖拽 | 50ms | 50 | 单次 saveStatus |
| 拉 Sprint 列表 | 120ms | 30 | 含 5 状态 join |
| BOM 多阶 (3 层) | 180ms | 20 | 递归展开 |
| ECN 启动流程 | 600ms | 2 | Flowable 启动开销 |

> 实测环境: 4 vCPU / 8GB / SSD / 50 并发用户, Docker Compose 单机部署。

---

**部署完成 ✓** — 接下来访问 `https://rd.example.com` 用企微扫码登录。
