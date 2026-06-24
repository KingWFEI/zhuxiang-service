# 管理端认证接口文档

**Base URL**: `http://localhost:8000/api`

---

## 1. 登录

```
POST /admin/auth/login
```

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号，11 位，格式 `^1\d{10}$` |
| password | string | 是 | 密码，明文传输 |

**请求示例**:
```json
{
    "phone": "13800138000",
    "password": "123456"
}
```

**成功响应** (200):

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 200 |
| message | string | "登录成功" |
| data.accessToken | string | 访问令牌 |
| data.refreshToken | string | 刷新令牌 |
| data.expiresIn | long | accessToken 有效期（秒） |
| data.user.id | string | 用户 ID |
| data.user.phone | string | 手机号 |
| data.user.nickname | string | 昵称 |
| data.user.avatarUrl | string | 头像 URL |
| data.user.role | string | 角色：ADMIN / HOUSEKEEPER / LANDLORD |
| data.user.isVerified | boolean | 是否已实名认证 |

```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "accessToken": "dXNlci0x.1719153600.abc123",
        "refreshToken": "aB3xYz...",
        "expiresIn": 7200,
        "user": {
            "id": "uuid-xxx",
            "phone": "13800138000",
            "nickname": "管理员张三",
            "avatarUrl": "",
            "role": "ADMIN",
            "isVerified": false
        }
    }
}
```

**错误码**:

| HTTP | code | message | 说明 |
|------|------|---------|------|
| 401 | 401 | 手机号或密码错误 | 账号不存在或密码错误 |
| 403 | 403 | 该账号无权登录管理端 | 角色为 TENANT 的普通用户 |
| 403 | 403 | 用户状态不可用 | 账号已被禁用或注销 |

---

## 2. 注册

```
POST /admin/auth/register
```

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号，11 位 |
| password | string | 是 | 密码，6-32 位 |
| nickname | string | 是 | 昵称，1-30 位 |
| role | string | 是 | 角色，只能为 ADMIN / HOUSEKEEPER / LANDLORD |

**请求示例**:
```json
{
    "phone": "13800138000",
    "password": "123456",
    "nickname": "管理员张三",
    "role": "ADMIN"
}
```

**成功响应** (200):

```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "accessToken": "dXNlci0y.1719153600.def456",
        "refreshToken": "xYz789...",
        "expiresIn": 7200,
        "user": {
            "id": "uuid-yyy",
            "phone": "13800138000",
            "nickname": "管理员张三",
            "avatarUrl": "",
            "role": "ADMIN",
            "isVerified": false
        }
    }
}
```

**错误码**:

| HTTP | code | message | 说明 |
|------|------|---------|------|
| 409 | 409 | 该手机号已注册 | 手机号重复 |
| 400 | 400 | 角色仅支持 ADMIN、HOUSEKEEPER、LANDLORD | role 值不合法 |

---

## 3. 刷新令牌

```
POST /admin/auth/refresh
```

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 登录/注册时获得的 refreshToken |

**请求示例**:
```json
{
    "refreshToken": "aB3xYz..."
}
```

**成功响应** (200):

```json
{
    "code": 200,
    "message": "刷新成功",
    "data": {
        "accessToken": "dXNlci0x.1719157200.new789",
        "refreshToken": "newRefreshToken...",
        "expiresIn": 7200
    }
}
```

---

## 4. 退出登录

```
POST /admin/auth/logout
```

**请求头**:
| 字段 | 说明 |
|------|------|
| Authorization | `Bearer {accessToken}` |

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 要注销的 refreshToken |

**请求示例**:
```json
{
    "refreshToken": "aB3xYz..."
}
```

**成功响应** (200):

```json
{
    "code": 200,
    "message": "退出成功",
    "data": true
}
```

---

## 角色枚举

| 值 | 说明 |
|------|------|
| `TENANT` | 租客（普通用户，**不可**登录管理端） |
| `HOUSEKEEPER` | 管家 |
| `LANDLORD` | 房东 |
| `ADMIN` | 管理员 |
