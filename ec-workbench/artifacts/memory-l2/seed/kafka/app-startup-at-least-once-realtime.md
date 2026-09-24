# APP_STARTUP_AT_LEAST_ONCE：实时链路不是零点批任务

## decision
代码里**不存在**「每天 00:00 cron 批量扫用户」的 Job。
`APP_STARTUP_AT_LEAST_ONCE` 是 **C 端实时行为 → outbox/Kafka → 异步消费**；午夜看起来像批处理，是因为午夜 C 端流量高峰。
入口包括 `openApp` / 首页 `asyncLogUserInfo` / `app/startup` → `AppStartupService.sendAppStartupEvent` → topic `app_startup_at_least_once_event` → `AppStartupAtLeastOnceEventProcessor`。

## mouths
- kafka / openApp
- homepage / 实时事件

## anchors
- symbols: `AppStartupService`, `AppStartupAtLeastOnceEventProcessor`, `RealTimeEventController#openApp`, `LoanHomePageLogService#asyncLogUserInfo`
- topic: `app_startup_at_least_once_event`
- event: `APP_STARTUP_AT_LEAST_ONCE`

## constraints
- 不要按「零点定时批」去排障放大/突增
- 先对触发入口与消费放大，再怀疑积压回放

## evidence
- Cursor×ec · transcript `75ad7c10`

## status
active

## updated
2026-09-24
