# 复贷意愿分：落库与 mock

## decision
意愿分在 EC **有落库**，可 mock。  
链路：risk 输出 → `risk_output_result`（`type = RELOAN_FEE_WILL_LEVEL_V4`，值在 `new_value`）→ `LoanUserRiskTraceService.getFeeWillLevelV4` → `HomeInterestFreeCardService.isLowFeeWillLevel`。

## mouths
- risk
- homepage

## anchors
- table: `risk_output_result`
- type: `RELOAN_FEE_WILL_LEVEL_V4`
- symbols:
  - `LoanUserRiskTraceService.getFeeWillLevelV4`
  - `HomeInterestFreeCardService.isLowFeeWillLevel`

## constraints
- 自测可直接改测试库对应 `loan_account_id` 的 `new_value`（如 `"1"`/`"2"`）再打首页
- 查日志服务名注意是 `indo-cashloan-ec-api`（勿误用空的 eu 库名）

## evidence
- Cursor×ec-1 · transcript `43e21908-83ba-4ff8-b453-09672c85eea2`

## status
active

## updated
2026-09-23
