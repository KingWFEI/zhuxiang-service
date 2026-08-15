# 后端改动交接（2026-08-15）

## 1. 交接范围

- 基线提交：`7a2aa63 feat: enhance recommendations and customer service integration`
- 发布分支：`codex/backend-migration-handoff`
- 本次整理内容：管理端账单接口、管理端系统概览、取消收藏离线房源、Flyway 迁移冲突处理及对应测试。
- `src/main/resources/application.yml` 中的本机数据库密码改动未纳入提交；数据库凭据应通过 `DB_PASSWORD` 环境变量提供。

## 2. 功能改动

### 管理端账单

新增接口：

- `GET /admin/bills`：分页查询账单，可按状态、到期日和关键词筛选。
- `GET /admin/bills/summary`：汇总账单数量、应收、实收、待收和逾期待收。
- `GET /admin/bills/{billId}`：查询账单以及关联租约、租客、房源和最近支付记录。

权限范围：

- `ADMIN`、`HOUSEKEEPER` 可查看平台账单。
- `LANDLORD` 仅可查看本人名下房源对应的账单。
- 其他角色返回 403。

金额字段沿用当前项目约定，以分为单位。

### 管理端系统概览

新增 `GET /admin/system/overview`，仅 `ADMIN` 可访问。接口返回代码内置角色定义和 `user` 表实时账号统计，包括账号总数、启用/禁用/注销数以及近 30 天登录数。

角色仍由代码管理，本次没有新增角色配置表。

### 房源取消收藏

取消收藏不再要求房源处于可用状态。用户可移除已下线房源的收藏；房源记录仍存在时，同步扣减收藏数并记录 `UNFAVORITE` 推荐事件。

## 3. Flyway 迁移说明

### `V20260812_04__backfill_missing_inspection_snapshots.sql`

该文件的两个字符串关联条件改为二进制比较，用于绕过历史表之间不同 collation 导致的 `Illegal mix of collations`，确保回填迁移能在字符集统一迁移之前执行。

此文件相对最初远端版本发生过内容变更。部署到已有数据库前必须先执行：

```powershell
.\mvnw.cmd flyway:validate
```

- 校验成功：继续执行迁移，不要运行 `repair`。
- 报 `V20260812_04` checksum mismatch：先确认该迁移此前已经成功完成，再备份 `flyway_schema_history`，最后执行 `flyway:repair` 更新 checksum。
- 存在 `V20260812_04` failed 记录：先确认失败原因确实是 collation 比较，清理该迁移产生的非事务性 DDL/数据残留后再执行 `repair` 和 `migrate`。
- 不要用 `repair` 掩盖其他校验错误，也不要手工删除已经成功的迁移记录。

### `V20260814_01__standardize_table_collations.sql`

新增迁移将历史业务表统一为 `utf8mb4_0900_ai_ci`。迁移会临时删除 `landlord_auth_proof` 到 `landlord_auth_application` 的外键，转换关联表后按原定义恢复；每个 DDL 都有 `information_schema` 守卫，失败后可在排查原因后重跑。

上线注意事项：

- 目标数据库必须为 MySQL 8.0；`utf8mb4_0900_ai_ci` 不适用于 MySQL 5.7/MariaDB。
- `ALTER TABLE ... CONVERT` 可能重建并锁定表，生产执行前应备份并安排维护窗口。
- MySQL DDL 非事务性；若中途失败，先检查表 collation 和外键状态，再重跑迁移。
- 完成后确认所有业务表使用目标 collation，并确认外键 `fk_landlord_auth_proof_application` 存在。

参考检查 SQL：

```sql
SELECT table_name, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND table_collation <> 'utf8mb4_0900_ai_ci';

SELECT constraint_name, table_name, referenced_table_name
FROM information_schema.referential_constraints
WHERE constraint_schema = DATABASE()
  AND constraint_name = 'fk_landlord_auth_proof_application';
```

本机 `zhuxiang_app` 的历史记录中还保留 `20260806.01 seed chongqing region hierarchy`，Flyway 显示为 `Deleted`，而当前仓库没有对应文件；新建数据库因此执行 89 个迁移，本机历史库显示 90 个。不要复用 `V20260806_01` 写入其他内容。

## 4. 已完成验证

- `flyway:validate`：本机 `zhuxiang_app` 校验通过，当前版本 `20260814.01`。
- 全新 MySQL 8 验证库：从空库执行 89 个迁移并再次校验成功。
- 全新验证库迁移后：非 `utf8mb4_0900_ai_ci` 业务表为 0，目标外键数量为 1。
- 定向单测：`AdminBillServiceTests`、`AdminSystemServiceTests`、`UserFavoriteHouseServiceTests` 共 7 项全部通过。
- OpenAPI 实际输出检查：本次 4 个新增接口均有摘要、说明和完整参数描述，4 个新增响应模型的全部 50 个字段均有描述。
- `mvnw -DskipTests package`：通过。

全量 `mvnw test` 当前仍未全绿：共执行 332 项，0 个断言失败、10 个错误。其中 2 个为既有 `AdminHouseImageCreationTests` 在房源价格为空时触发 NPE；另外 8 个集成测试错误来自 `application-test.yml` 所配置的本机测试库不可用。临时创建并迁移测试库后，OpenAPI 门禁可正常生成文档，但会先在既有 `GET /admin/advertisements` 缺少接口摘要处失败。这些问题与本次新增功能无直接关系，后续应分别修复测试数据/空值处理、把测试库连接改为可配置或容器化，并补齐已有接口文档。

## 5. 接手建议

1. 拉取分支后先执行 `git status` 和 `flyway:validate`，不要直接执行 `repair`。
2. 使用环境变量注入数据库账号密码，避免提交个人配置。
3. 在测试环境完整执行 `migrate`，重点观察字符集转换耗时和外键恢复结果。
4. 联调四个新增管理端接口，覆盖 `ADMIN`、`HOUSEKEEPER`、`LANDLORD`、`TENANT` 权限边界。
5. 修复全量测试的既有 10 个错误后，再将全量测试作为合并门禁。
