# 次新第二单发券：前置 isHoldoutControl 后，防结清仍须 passesHoldout

## decision
`ReloanSecondOrderDiscountCouponService#tryGrantIfBetter`（及同类次新回端发券）在命中本期实验组后：非防结清走只读 `isHoldoutControl`；**防结清人群仍必须后置 `passesHoldout` 写入 13830**。仓规 `anti-settlement-long-term-exp.mdc` / `AGENTS.md` R6：**漏接后置 = 缺陷**（样本/口径失真）。
曾有 commit 声称「后置不变」却删掉 `passesHoldout` 只留双段 `isHoldoutControl`——评审/回滚时以代码为准，补回后置并写清「修复遗漏后置入组」，避免再被当无用代码删掉。

## workspace
- `TAPD-377551-reloan-discount-crowd`
- `TAPD-374680-reloan-discount-coupon`

## mouths
- 次新第二单 / tryGrantIfBetter
- 防结清 holdout

## anchors
- symbols: `ReloanSecondOrderDiscountCouponService#tryGrantIfBetter`, `AntiSettlementLongTermExpService#passesHoldout`, `AntiSettlementLongTermExpService#isHoldoutControl`
- rules: `.cursor/rules/anti-settlement-long-term-exp.mdc`

## constraints
- 勿用「全量前置只读」替换掉防结清写路径
- 改 holdout 接线后跑 architecture / R6 检查

## evidence
- Claude×ec · session `c74b5a96`

## status
active

## updated
2026-09-28
