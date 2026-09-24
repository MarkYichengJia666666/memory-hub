# 开屏灌券：发券在 Kafka，不在 openApp Controller

## decision
C 端 `openApp` 只投递实时事件；真正发券在 Kafka Consumer（`AppStartUpGrantCouponProcessor`）里完成。改发券逻辑不要只改 Controller。

## mouths
- ec-api
- kafka

## anchors
- routes: `POST /api/realTimeEvent/openApp`
- symbols:
  - `AppStartupService.processAppStartupEvent`
  - `AppStartUpGrantCouponProcessor.processRecord`
  - `OpenAppGrantCouponService`（发券编排；含实验分流）
- topics: 开屏/启动相关实时事件 topic（以配置为准；入口是 openApp 投递）

## constraints
- 不要只改 Controller 就当发券改完
- 实验枝（如 `RELOAN_TOP_WILLING_COUPON_V2`）仍在发券主链 `getResult`；关量 ≠ 代码已死，埋点前要人审
- 一仓一图：结构探路用 `cg-zp42c34n`；本条只管判决，不替代图

## evidence
- Capillary / 切片探路（2026-09）：开屏灌券主路径 Kafka；v1 Deprecated、v2 仍在主链但平台长期 0% 实验组
- approved 参考：`coupon-v1-2026-09-08.md`

## status
active

## updated
2026-09-23
