# 撞库升级 fallback：旧方法名已不存在

## decision
历史判决：关键方法 `CollisionOrchestrator.executeUpgradeWithFallback`，升级失败有 fallback。
**现码复核**：`CollisionOrchestrator` 仅保留 `execute()`，统一走决策树 `COLLISION_DECISION_TREE`；**无** `executeUpgradeWithFallback`。空结果兜底在 `CollisionProcessService`（fallback `REJECT`）。本条 superseded，请用新判决。

## mouths
- apichannel / collision

## anchors
- current: `CollisionOrchestrator#execute`, `CollisionProcessService#execute`
- removed: `executeUpgradeWithFallback`

## constraints
- 不要再搜 executeUpgradeWithFallback

## evidence
- Cursor×ec · transcript `6e437239`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
superseded

## updated
2026-09-24
