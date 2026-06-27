# 管理端房源设施与标签接口

基础地址：`/api`

所有接口均位于 `/admin/**` 下，请求头需要携带管理端访问令牌：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

## TypeScript 类型

```ts
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface FacilityItem {
  id: string
  name: string
  iconKey: string | null
  sortOrder: number | null
  enabled: boolean
}

export interface HouseTagItem {
  id: string
  name: string
  tagType: string | null
  sortOrder: number | null
  enabled: boolean
}

export interface HouseAttributes {
  houseId: string
  facilities: FacilityItem[]
  tags: HouseTagItem[]
}

export interface FacilityPayload {
  name: string
  iconKey?: string | null
  sortOrder?: number
  enabled?: boolean
}

export interface UpdateFacilityPayload extends FacilityPayload {
  sortOrder: number
  enabled: boolean
}

export interface HouseTagPayload {
  name: string
  tagType: string
  sortOrder?: number
  enabled?: boolean
}

export interface UpdateHouseTagPayload extends HouseTagPayload {
  sortOrder: number
  enabled: boolean
}
```

## 1. 查询设施字典

```http
GET /api/admin/house-facilities
```

返回全部设施，包括已停用项。前端配置表单应禁止选择 `enabled: false` 的项目。

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "wifi",
      "name": "Wi-Fi",
      "iconKey": "wifi",
      "sortOrder": 1,
      "enabled": true
    }
  ]
}
```

## 2. 新增设施字典项

```http
POST /api/admin/house-facilities
```

```json
{
  "name": "Wi-Fi",
  "iconKey": "wifi",
  "sortOrder": 10,
  "enabled": true
}
```

`sortOrder` 不传时默认为 `0`，`enabled` 不传时默认为 `true`。成功后返回创建的 `FacilityItem`。

## 3. 编辑设施字典项

```http
PUT /api/admin/house-facilities/{id}
```

```json
{
  "name": "高速Wi-Fi",
  "iconKey": "wifi",
  "sortOrder": 10,
  "enabled": true
}
```

编辑接口采用完整更新，`name`、`sortOrder`、`enabled` 必填。

## 4. 删除设施字典项

```http
DELETE /api/admin/house-facilities/{id}
```

成功返回：

```json
{
  "code": 200,
  "message": "设施删除成功",
  "data": true
}
```

设施仍被房源引用时返回 `409`。如需保留历史关联，应通过编辑接口将 `enabled` 设置为 `false`，而不是删除。

## 5. 查询房源标签字典

```http
GET /api/admin/house-tags
```

返回全部标签，包括已停用项。

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "tag-metro",
      "name": "近地铁",
      "tagType": "traffic",
      "sortOrder": 1,
      "enabled": true
    }
  ]
}
```

## 6. 新增标签字典项

```http
POST /api/admin/house-tags
```

```json
{
  "name": "近地铁",
  "tagType": "traffic",
  "sortOrder": 10,
  "enabled": true
}
```

## 7. 编辑标签字典项

```http
PUT /api/admin/house-tags/{id}
```

```json
{
  "name": "地铁房",
  "tagType": "traffic",
  "sortOrder": 10,
  "enabled": true
}
```

编辑接口采用完整更新，四个字段均必填。

## 8. 删除标签字典项

```http
DELETE /api/admin/house-tags/{id}
```

标签仍被房源引用时返回 `409`；可以先解除房源关联或将标签停用。

## 9. 查询房源当前配置

```http
GET /api/admin/houses/{houseId}/attributes
```

示例：

```http
GET /api/admin/houses/house_006/attributes
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "houseId": "house_006",
    "facilities": [
      {
        "id": "wifi",
        "name": "Wi-Fi",
        "iconKey": "wifi",
        "sortOrder": 1,
        "enabled": true
      }
    ],
    "tags": [
      {
        "id": "tag-smart-lock",
        "name": "智能门锁",
        "tagType": "feature",
        "sortOrder": 3,
        "enabled": true
      }
    ]
  }
}
```

## 10. 替换房源设施

```http
PUT /api/admin/houses/{houseId}/facilities
```

请求体：

```json
{
  "facilityIds": ["wifi", "air_conditioner", "washing_machine"]
}
```

该接口是完整替换，不是增量追加。传入空数组会清空该房源的全部设施：

```json
{
  "facilityIds": []
}
```

成功后返回最新的 `HouseAttributes`。

## 11. 替换房源标签

```http
PUT /api/admin/houses/{houseId}/tags
```

请求体：

```json
{
  "tagIds": ["tag-metro", "tag-smart-lock"]
}
```

该接口同样是完整替换。传入空数组会清空全部标签。成功后返回最新的 `HouseAttributes`。

## 前端请求封装示例

```ts
import request from '@/utils/request'

export const getFacilityDictionary = () =>
  request.get<ApiResponse<FacilityItem[]>>('/admin/house-facilities')

export const createFacility = (data: FacilityPayload) =>
  request.post<ApiResponse<FacilityItem>>('/admin/house-facilities', data)

export const updateFacility = (id: string, data: UpdateFacilityPayload) =>
  request.put<ApiResponse<FacilityItem>>(`/admin/house-facilities/${id}`, data)

export const deleteFacility = (id: string) =>
  request.delete<ApiResponse<boolean>>(`/admin/house-facilities/${id}`)

export const getHouseTagDictionary = () =>
  request.get<ApiResponse<HouseTagItem[]>>('/admin/house-tags')

export const createHouseTag = (data: HouseTagPayload) =>
  request.post<ApiResponse<HouseTagItem>>('/admin/house-tags', data)

export const updateHouseTag = (id: string, data: UpdateHouseTagPayload) =>
  request.put<ApiResponse<HouseTagItem>>(`/admin/house-tags/${id}`, data)

export const deleteHouseTag = (id: string) =>
  request.delete<ApiResponse<boolean>>(`/admin/house-tags/${id}`)

export const getHouseAttributes = (houseId: string) =>
  request.get<ApiResponse<HouseAttributes>>(`/admin/houses/${houseId}/attributes`)

export const replaceHouseFacilities = (houseId: string, facilityIds: string[]) =>
  request.put<ApiResponse<HouseAttributes>>(`/admin/houses/${houseId}/facilities`, {
    facilityIds,
  })

export const replaceHouseTags = (houseId: string, tagIds: string[]) =>
  request.put<ApiResponse<HouseAttributes>>(`/admin/houses/${houseId}/tags`, {
    tagIds,
  })
```

## 错误响应

| HTTP 状态 | 场景 |
|---|---|
| 400 | ID 为空、字典项不存在或已经停用 |
| 401 | 未携带令牌或令牌失效 |
| 403 | 当前账号没有管理端权限 |
| 404 | 房源、设施字典项或标签字典项不存在 |
| 409 | 名称重复，或删除的字典项仍被房源引用 |

错误示例：

```json
{
  "code": 400,
  "message": "设施不存在或已停用: invalid-id",
  "data": null
}
```
