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
CREATE DATABASE ease_nest
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

首次启动时 Flyway 会自动执行当前基线迁移：

- `V1__create_application_schema.sql`：创建完整业务表、索引和外键。65 张业务表和 935 个字段统一使用 `utf8mb4_0900_ai_ci`，表及字段均有中文注释。
- `V2__seed_reference_catalogs.sql`：初始化创建房源所需的标准户型字典，不写入房源、用户、订单、门锁等样例业务数据。

如果从旧迁移历史切换到当前基线，必须先备份并重建目标数据库，使其为空后再启动服务；旧 `flyway_schema_history` 与当前基线不兼容。重建会删除目标数据库中的全部数据：

```sql
DROP DATABASE ease_nest;
CREATE DATABASE ease_nest
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

项目要求 MySQL 8.0 或更高版本。行政区划、小区、房源、标签、设施、广告等业务数据在首次启动后为空，需要由管理端配置或通过业务流程录入。超级管理员和平台房东继续由应用启动初始化器按配置创建。

## 配置

| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| JDBC 地址 | `DB_URL` | `jdbc:mysql://localhost:3306/ease_nest?characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai` |
| 数据库用户名 | `DB_USERNAME` | `root` |
| 数据库密码 | `DB_PASSWORD` | 空 |
| 服务端口 | `SERVER_PORT` | `8000` |
| API 前缀 | `API_PREFIX` | `/api` |
| Token 密钥 | `TOKEN_SECRET` | 本地开发默认值 |
| 固定验证码 | `FIXED_SMS_CODE` | `123456` |
| 图片存储类型 | `STORAGE_TYPE` | `local` |
| 本地公开文件目录 | `LOCAL_STORAGE_DIRECTORY` | `./uploads` |
| 本地敏感文件目录 | `LOCAL_PRIVATE_STORAGE_DIRECTORY` | `./uploads-private` |
| 本地公开文件 URL 路径 | `LOCAL_STORAGE_PUBLIC_PATH` | `/uploads`（不含 API 前缀） |
| COS SecretId | `TENCENT_COS_SECRET_ID` | 空 |
| COS SecretKey | `TENCENT_COS_SECRET_KEY` | 空 |
| COS 临时 Token | `TENCENT_COS_SESSION_TOKEN` | 空 |
| COS 地域 | `TENCENT_COS_REGION` | 空 |
| COS 存储桶 | `TENCENT_COS_BUCKET` | 空 |
| COS 访问域名 | `TENCENT_COS_PUBLIC_BASE_URL` | COS 默认域名 |
| COS 对象前缀 | `TENCENT_COS_KEY_PREFIX` | `zhuxiang` |

当前阶段尚未对接短信供应商，本地验证码默认固定为 `123456`。部署到非开发
环境时必须设置安全的 `TOKEN_SECRET`，并在接入短信服务后移除固定验证码。

### 初始化账号

超级管理员和勿忧管家平台房东由应用启动时的初始化器创建，不属于 Flyway SQL 迁移。启动前复制 `.env.example` 为 `.env`，并填写 `SUPER_ADMIN_PHONE`、`SUPER_ADMIN_PASSWORD`、`SUPER_ADMIN_NICKNAME`、`PLATFORM_LANDLORD_ID`、`PLATFORM_LANDLORD_PHONE` 和 `PLATFORM_LANDLORD_NAME`。对应的 `*_ENABLED` 变量可设为 `false` 以关闭该账号的初始化。

初始化器只创建缺失的账号，不会因环境变量变化而修改已存在账号的密码或资料。

### 使用 `.env` 配置 COS

复制 `.env.example` 为项目根目录下的 `.env`，填写腾讯云 COS 参数并设置：

```properties
STORAGE_TYPE=cos
TENCENT_COS_SECRET_ID=your-secret-id
TENCENT_COS_SECRET_KEY=your-secret-key
TENCENT_COS_REGION=ap-guangzhou
TENCENT_COS_BUCKET=example-1250000000
TENCENT_COS_PUBLIC_BASE_URL=https://example-1250000000.cos.ap-guangzhou.myqcloud.com
TENCENT_COS_KEY_PREFIX=zhuxiang
```

使用本地存储时，将 `STORAGE_TYPE` 设为 `local`，并用 `LOCAL_STORAGE_DIRECTORY`、`LOCAL_PRIVATE_STORAGE_DIRECTORY` 和 `LOCAL_STORAGE_PUBLIC_PATH` 配置公开文件目录、敏感文件目录及 URL 路径。敏感文件目录必须位于公开文件目录之外。

`.env` 已加入 Git 忽略列表。生产环境建议直接设置系统环境变量；系统环境变量
优先级高于 `.env`，无需修改或重新打包后端。COS 密钥应使用仅具备目标路径上传
权限的 CAM 子账号密钥。当前上传接口返回稳定对象 URL，因此对应 COS 域名需要具备
合适的读取策略；身份证图片等敏感资源不应配置为全网公开读。

文件上传接口：

- `POST /api/files/upload`：租客实名认证图片，仅支持 `id_card_front`、`id_card_back`。
- `POST /api/admin/files/house-images/upload`：管理端房源图片，仅允许管理员、管家或房东。

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

集成测试使用 MySQL 测试库 `zhuxiang_test` 和 Flyway，测试库也需要按上述字符集与排序规则创建。运行测试前确认 `src/test/resources/application-test.yml` 中的连接信息对应可清空的测试库：

```powershell
.\mvnw.cmd clean verify
```
