# 续借测额：NORMAL 不按时长自动过期

## decision
在 `CashLoanCalcCreditsService#getCalcCreditsVOForMultiLoan` 链路上，注释写明「判断额度过期，目前仅回捞等级可能过期」。只要用户组未到期（NORMAL 常见 `expire_time=-1`），会走「未过期」分支返回不需要测额语义，**没有**按小时配置的自动失效。
勿与还款侧 `multi_loan.repayment_multi_credits_expired_hours`（能否自动提交续借测额）混淆。

## mouths
- risk / 续借
- multi-loan / 首页额度

## anchors
- symbols: `CashLoanCalcCreditsService#getCalcCreditsVOForMultiLoan`, `riskUserGroupEntranceService#checkUserGroupExpireByAccountId`
- status: `CALC_CREDITS_NOT_NEEDED` / `CALC_CREDITS_EXPIRED`
- related config: `multi_loan.repayment_multi_credits_expired_hours`（另一条链）

## constraints
- 问「NORMAL 会不会到点失效」先看 user_group.expire_time，不要先搜小时配置
- 还款自动戳额配置 ≠ 首页额度展示过期

## evidence
- Cursor×ec · transcript `ed671c6f`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
