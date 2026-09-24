# 开屏灌券：v1 死枝 vs v2 Kafka 主路径

## decision
`RELOAN_TOP_*_v1` / 常量 `RELOAN_TOP_LOW_WILL_COUPON`：**发放链路已不在**（常量还能搜到、callers 空，或只剩 ABTestConfig 版本表）。  
**活着的是 v2** `RELOAN_TOP_WILLING_COUPON_V2`：入口是 Kafka `AppStartUpGrantCouponProcessor` → `grantCutInterestCouponForOpenApp` → 分流 `ExpDiversionClient.getResult`。  
C 端 openApp HTTP 只投递事件。另：817 氛围走 `IExperimentAdapter`，灌券走 `ExpDiversionClient`——查图不能只认一种分流。

## mouths
- kafka
- ec-api

## anchors
- symbols:
  - `RELOAN_TOP_LOW_WILL_COUPON`（死发放）
  - `RELOAN_TOP_WILLING_COUPON_V2`
  - `AppStartUpGrantCouponProcessor`
  - `ExpDiversionClient`
- 相关：见 `ec/coupon/open-app-grant.md`

## constraints
- 不要把 v1 常量当还在发券
- 不要把定时 `CouponTaskGrantJob` 当成开屏灌券入口
- 枚举 `.getKey()` 在图上 callers 常空——要用方法链确认

## evidence
- Cursor×ec 探路 2026-08-31 · transcript `19835a78-4101-4c44-887a-5b8a41d14957`

## status
active

## updated
2026-09-23
