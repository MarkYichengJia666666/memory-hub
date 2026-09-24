# jzdl mobile_result：用 CleanJob 滚动清理

## decision
`api_channel_check_mobile_result` 里精准营销（`api_channel LIKE 'jzdl_%'`）超保留期数据，**不要手写 SQL 删**；用已合入的 `ApiChannelCheckMobileResultCleanJob` 按批次删最安全。
业务上按 **6 个月**（`retentionDays=180`）删是安全的（读窗口不超过此）；长期跑用 `retentionDays` + `timeCreated=0` 滚动窗口，**不要**长期固定一个毫秒 cutoff。禁止无脑 7×24 大批次并行猛删。

## mouths
- apichannel / 精准营销
- scheduler / 降本

## anchors
- job: `ApiChannelCheckMobileResultCleanJob`
- table: `api_channel_check_mobile_result`
- filter: `api_channel LIKE 'jzdl_%'`
- params: `retentionDays`, `batchSize`, `sleepMillis`, `maxDeletePerExecution`, `startId`/`lastId`

## constraints
- 日常用滚动 retention，不用固定 timeCreated 毫秒戳
- 积压期控速（有上限、有 sleep、禁止并行）

## evidence
- Cursor×ec · transcript `8f62e33a`

## status
active

## updated
2026-09-24
