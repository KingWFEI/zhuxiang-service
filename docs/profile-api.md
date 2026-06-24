# 个人中心接口文档

## 通用说明

### 基础地址

```
http://<host>:<port>
```

### 认证方式

所有个人中心接口均需登录认证。在请求头中携带 JWT Token：

```
Authorization: Bearer <accessToken>
```

### 统一响应格式

所有接口返回统一的 JSON 结构 `ApiResponse<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 200 表示成功，其他值表示错误 |
| message | string | 成功时为 "success" 或自定义消息，失败时为错误描述 |
| data | object / null | 响应数据体，失败时为 null |

### 分页响应格式

带分页的接口返回 `PageData<T>` 作为 data：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [ ... ],
    "page": 1,
    "pageSize": 20,
    "hasMore": false,
    "total": 35
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 当前页数据列表 |
| page | long | 当前页码 |
| pageSize | long | 每页条数 |
| hasMore | boolean | 是否还有下一页 |
| total | long | 数据总条数 |

---

## 1. 获取当前用户资料

### 基本信息

- **路径**: `GET /profile`
- **描述**: 获取当前登录用户的个人资料信息

### 请求参数

无

### 请求示例

```bash
curl -X GET "http://localhost:8080/profile" \
  -H "Authorization: Bearer <accessToken>"
```

### 响应数据 `AuthDtos.UserView`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "a1b2c3d4e5f6...",
    "phone": "13800138000",
    "nickname": "小明",
    "avatarUrl": "/uploads/avatar/a1b2c3.jpg",
    "isVerified": true
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 用户 ID |
| phone | String | 手机号（脱敏展示由前端处理） |
| nickname | String | 昵称 |
| avatarUrl | String | 头像地址（相对路径或空字符串） |
| isVerified | boolean | 是否已实名认证 |

---

## 2. 更新当前用户资料

### 基本信息

- **路径**: `PUT /profile`
- **描述**: 更新当前用户的昵称和/或头像地址
- **Content-Type**: `application/json`

### 请求参数 `ProfileDtos.UpdateProfileRequest`

```json
{
  "nickname": "新昵称",
  "avatarUrl": "/uploads/avatar/xxx.jpg"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| nickname | String | 否 | 1-30 位 | 新昵称，不传则不更新 |
| avatarUrl | String | 否 | 最大 500 字符 | 头像地址，不传则不更新 |

> 两个字段均为可选，但至少需传一个。

### 请求示例

```bash
curl -X PUT "http://localhost:8080/profile" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"nickname": "新昵称"}'
```

### 响应数据 `AuthDtos.UserView`

同 [获取当前用户资料](#1-获取当前用户资料) 的响应，返回更新后的完整用户信息。

---

## 3. 上传用户头像

### 基本信息

- **路径**: `POST /profile/avatar`
- **描述**: 上传头像图片，服务端保存后返回头像访问地址
- **Content-Type**: `multipart/form-data`

### 请求参数

| 字段 | 类型 | 必填 | 位置 | 说明 |
|------|------|------|------|------|
| file | File | 是 | form-data | 头像图片文件（最大 5MB） |

### 请求示例

```bash
curl -X POST "http://localhost:8080/profile/avatar" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@/path/to/avatar.jpg"
```

### 响应数据 `ProfileDtos.AvatarResult`

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "avatarUrl": "/uploads/avatar/a1b2c3d4.jpg"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| avatarUrl | String | 上传后的头像访问地址（相对路径） |

### 错误说明

| code | message | 说明 |
|------|---------|------|
| 400 | 上传文件不能超过 5MB | 文件大小超过限制 |

---

## 4. 获取当前租约房源信息

### 基本信息

- **路径**: `GET /profile/current-home`
- **描述**: 获取当前用户正在履行（active/pending）的租约及其关联的房源和门锁摘要信息。若用户无租约返回 null。

### 请求参数

无

### 请求示例

```bash
curl -X GET "http://localhost:8080/profile/current-home" \
  -H "Authorization: Bearer <accessToken>"
```

### 响应数据 `ProfileDtos.CurrentHome`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "houseId": "house-uuid-001",
    "community": "阳光花园",
    "building": "3栋",
    "unit": "2单元",
    "room": "1201",
    "leaseId": "lease-uuid-001",
    "leaseStatus": "active",
    "lockId": "lock-uuid-001",
    "lockStatus": "online"
  }
}
```

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| houseId | String | 否 | 房源 ID |
| community | String | 否 | 小区名称，无数据时为空字符串 "" |
| building | String | 否 | 楼栋号 |
| unit | String | 否 | 单元号 |
| room | String | 否 | 房间号 |
| leaseId | String | 否 | 租约 ID |
| leaseStatus | String | 否 | 租约状态：`pending` 待生效 / `active` 生效中 / `expired` 已到期 / `terminated` 已退租 |
| lockId | String | 是 | 门锁设备 ID，无门锁时为 null |
| lockStatus | String | 否 | 门锁状态：`online` 在线 / `offline` 离线 / `low_battery` 低电量 / `unknown` 未知；无门锁时固定为 `"unknown"` |

---

## 5. 获取门锁展示信息

### 基本信息

- **路径**: `GET /profile/lock`
- **描述**: 获取当前用户租约对应的门锁展示信息，含门锁设备详情、租约摘要和权限有效期。专用于移动端个人中心门锁卡片展示。若用户无租约或无门锁设备返回 null。
- **与 `/profile/current-home` 的区别**: `current-home` 以房源为中心，门锁仅附带 ID 和在线状态；本接口以门锁为中心，返回门锁名称、品牌、电量、权限有效期等完整展示信息。

### 请求参数

无

### 请求示例

```bash
curl -X GET "http://localhost:8080/profile/lock" \
  -H "Authorization: Bearer <accessToken>"
```

### 响应数据 `ProfileDtos.LockInfo`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "lockId": "lock-uuid-001",
    "lockName": "1201门锁",
    "lockBrand": "通通锁",
    "lockStatus": "online",
    "batteryLevel": 85,
    "leaseId": "lease-uuid-001",
    "leaseStatus": "active",
    "startDate": "2026-01-01",
    "endDate": "2026-12-31",
    "permissionStatus": "active",
    "validFrom": "2026-01-01T00:00:00",
    "validTo": "2026-12-31T23:59:59"
  }
}
```

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| **门锁信息** | | | |
| lockId | String | 否 | 门锁设备 ID |
| lockName | String | 否 | 门锁名称（如 "1201门锁"） |
| lockBrand | String | 否 | 门锁品牌（如 "通通锁"） |
| lockStatus | String | 否 | 门锁在线状态：`online` 在线 / `offline` 离线 / `low_battery` 低电量 / `unknown` 未知 |
| batteryLevel | Integer | 否 | 电池电量百分比（0-100） |
| **租约信息** | | | |
| leaseId | String | 否 | 租约 ID |
| leaseStatus | String | 否 | 租约状态：`pending` 待生效 / `active` 生效中 |
| startDate | String | 是 | 租约开始日期（yyyy-MM-dd），无数据时为 null |
| endDate | String | 是 | 租约结束日期（yyyy-MM-dd），无数据时为 null |
| **权限信息** | | | |
| permissionStatus | String | 是 | 门锁权限状态：`active` 有效 / `expired` 已过期 / `revoked` 已回收；无权限记录时为 null |
| validFrom | String | 是 | 权限开始时间（ISO 8601），无数据时为 null |
| validTo | String | 是 | 权限结束时间（ISO 8601），无数据时为 null |

### 业务逻辑

1. 查询用户当前 active/pending 租约（取最新一条）
2. 通过租约关联的房源查询门锁设备
3. 通过 userId + leaseId + lockId 查询门锁权限
4. 任意环节无数据时返回 `null`

---

## 6. 分页获取收藏房源

### 基本信息

- **路径**: `GET /profile/favorite-houses`
- **描述**: 分页获取当前用户收藏的房源列表

### 请求参数

| 字段 | 类型 | 必填 | 默认值 | 校验 | 说明 |
|------|------|------|--------|------|------|
| page | long | 否 | 1 | 最小 1 | 页码 |
| pageSize | long | 否 | 20 | 1-100 | 每页条数 |

### 请求示例

```bash
curl -X GET "http://localhost:8080/profile/favorite-houses?page=1&pageSize=10" \
  -H "Authorization: Bearer <accessToken>"
```

### 响应数据 `PageData<HouseDtos.HouseView>`

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "house-uuid-001",
        "title": "阳光花园精装两室",
        "coverImage": "/uploads/house/cover001.jpg",
        "location": "朝阳区",
        "community": "阳光花园",
        "price": 350000,
        "roomType": "两室一厅",
        "area": 85,
        "floor": "12/28层",
        "orientation": "朝南",
        "tags": ["近地铁", "精装修", "智能门锁"],
        "facilities": ["wifi", "空调", "洗衣机"],
        "description": "精装修两室一厅，采光好...",
        "isSmartLockSupported": true,
        "isFavorite": true,
        "metro": "1号线-四惠站",
        "decoration": "精装修",
        "availableDate": "2026-07-01"
      }
    ],
    "page": 1,
    "pageSize": 10,
    "hasMore": false,
    "total": 1
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 房源 ID |
| title | String | 房源标题 |
| coverImage | String | 封面图地址 |
| location | String | 所属区域 |
| community | String | 小区名称 |
| price | Integer | 月租金（单位：分） |
| roomType | String | 户型（如 "两室一厅"） |
| area | Integer | 面积（平方米） |
| floor | String | 楼层信息（如 "12/28层"） |
| orientation | String | 朝向（如 "朝南"） |
| tags | List\<String\> | 标签列表 |
| facilities | List\<String\> | 设施列表 |
| description | String | 房源描述 |
| isSmartLockSupported | boolean | 是否支持智能门锁 |
| isFavorite | boolean | 当前用户是否已收藏（收藏列表中恒为 true） |
| metro | String | 附近地铁 |
| decoration | String | 装修情况（如 "精装修"） |
| availableDate | String | 可入住日期（yyyy-MM-dd） |
