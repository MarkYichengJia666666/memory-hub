# 下游还款同步配置：改完要重启

## decision
`DownstreamOrderRepaySyncConfig` 里 `enabled` / `whitelistMode` / `whitelistUserIdsRaw` / `receiveUrl` 等是 **`@Value` 只注入一次**；白名单也在 `@PostConstruct` 启动时解析。
按当前实现，**改配置后必须重启服务才会生效**，不是热更新。

## mouths
- downstream / repay-sync
- config

## anchors
- symbols: `DownstreamOrderRepaySyncConfig`
- fields: `enabled`, `whitelistMode`, `whitelistUserIdsRaw`, `receiveUrl`

## constraints
- 线上改白名单/开关后要排重启
- 不要假设配置中心推送后进程内立即生效

## evidence
- Cursor×ec · transcript `80274d26`

## status
active

## updated
2026-09-24
