# 资源位策略耗时：空 payload 规则会跑满用户级资格

## decision
Grafana「资源位策略耗时」长尾常见于少数 strategy：若规则 `payload` 为 `{}`，则**无轻量前置短路**，每次校验都跑完整用户级资格（如 strategyId 1165/1175 一类）。治理：读策略配置/打 `strategyCostLog` 点名慢规则；缓存慢规则结果、拆出关键路径、或隔离慢策略线程池。若误开 `GENERAL_RULE_BY_DB`，每条规则多一次 DB，会整体拖慢。

## workspace
- （资源位性能横切）

## mouths
- 资源位 / checkStrategy
- 策略耗时

## anchors
- metrics: 策略耗时 / `strategyCostLog`
- config: `GENERAL_RULE_BY_DB`
- pattern: empty `payload` → full eligibility

## constraints
- 先用数据点名 strategyId，再下钻规则处理器
- 空 payload「全能规则」勿堆在热路径无隔离

## evidence
- Claude×ec · session `26138b86`

## status
active

## updated
2026-09-28
