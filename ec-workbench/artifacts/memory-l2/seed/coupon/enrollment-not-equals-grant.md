# 入组 ≠ 发券：次新回端还要过 crowd / holdout / 打款笔数

## decision
实验平台「入组」包含对照，且即便实验组也还要过发券硬门闸与 holdout/客群。排障「入了组却没券」先区分：对照、防结清 crowd 否决、`passesHoldout`/`isHoldoutControl`、根本未走到发券日志、以及产品「入组时间」与 EC 实时 `abTest` 是否同一口径。
硬门闸常用 **打款笔数 `COUNT(STATUS IN ('R','C')) = 1`**（次新第二单），**不要**误用 `loan_account.LOAN_TIMES`。可下单态常见锚点：`MULTI_LOAN_CREDITS_ACCEPTED` + `canOrder=true`。

## workspace
- `TAPD-374680-reloan-discount-coupon`
- `TAPD-377551-reloan-discount-crowd`

## mouths
- 复贷发券 / 次新
- 入组 vs 发券

## anchors
- symbols: `AntiSettlementLongTermExpService#passesHoldout`, `AntiSettlementLongTermExpService#isHoldoutControl`
- gate: 打款笔数 =1（非 `LOAN_TIMES`）

## constraints
- 勿把「实验组名单」直接当成应发券名单
- 样本不足时先核对照/实验组标签再谈补发

## evidence
- Cursor×ec · transcript `d20bf7f0`
- Cursor×ec · transcript `5ed170d3`

## status
active

## updated
2026-09-28
