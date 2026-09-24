# JOOQ：traceId=null 仍绑定 WHERE 会 EMPTY_BIND

## decision
`findByTraceIdAndType(null, …)` 时若仍 `WHERE trace_id = ?` 且 bind null → `EMPTY_BIND_VALUE` 告警。  
不能只改 `RiskOutputService` 一处：大量代码**直调 Model** 绕过包装。

## mouths
- ec-api
- ec-internal-api

## anchors
- symbols:
  - `RiskOutputService.findByTraceIdAndType`（统一入口守卫）
  - `CashLoanCreditsService.insertOrUpdateRiskOutputResult`（直调 Model）
  - `LoanUserRiskTraceService`（直调 Model）
- routes 样本：`POST /api/v2/loan/reapply`（traceId=null 可复现）

## constraints
- 修 EMPTY_BIND：Service 源头守卫 + 扫直调 Model 热点，不要只改包装层
- 曾撤销「只在 Model 判空」的做法——以 Service 守卫为准

## evidence
- Cursor×ec Case 6 · transcript `b0caa16c-8535-404b-8b93-2c4c96a98233`

## status
active

## updated
2026-09-23
