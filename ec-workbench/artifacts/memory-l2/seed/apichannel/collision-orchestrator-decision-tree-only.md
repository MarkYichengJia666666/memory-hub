# 撞库编排：仅决策树 execute

## decision
现码 `CollisionOrchestrator` **只有** `execute()`：设 `upgradeFlow=true` 后统一跑 `CollisionProcessEnum.COLLISION_DECISION_TREE`。
无节点命中时 `CollisionProcessService` 兜底 `REJECT`。不要再找 `executeUpgradeWithFallback`。

## mouths
- apichannel / 撞库

## anchors
- symbols: `CollisionOrchestrator#execute`, `CollisionProcessService#execute`, `CollisionProcessEnum.COLLISION_DECISION_TREE`
- fallback: `ApiChannelUserType.REJECT`

## constraints
- 改撞库看决策树节点与 ProcessManage，不看已删除的 upgrade-with-fallback API

## evidence
- 承接旧 L2 collision-upgrade-fallback
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
