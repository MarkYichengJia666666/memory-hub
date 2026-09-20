# exp-ligation slice — key to Java symbol

图只认符号名，不认平台 key 字符串。查图前先换成 Java 名字。**活不活问生产实验 MCP**（`ExperimentController_get`，`name`=平台 key，`needReopen=true`），不问这张表的旧备注。

判定：只有实验组流量 > 0 才算对用户生效。`ZERO_PERCENTAGE`、或状态还亮着但空白组 100%、实验组 0%，都是线上不进实验组。

## 对照 + 2026-08-31 生产快照

| 平台 key | Java 符号 | MCP 状态 | 流量 | 对用户生效 |
|---|---|---|---|---|
| first_loan_product_26h1-lending-abroad-loan-Rayakan_Kemerdekaan | NATIONAL_DAY_817_FIRST_LOAN | ZERO_PERCENTAGE，剩余约 76 天 | 空白 100%，实验组 0% | 否 |
| reloan_order_26h1-lending-abroad-loan_all-Rayakan_Kemerdekaan | NATIONAL_DAY_817_RELOAN | ZERO_PERCENTAGE，剩余约 76 天 | 空白 100%，实验组 0% | 否 |
| reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1 | RELOAN_TOP_LOW_WILL_COUPON | LIGHT，剩余 0 | 空白 100%；重开 v3 为 ZERO_PERCENTAGE 剩余 0 | 否 |
| reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v2 | RELOAN_TOP_WILLING_COUPON_V2 | LIGHT，剩余 0 | 空白 100% | 否 |

v2 代码写的是上面这个 **v2 原名**。平台最新重开叫 `26h2_11_reloan_mkt-lending-abroad-renew-top_willing_coupon_v2_reopen_v6`（ZERO_PERCENTAGE，剩余约 63 天，实验组仍 0%）。EC Java 里搜不到 reopen_v6 这个名字。

两条 817 key 都进同一个方法：`NationalDay817ExpService.isAtmosphereGroup` → `IExperimentAdapter.abTestByUserId` → `ExperimentAdapter.abTestByUserId`。

首页 banner 入口不在 ec-api，在 `POST /ecInternalApi/checkStrategy`（`StrategyCheckController`）。中间经 Aviator `rule()` 动态调 `GeneralPageConfigFilterRuleService.hitRule`，再按枚举 `ND817HB` 找到 `NationalDay817HomeBannerFilterRuleProcessor`。这截不是普通方法调用边。

资源位 / 开屏发券没有 `*Job`。统一发券任务的 Job 是 `CouponTaskGrantJob`，入口按服务补，见 `PLAYBOOK.md`，不按这张 key 表筛文件。

`RELOAN_TOP_LOW_WILL_COUPON`（v1）已 `@Deprecated`，生产发放改走 `RELOAN_TOP_WILLING_COUPON_V2`。v1 在代码里只剩 `ABTestConfig` 版本表。v2 入口是 Kafka `AppStartUpGrantCouponProcessor`，分流是 `ExpDiversionClient.getResult`，不是 `IExperimentAdapter`。
