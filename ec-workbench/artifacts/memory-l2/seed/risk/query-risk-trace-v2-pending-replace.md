# queryLoanUserRiskTrace：V2 已在，旧 GET 待替换

## decision
`LoanUserController` 上旧 `GET /queryLoanUserRiskTrace` 旁有注释：V2 测完后将删除；并排提供 `POST /queryLoanUserRiskTraceV2`。
排障/优化时确认调用方走的是哪个版本；新接入优先 V2，不要再扩旧 GET 用法。

## mouths
- risk / internal-api

## anchors
- routes: `/ecInternalApi/loanUser/queryLoanUserRiskTrace`, `/queryLoanUserRiskTraceV2`
- symbols: `LoanUserController`

## constraints
- 旧接口仍可能被特征引擎高频打——下线前先切流量

## evidence
- 现码注释 TODO(mario)
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
