# queryLoanUserRiskTrace：单账号高频，不是默认大批量 IN

## decision
`GET /ecInternalApi/loanUser/queryLoanUserRiskTrace` 会打到 `LoanUserRiskTraceModel.fetchByCondition`（`loan_account_id IN (...)`）。
现码/观测下，特征引擎上游（`overseas-feature-engine-api`）常见形态是**单 `loanAccountId` 高频**拉全量历史 trace（单响可达数十 KB），不要默认假设「一次 937 ID 大批量 IN」是日常形态；偶发大批量 SQL 仍可能同源链路，但需另证。另有 `queryLoanUserRiskTraceV2`（代码注释计划替换旧接口）。

## mouths
- risk / internal-api
- 慢查询 / 特征引擎

## anchors
- routes: `GET /ecInternalApi/loanUser/queryLoanUserRiskTrace`, `POST .../queryLoanUserRiskTraceV2`
- symbols: `LoanUserController`, `LoanUserRiskTraceModel#fetchByCondition`
- upstream: `overseas-feature-engine-api`

## constraints
- 优化前先分清「单账号大结果」vs「多 ID IN」
- 不要只盯 EC 慢 SQL 日志，也要看 internal-api 请求形态

## evidence
- Cursor×ec · transcript `badfbbdd`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
