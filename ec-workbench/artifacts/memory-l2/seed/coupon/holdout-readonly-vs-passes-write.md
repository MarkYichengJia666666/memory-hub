# 发券 holdout：非防结清只读 isHoldoutControl，防结清写路径 passesHoldout

## decision
复贷发券接长期 holdout（如实验 13830）时要分人群，**不能**对所有人统一后置 `passesHoldout`：
- **非防结清**：`AntiSettlementLongTermExpService#isHoldoutControl`（只读）→ 对照不发、非对照可发，**不写入** holdout 实验样本；
- **防结清**：`#passesHoldout`（写路径）→ 对照不发；非对照发券（会入组）。
`passesHoldout` 在非防结清人群上会直接 false（人群门），与「只挡对照」不是同一语义。排查「实验组没券」要区分：被只读对照拦住 vs 被写路径对照拦住 vs 根本没走到发券。命中对照后建议保留 `skip holdout control` 类 debug 日志，否则线上无法区分「被 holdout 拦」和「没进这段逻辑」。

## workspace
- `TAPD-374680-reloan-discount-coupon`（与 EC `specs/<workspace>` 同名；相关 sod-holdout 分支）

## mouths
- 发券 / OpenApp / 复贷券
- holdout / 防结清

## anchors
- symbols: `AntiSettlementLongTermExpService#isHoldoutControl`, `AntiSettlementLongTermExpService#passesHoldout`, `UserGroupInfo#antiSettlementCrowd`

## constraints
- 勿对非防结清人群用 `passesHoldout` 冒充「只读挡对照」（会污染/改写 13830 样本或误杀未入组用户）
- 勿把「人群过滤不发」与「对照否决」混成一句排障结论

## evidence
- Cursor×ec · transcript `0dca7d3c`

## status
active

## updated
2026-09-28
