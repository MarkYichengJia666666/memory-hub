# GoPay pollAuthStatus：事件类型映射缺失 → 1001

## decision
`POST /api/loanmarket/gopay/h5/pollAuthStatus` 返回 HTTP 200 但 `yqg-business-error-code: 1001` 时，常见根因不是接口本身，而是
`LoanUserEventService.initUserEventTypeMappingCache` **查不到**
`(sdkType, riskType, userType) → eventType` 映射后抛错。
已证实组合：`IDN_YQD` + `REAPPLY` + `RETRIEVAL_CALC_CREDITS_EXPIRE_USER`。属配置映射面问题，先补映射表再谈改代码。

## mouths
- gopay / loanmarket
- event-type / 映射

## anchors
- routes: `POST /api/loanmarket/gopay/h5/pollAuthStatus`
- symbols: `LoanUserEventService#initUserEventTypeMappingCache`
- tables: `fdm_ec_loan_user_event_type_relation`, `fdm_ec_loan_user_event_type`

## constraints
- 排障 1001 先核 sdkType/riskType/userType 三元组是否有映射
- 发布前应对关键组合做映射完整性校验

## evidence
- Cursor×ec · transcript `2ee22efb`

## status
active

## updated
2026-09-24
