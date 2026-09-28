# 活体分流 SHADOW：低版本须保留降级 FACEPP

## decision
`living-diversion-compare` 在 `mode=SHADOW` 时对外仍返回旧源，`match=false` 在「渠道迁移」（如 UNAUTH→FACEPP_V5）场景下可以是预期，不是等价重构失败。切 `mode=ON` 前必须保证 Router V2 **复刻旧逻辑的低 build 降级到 FACEPP**；否则老客户端（如 build 35523）会被误导到 FACEPP_V5。优先看聚合监控 `LIVING_DIVERSION_COMPARE` 的 AUTH 场景 match，再全量 ON。

## workspace
- （活体分流 / provider 抽象相关；对话锚点 living-diversion-compare）

## mouths
- 活体分流 / FACEPP
- build 降级

## anchors
- logs: `[living-diversion-compare]`, `LIVING_DIVERSION_COMPARE`
- modes: `SHADOW` / `ON`
- filter: `filterByBuild`

## constraints
- SHADOW 下 match=false 先对照默认配置是否本就在迁渠道
- 切 ON 前修低版本降级，勿只看整体 match 率

## evidence
- Cursor×ec · transcript `0c641208`

## status
active

## updated
2026-09-28
