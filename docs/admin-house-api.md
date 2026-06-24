# 管理端房源接口文档

**Base URL**: `http://localhost:8000/api`

---

## 1. 新增房源

```
POST /admin/houses
```

**请求体** (JSON):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 房源标题 |
| coverImage | string | 是 | 封面图 URL |
| location | string | 是 | 位置描述，如"渝北区" |
| communityId | string | 是 | 所属小区 ID |
| address | string | 否 | 详细地址 |
| building | string | 否 | 楼栋，如"3栋" |
| unit | string | 否 | 单元，如"2单元" |
| room | string | 否 | 房间号，如"1201" |
| price | int | 是 | 月租金，单位：分（如 268000 = 2680 元） |
| deposit | int | 否 | 押金，单位：分，默认 0 |
| paymentMethod | string | 否 | 付款方式，如"押一付一" |
| roomType | string | 否 | 户型，如"1室1厅1卫" |
| area | number | 否 | 面积，单位平方米 |
| floor | string | 否 | 楼层描述，如"12/28层" |
| orientation | string | 否 | 朝向，如"朝南" |
| decoration | string | 否 | 装修情况，如"精装修" |
| availableDate | string | 否 | 可入住日期，格式 `YYYY-MM-DD` |
| metro | string | 否 | 地铁交通信息，如"距3号线500m" |
| description | string | 否 | 房源描述 |
| rentType | string | 是 | 租赁类型：recommended / short_rent / homestay / long_rent |
| landlordId | string | 是 | 房东/管家 ID |
| isSmartLockSupported | boolean | 否 | 是否支持智能门锁，默认 false |
| isSelfViewingSupported | boolean | 否 | 是否支持自主看房，默认 false |

**请求示例**:
```json
{
    "title": "温馨一居 · 阳光充足",
    "coverImage": "https://example.com/images/house-1.jpg",
    "location": "渝北区",
    "communityId": "community-1",
    "address": "重庆市渝北区幸福小区 3 栋 2 单元 1201",
    "building": "3栋",
    "unit": "2单元",
    "room": "1201",
    "price": 268000,
    "deposit": 268000,
    "paymentMethod": "押一付一",
    "roomType": "1室1厅1卫",
    "area": 42.00,
    "floor": "12/28层",
    "orientation": "朝南",
    "decoration": "精装修",
    "availableDate": "2026-07-01",
    "metro": "距 3 号线 500m",
    "description": "采光好，交通便利，拎包入住。",
    "rentType": "recommended",
    "landlordId": "landlord-1",
    "isSmartLockSupported": true,
    "isSelfViewingSupported": true
}
```

**成功响应** (200):

```json
{
    "code": 200,
    "message": "房源创建成功",
    "data": {
        "id": "uuid-xxx",
        "title": "温馨一居 · 阳光充足",
        "coverImage": "https://example.com/images/house-1.jpg",
        "location": "渝北区",
        "communityId": "community-1",
        "address": "重庆市渝北区幸福小区 3 栋 2 单元 1201",
        "building": "3栋",
        "unit": "2单元",
        "room": "1201",
        "price": 268000,
        "deposit": 268000,
        "paymentMethod": "押一付一",
        "roomType": "1室1厅1卫",
        "area": 42.00,
        "floor": "12/28层",
        "orientation": "朝南",
        "decoration": "精装修",
        "availableDate": "2026-07-01",
        "metro": "距 3 号线 500m",
        "description": "采光好，交通便利，拎包入住。",
        "rentType": "recommended",
        "status": "draft",
        "isSmartLockSupported": true,
        "isSelfViewingSupported": true,
        "smartLockBound": false,
        "lockDevice": null,
        "landlordId": "landlord-1",
        "viewCount": 0,
        "favoriteCount": 0,
        "createdAt": "2026-06-23T17:30:00",
        "updatedAt": "2026-06-23T17:30:00"
    }
}
```

> 新建房源状态默认为 `draft`（草稿），智能锁默认未绑定。

---

## 2. 查询所有房源（含智能锁信息）

```
GET /admin/houses
```

**请求参数**: 无

**成功响应** (200):

```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": "house-1",
            "title": "温馨一居 · 阳光充足",
            "coverImage": "https://example.com/images/house-1.jpg",
            "location": "渝北区",
            "communityId": "community-1",
            "address": "重庆市渝北区幸福小区 3 栋 2 单元 1201",
            "building": "3栋",
            "unit": "2单元",
            "room": "1201",
            "price": 268000,
            "deposit": 268000,
            "paymentMethod": "押一付一",
            "roomType": "1室1厅1卫",
            "area": 42.00,
            "floor": "12/28层",
            "orientation": "朝南",
            "decoration": "精装修",
            "availableDate": "2026-07-01",
            "metro": "距 3 号线 500m",
            "description": "采光好，交通便利，拎包入住。",
            "rentType": "recommended",
            "status": "available",
            "isSmartLockSupported": true,
            "isSelfViewingSupported": true,
            "smartLockBound": true,
            "lockDevice": {
                "lockId": "lock-1",
                "lockName": "1201门锁",
                "lockBrand": "住享智能锁",
                "lockSn": "ZX-LOCK-0001",
                "lockStatus": "online",
                "batteryLevel": 88
            },
            "landlordId": "landlord-1",
            "viewCount": 120,
            "favoriteCount": 8,
            "createdAt": "2026-06-12T21:34:00",
            "updatedAt": "2026-06-20T15:00:00"
        },
        {
            "id": "house-2",
            "title": "轻奢两居 · 近地铁",
            "location": "江北区",
            "isSmartLockSupported": true,
            "smartLockBound": false,
            "lockDevice": null
        },
        {
            "id": "house-3",
            "title": "普通单间",
            "isSmartLockSupported": false,
            "smartLockBound": false,
            "lockDevice": null
        }
    ]
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 房源 ID |
| status | string | draft=草稿, available=可租, rented=已租, offline=下架 |
| isSmartLockSupported | boolean | 房源是否支持智能门锁 |
| isSelfViewingSupported | boolean | 是否支持自主看房 |
| **smartLockBound** | **boolean** | **是否已绑定智能锁设备** |
| **lockDevice** | **object / null** | **绑定的门锁信息，未绑定为 null** |
| lockDevice.lockId | string | 门锁 ID |
| lockDevice.lockName | string | 门锁名称 |
| lockDevice.lockBrand | string | 门锁品牌 |
| lockDevice.lockSn | string | 门锁序列号 |
| lockDevice.lockStatus | string | online=在线, offline=离线, low_battery=低电量, unknown=未知 |
| lockDevice.batteryLevel | int | 电量百分比 (0-100) |
| price | int | 月租金，单位：分 |
| deposit | int | 押金，单位：分 |
| createdAt | string | 创建时间 ISO 8601 |
| updatedAt | string | 更新时间 ISO 8601 |

---

## 智能锁绑定状态判断逻辑

| isSmartLockSupported | smartLockBound | lockDevice | 含义 |
|----------------------|----------------|------------|------|
| `false` | `false` | `null` | 不支持智能锁 |
| `true` | `false` | `null` | 支持智能锁，但**未绑定**设备 |
| `true` | `true` | `{...}` | 支持智能锁，**已绑定**设备 |

---

## 租赁类型枚举

| 值 | 说明 |
|------|------|
| `recommended` | 推荐 |
| `short_rent` | 短租 |
| `homestay` | 民宿 |
| `long_rent` | 长租 |

## 房源状态枚举

| 值 | 说明 |
|------|------|
| `draft` | 草稿（新建默认状态） |
| `available` | 可租 |
| `rented` | 已租 |
| `offline` | 下架 |
