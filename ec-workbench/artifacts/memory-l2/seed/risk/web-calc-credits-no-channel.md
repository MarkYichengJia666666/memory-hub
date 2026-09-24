# WEB 测额：风控 trace 无细分 channel

## decision
WEB 下「戳额 / 额度过期重新测额」的风控 trace **没有 flip/ovo 等细分 channel**，只记到 `source_type = WEB`。
原因：入口 `LoanAccountService.submitCalcCreditsApplication` → `SubmitCalcCreditsApplicationVO` **无 channel 字段**，`RiskProcessParam.fromForCalcCredits()` 也就带不进细分渠道。
查渠道应看 `user.channel` / `loan_account_details_new.channel` 做归因推断，不要指望本次测额 risk_trace 精确到页面渠道。

## mouths
- risk / 测额
- WEB / H5

## anchors
- symbols: `LoanAccountController`, `LoanAccountService#submitCalcCreditsApplication`, `SubmitCalcCreditsApplicationVO`, `RiskProcessParam#fromForCalcCredits`
- table: `loan_user_risk_trace.source_type`
- attribution: `user.channel`, `loan_account_details_new.channel`

## constraints
- 不要用本次测额 risk_trace 回答「是不是 flip/ovo 点进来的」
- 细分渠道要另查注册/完件 channel

## evidence
- Cursor×ec · transcript `6056dda0`

## status
active

## updated
2026-09-24
