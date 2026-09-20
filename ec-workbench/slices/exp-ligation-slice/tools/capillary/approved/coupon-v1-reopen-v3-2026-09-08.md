# Capillary 删除计划 · coupon-v1-reopen-v3

- 平台 key：`reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1_reopen_v3`
- grade：`candidate_bury`
- safe_auto：False

## 为什么删

- 平台状态=ZERO_PERCENTAGE
- 流量：对照 0.0% / 实验 0.0% / 空白 100.0%
- 剩余约 0.0 天
- 描述：复贷头部灌券-意愿模型_重开_v3
- 关量/结束且对用户不生效，图上能定位到符号 → 可进删除候选（需人确认）
- 生产 reopen_v3 已关量；代码搜不到 reopen 名，只剩 v1 常量/版本表

## 涉及符号

- `RELOAN_TOP_LOW_WILL_COUPON`
- `reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1`

## 操作要求

- 只删实验枝 / 废弃常量，不要动全量后的主业务路径。
- 首页枚举分发、Kafka 反射拉起：必须人工核。
- 改完跑相关单测，再 arc diff；合入前 Code Review。
