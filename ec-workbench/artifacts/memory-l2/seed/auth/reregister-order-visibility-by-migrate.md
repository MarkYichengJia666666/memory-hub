# 注销再注册：旧订单可见性取决于迁移分支

## decision
同一手机号注销后再注册，**是否还能看到旧订单取决于走哪条注册流程**。订单按 `user_id`/`account_id` 查，不认手机号；注销（`UserDeleteService.doDeleteUser`）不改订单归属。
- 走 `isDeletedAccountAllowMigrate` → `migrateUser`：**新 userId**，旧订单留在旧户 → **看不到**。
- 入口直接 `createUser`：按手机号能查到已注销用户则**复用旧 userId** → **能看到**旧订单。

## mouths
- auth / 注销再注册
- order

## anchors
- symbols: `UserDeleteService#doDeleteUser`, `UserService#migrateUser`, `isDeletedAccountAllowMigrate`, `UserController#createUser`
- tables: `financing_order.user_id`, `cash_loan_order.user_id`

## constraints
- 不要用「注销了订单就没了」一句话概括
- 排障先确认入口是否迁移分支

## evidence
- Cursor×ec · transcript `fc508b8e`

## status
active

## updated
2026-09-24
