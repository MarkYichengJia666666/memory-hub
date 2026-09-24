# CollisionProcessService：是新撞库流程

## decision
`CollisionProcessService#execute` **不是**「老流程」。它是撞库**新**流程执行器：把条件交给 `process-manage`（`ProcessManageService#getNextProcessNode`），按 `CollisionProcessEnum` 的 code/version 跑节点，再汇总 `ApiChannelUserType`；节点无结果时兜底 `REJECT`。
排障/改规则时不要把它当成旧 `@Deprecated` 链式 checker 列表。

## mouths
- apichannel / 撞库
- process-manage

## anchors
- symbols: `CollisionProcessService#execute`, `ProcessManageService#getNextProcessNode`, `CollisionProcessEnum`, `ApiChannelUserType`
- context: `CollisionProcessContext`, `CollisionNodeContext`

## constraints
- 新流程改配置/节点，不要混到 deprecated 旧规则链
- 无节点结果 = REJECT，不是「放行」

## evidence
- Cursor×ec · transcript `fcc24824`

## status
active

## updated
2026-09-24
