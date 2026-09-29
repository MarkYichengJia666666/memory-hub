# 人审包 · 开屏灌券实验枝（2026-09-23）

> 扫描：`capillary-20260923-124222` · 图 `cg-zp42c34n` · MCP 已刷新 · **未改 EC**

## 请你拍板

| vessel | 建议 | 请你选 |
|---|---|---|
| `coupon-v1` | **candidate_bury**（LIGHT，空白 100%） | 批准可埋 / 暂缓 |
| `coupon-v1-reopen-v3` | **candidate_bury**（ZERO_PERCENTAGE） | 批准可埋 / 暂缓 |
| `coupon-v2` | **manual_only**（never_bury 主路径） | 只观察，不进可埋 |

批准后我会写 L2：
- `ec/coupon/bury-coupon-v1.md`（及 reopen，若你也批）
- `ec/coupon/observe-coupon-v2.md`
- 并在 `open-app-grant.md` 加指针

**今天不** `--apply`、不切 EC 分支、不 arc。

---

## 扫描摘录

### `coupon-v1` · 开屏灌券 Kafka

- 平台 key：`reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1`
- Java 符号：`RELOAN_TOP_LOW_WILL_COUPON`, `grantCutInterestCouponForOpenApp`
- 图：`cg-zp42c34n`
- 备注：v1 已 Deprecated，发放走 v2；删常量前要对 ABTestConfig
- **为什么：**
  - 平台状态=LIGHT
  - 流量：对照 0.0% / 实验 0.0% / 空白 100.0%
  - 剩余约 0.0 天
  - 描述：复贷头部灌券-意愿模型
  - 关量/结束且对用户不生效，图上能定位到符号 → 可进删除候选（需人确认）
  - v1 已 Deprecated，发放走 v2；删常量前要对 ABTestConfig
- 图查询摘要：
  - `search:RELOAN_TOP_LOW_WILL_COUPON`：**Search Results (1 found)**  **RELOAN_TOP_LOW_WILL_COUPON** (constant) ec-common/src/main/java/com/yqg/ec/common/constant/ExperimentKeyConstants.java:55 `String RELOAN_TOP_LOW_WILL_COUPON`
  - `callers:RELOAN_TOP_LOW_WILL_COUPON`：No callers found for "RELOAN_TOP_LOW_WILL_COUPON"
  - `search:grantCutInterestCouponForOpenApp`：**Search Results (1 found)**  **grantCutInterestCouponForOpenApp** (method) ec-core/src/main/java/com/yqg/core/service/loan/coupon/openappgrantcoupon/OpenAppGrantCouponService.java:538 `void (AppStartupAtLeastOnceVO even
  - `callers:grantCutInterestCouponForOpenApp`：**Callers of grantCutInterestCouponForOpenApp (1 found)**  - processRecord (method) - ec-core/src/main/java/com/yqg/core/service/kafka/consumer/processor/AppStartUpGrantCouponProcessor.java:53
- 下一步：确认后执行 `python3 propose_diff.py --report capillary-20260923-124222.md --vessel-id coupon-v1 --dry-run`

### `coupon-v1-reopen-v3` · 开屏灌券 Kafka

- 平台 key：`reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1_reopen_v3`
- Java 符号：`RELOAN_TOP_LOW_WILL_COUPON`, `reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1`
- 图：`cg-zp42c34n`
- 备注：生产 reopen_v3 已关量；代码搜不到 reopen 名，只剩 v1 常量/版本表
- **为什么：**
  - 平台状态=ZERO_PERCENTAGE
  - 流量：对照 0.0% / 实验 0.0% / 空白 100.0%
  - 剩余约 0.0 天
  - 描述：复贷头部灌券-意愿模型_重开_v3
  - 关量/结束且对用户不生效，图上能定位到符号 → 可进删除候选（需人确认）
  - 生产 reopen_v3 已关量；代码搜不到 reopen 名，只剩 v1 常量/版本表
- 图查询摘要：
  - `search:RELOAN_TOP_LOW_WILL_COUPON`：**Search Results (1 found)**  **RELOAN_TOP_LOW_WILL_COUPON** (constant) ec-common/src/main/java/com/yqg/ec/common/constant/ExperimentKeyConstants.java:55 `String RELOAN_TOP_LOW_WILL_COUPON`
  - `callers:RELOAN_TOP_LOW_WILL_COUPON`：No callers found for "RELOAN_TOP_LOW_WILL_COUPON"
  - `search:reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1`：No results found for "reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1"
  - `callers:reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1`：Symbol "reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1" not found in the codebase
- 下一步：确认后执行 `python3 propose_diff.py --report capillary-20260923-124222.md --vessel-id coupon-v1-reopen-v3 --dry-run`

## 只能人工看

### `coupon-v2` · 开屏灌券 Kafka

- 平台 key：`reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v2`
- Java 符号：`RELOAN_TOP_WILLING_COUPON_V2`, `AppStartUpGrantCouponProcessor`, `grantCutInterestCouponForOpenApp`
- 图：`cg-zp42c34n`
- 备注：Kafka 灌券主路径；never_bury，只观察
- **为什么：**
  - 平台状态=LIGHT
  - 流量：对照 0.0% / 实验 0.0% / 空白 100.0%
  - 剩余约 0.0 天
  - 描述：复贷头部低意愿灌券_久期V2
  - 关量/结束且对用户不生效，图上能定位到符号 → 可进删除候选（需人确认）
  - 配置了 never_bury（主路径保护）→ 降为只能人工看，不进自动可埋
  - Kafka 灌券主路径；never_bury，只观察
- 图查询摘要：
  - `search:RELOAN_TOP_WILLING_COUPON_V2`：**Search Results (2 found)**  **RELOAN_TOP_WILLING_COUPON_V2** (enum_member) ec-core/src/main/java/com/yqg/core/common/enums/UserFlowExperimentEnum.java:69  **resolveReloanHeadCutInterestCouponIds** (method) ec-core/src/
  - `callers:RELOAN_TOP_WILLING_COUPON_V2`：No callers found for "RELOAN_TOP_WILLING_COUPON_V2"
  - `search:AppStartUpGrantCouponProcessor`：**Search Results (5 found)**  **AppStartUpGrantCouponProcessor** (class) ec-core/src/main/java/com/yqg/core/service/kafka/consumer/processor/AppStartUpGrantCouponProcessor.java:29  **AppStartUpGrantCouponProcessor.java**
  - `callers:AppStartUpGrantCouponProcessor`：**Callers of AppStartUpGrantCouponProcessor — 2 distinct definitions (narrow with `file`)**  **com.yqg.core.service.kafka.consumer.processor::AppStartUpGrantCouponProcessor** (class) — ec-core/src/main/java/com/yqg/core/
  - `search:grantCutInterestCouponForOpenApp`：**Search Results (1 found)**  **grantCutInterestCouponForOpenApp** (method) ec-core/src/main/java/com/yqg/core/service/loan/coupon/openappgrantcoupon/OpenAppGrantCouponService.java:538 `void (AppStartupAtLeastOnceVO even
  - `callers:grantCutInterestCouponForOpenApp`：**Callers of grantCutInterestCouponForOpenApp (1 found)**  - processRecord (method) - ec-core/src/main/java/com/yqg/core/service/kafka/consumer/processor/AppStartUpGrantCouponProcessor.java:53

## 未知

