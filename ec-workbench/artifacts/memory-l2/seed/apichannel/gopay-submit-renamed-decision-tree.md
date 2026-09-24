# GoPay 撞库：process code 已更名决策树

## decision
同一业务（如 `channel=GOPAY` + `checkerType=SUBMIT_APPLICATION`）的撞库主路径，process code 已从历史的 `gopay.submit_application` **更名为** `collision.decision_tree`（`CollisionProcessEnum.COLLISION_DECISION_TREE`），由 `CollisionOrchestrator#execute` 统一跑。
对比耗时/日志时按业务条件对齐，不要因 process 字符串变了就当成不同场景。

## mouths
- gopay / 撞库
- process-manage

## anchors
- process: `collision.decision_tree`（旧：`gopay.submit_application`）
- symbols: `CollisionOrchestrator#execute`, `CollisionProcessEnum.COLLISION_DECISION_TREE`

## constraints
- 搜旧 process 名可能搜不到现行日志
- 与「executeUpgradeWithFallback 已删除」配套理解

## evidence
- Cursor×ec · transcript `fcc24824`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
