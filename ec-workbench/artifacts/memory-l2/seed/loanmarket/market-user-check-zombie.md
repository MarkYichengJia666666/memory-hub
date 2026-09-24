# MarketUserCheck：已下线（历史僵尸判决）

## decision
历史判决：`/api/market/user/check` 生产零业务流量，可回滚下线。
**现码复核**：`MarketUserController` / `MarketUserCheckService` 已不在仓；仅残留 `MarketUserQueryLogModel` 等痕迹。判决方向正确，状态改为已完成/过时。

## mouths
- loanmarket / 下线

## anchors
- removed: `MarketUserController`, `MarketUserCheckService`, `/api/market/user/check`
- residual: `MarketUserQueryLogModel`

## constraints
- 不要再当「待删僵尸」跟踪

## evidence
- Cursor×ec · transcript `371a25ea`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
superseded

## updated
2026-09-24
