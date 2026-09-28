# E 档打开 App 补发：必须绕过全局 20 分钟 CUT_INTEREST 短路

## decision
首贷 E 档打开 App 灌券（`OpenAppGrantCouponService` 的 E 补发支路）插入点若落在全局「打开 App 20 分钟门槛 / CUT_INTEREST cache」之后，会被前面的 `else return` 挡掉。**E 分支必须提前绕过**该全局短路（独立更短窗口，如 10min），不能把全局 cache 当唯一闸。
券冲突：24h 内有任意降息券则不灌；24h 后只看是否已有**本实验工具券**，其他降息券不拦。入组分组名要以实验平台实际组名为准（`EXPERIMENT_GROUP` vs `EXPERIMENT_GROUP_ONE`），不要代码与平台各认一套。

## workspace
- `TAPD-1371553-erank-10min-coupon`

## mouths
- 打开 App 发券 / OpenAppGrant
- E 档补发

## anchors
- symbols: `OpenAppGrantCouponService`（`tryFirstLoanErankOpenAppGrant` 一类 E 支路）
- cache: 全局 CUT_INTEREST / open-app 时间窗

## constraints
- 热部署 `ec-core` 必须绑到本需求 kafka/api 端口；绑错泳道会出现「改了代码仍不发」
- 排查不发先看是否被全局 cache/`else return` 短路，再查实验组名

## evidence
- Cursor×ec-01 · transcript `1567e930`

## status
active

## updated
2026-09-28
