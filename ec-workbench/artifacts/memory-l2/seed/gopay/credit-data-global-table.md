# GoPay index 授信：读的是全局授信表

## decision
GoPay / api-channel index 授信查询里的 `getCreditData`，读的是 EC **全局**授信表 `loan_user_credits_info`，**不是** GOPAY 渠道专属表。
因此「授信 ACCEPTED → 下单页」分支反映的是全局授信状态，写方案时不要假设渠道隔离额度视图。

## mouths
- api-channel / gopay
- credit

## anchors
- symbols: `getCreditData`
- table: `loan_user_credits_info`
- related: `reloanStatus`

## constraints
- 做 GOPAY 续借/复贷方案时，先 reality-check 授信读的是哪张表
- 不要把渠道专属额度与全局 `loan_user_credits_info` 混为一谈

## evidence
- Cursor×ec · transcript `6b78a5f5`

## status
active

## updated
2026-09-24
