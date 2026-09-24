# NoOrderAfterReloanCompleted：遗留 checker 已清理

## decision
历史判决曾说：`NoOrderAfterReloanCompletedChecker` 业务路径已死，但删类会因 `CollisionNodeBeanConfig` 注册失败。
**现码复核**：该类与 BeanConfig 注册路径已不存在；复贷「授信未下单」现走决策树节点 `reloan_credit_no_order`（`ReloanBranchDecisions#reloanCreditNoOrder`）+ 各渠道 `*Checker` 对 `NO_ORDER_AFTER_RELOAN_COMPLETED` 的转换。旧「不能只删 checker」约束已过时。

## mouths
- apichannel / 撞库
- 遗留 / 下线

## anchors
- symbols: `ReloanBranchDecisions#reloanCreditNoOrder`, `CollisionProcessNode.BRANCH_RELOAN_CREDIT_NO_ORDER`
- types: `ApiChannelUserType.NO_ORDER_AFTER_RELOAN_COMPLETED`
- removed: `NoOrderAfterReloanCompletedChecker`

## constraints
- 不要再按旧 checker 类名排障
- 改规则看决策树节点与渠道 Checker 转换

## evidence
- Cursor×ec · transcript `d76bc97f`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
superseded

## updated
2026-09-24
