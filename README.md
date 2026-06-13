# zhuxiang-service

住享移动端应用后端服务，已实现《住享移动端接口文档 V0.2》第一批接口。

## 技术栈

- Java 21
- Spring Boot 3.5.15
- MyBatis-Plus 3.5.15
- MySQL 8.x
- Flyway
- Maven Wrapper

## 已实现接口

所有接口统一使用 `/api` 前缀。

- 认证：短信验证码、验证码登录、密码登录、注册、刷新 Token、退出登录
- 房源：首页瀑布流、搜索筛选、筛选项、房源详情、房东详情
- 收藏：收藏、取消收藏、我的收藏
- 业务：预约看房、租住申请、创建聊天会话
- 消息：列表、未读统计、单条/全部已读、删除、清理已读
- 个人中心：资料查询、资料修改、头像上传、当前住所摘要

需要登录的接口使用：

```http
Authorization: Bearer <accessToken>
```

## 数据库

创建数据库：

```sql
CREATE DATABASE zhuxiang_app
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

首次启动时 Flyway 会自动执行：

- `V1__create_first_batch_tables.sql`：创建第一批接口所需数据表
- `V2__seed_development_catalog.sql`：写入房源、房东和筛选项示例数据

## 配置

| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| JDBC 地址 | `DB_URL` | `jdbc:mysql://localhost:3306/zhuxiang_app?...` |
| 数据库用户名 | `DB_USERNAME` | `root` |
| 数据库密码 | `DB_PASSWORD` | 空 |
| 服务端口 | `SERVER_PORT` | `8000` |
| API 前缀 | `API_PREFIX` | `/api` |
| Token 密钥 | `TOKEN_SECRET` | 本地开发默认值 |
| 固定验证码 | `FIXED_SMS_CODE` | `123456` |
| 上传目录 | `UPLOAD_DIRECTORY` | `./uploads` |

当前阶段尚未对接短信供应商，本地验证码默认固定为 `123456`。部署到非开发
环境时必须设置安全的 `TOKEN_SECRET`，并在接入短信服务后移除固定验证码。

## 运行

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:TOKEN_SECRET = "replace-with-a-long-random-secret"
.\mvnw.cmd spring-boot:run
```

默认服务地址：

```text
http://localhost:8000/api
```

## 测试与构建

测试使用 H2 的 MySQL 兼容模式，不依赖本机 MySQL：

```powershell
.\mvnw.cmd clean verify
```
