# Lazada：首贷长拒可重审只放开到二次撞库

## decision
用户类型 `FIRST_LOAN_REJECT_LONG_CAN_RETRY`（决策树叶子：首贷被拒 180+ 天且可重审）在现码中：
- `LazadaPreSelectChecker` / 二次前筛侧：可映射 **`LAZADA_APPROVE`**，分组 **`LAPSED_USER`**
- `LazadaCreateOrderChecker`（下单撞库）：仍映射 **`LAZADA_WITHIN_REJECTION_PERIOD`（拒）**
即：允许重回授信审批，**未过审不能直接借款**。改规则时按 Checker 阶段改映射，勿只改一处。

## mouths
- lazada / 撞库
- 重审

## anchors
- type: `ApiChannelUserType.FIRST_LOAN_REJECT_LONG_CAN_RETRY`
- symbols: `LazadaPreSelectChecker`, `LazadaSecondPreSelectChecker`, `LazadaCreateOrderChecker`
- node: `BRANCH_FIRST_REJECT_180D` 相关叶子

## constraints
- 前筛通过 ≠ 可下单
- CreateOrder 映射不要随便跟 PreSelect 一起放

## evidence
- Cursor×ec · transcript `cc17f45a`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
