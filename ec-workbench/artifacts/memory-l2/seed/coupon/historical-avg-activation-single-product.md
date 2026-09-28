# 百分比降息券「历史件均」启用金额：单产品取值 + 固定计算序

## decision
`CouponActivationAmountCalcType` 历史件均启用金额（百分比-降息券 Fixed 系）按序：**① 历史成功放款本金均值×系数 → ② 与可借额度上限取小 → ③ 按 10 万步长取整 → ④ 不低于产品最低借款金额**。历史放款订单与可借额度均限定在**该券所适用的单一借款产品**内，不是账户全局可借、不跨产品统计。
仓内另有 `AbstractAvgHistoricalCutInterestCouponGrantRule` / `AvgHistoricalLoanPrincipalRuleResolver` 工具族，服务另一类「历史件均降息券」产品——**不要**与本次枚举改动混为一谈。全额退款订单排除若无字段可识别，可记为已知限制/Out of Scope；已取消未成功放款天然不进样本。

## workspace
- `TAPD-377605-coupon-historical-avg-threshold`

## mouths
- 百分比降息券 / 启用金额
- 历史件均

## anchors
- enums: `CouponActivationAmountCalcType`
- related-but-separate: `AbstractAvgHistoricalCutInterestCouponGrantRule`, `AvgHistoricalLoanPrincipalRuleResolver`

## constraints
- 勿跨产品拉历史本金或全局剩余额度
- 勿打乱 ①→④ 顺序（上限与最低额冲突按 spec 已澄清优先级）

## evidence
- Claude×ec-01 · session `66965bd3`
- Claude×ec-01 · session `48db0c77`

## status
active

## updated
2026-09-28
