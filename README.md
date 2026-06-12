# zhuxiang-service

住享移动端应用的后端服务基础项目。

## 技术栈

- Java 21
- Spring Boot 3.5.15
- MyBatis-Plus 3.5.15
- MySQL
- Maven Wrapper

## 当前范围

项目目前只包含可运行的后端基础骨架和数据库连接配置，暂未添加
Controller、Service、Mapper、Entity 等业务代码。

## 环境要求

- JDK 21 或更高版本
- MySQL 8.x

默认数据库配置：

| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| JDBC 地址 | `DB_URL` | `jdbc:mysql://localhost:3306/zhuxiang?...` |
| 用户名 | `DB_USERNAME` | `root` |
| 密码 | `DB_PASSWORD` | 空 |
| 服务端口 | `SERVER_PORT` | `8000` |

首次运行前请创建数据库：

```sql
CREATE DATABASE zhuxiang
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

## 运行项目

Windows PowerShell：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run
```

服务默认地址为 `http://localhost:8000/api`。

## 构建与测试

```powershell
.\mvnw.cmd clean verify
```
