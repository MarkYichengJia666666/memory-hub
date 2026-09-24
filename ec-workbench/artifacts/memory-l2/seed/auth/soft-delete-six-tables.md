# 注销：软删除，不是物理删库

## decision
`UserDeleteService.doDeleteUser` **不做物理清理**，主要改状态位：用户 `DELETED`、联合视图标记、写 `user_deleted_detail`、失效 token/生物认证、借贷账户标记删除等（约 6 张表级操作）。完件用户关联表可达上百张——物理删除是另一套工程，不要假设「注销=数据没了」。

## mouths
- auth / 注销
- 合规 / 降本

## anchors
- symbols: `UserDeleteService#doDeleteUser`
- tables: `user`, `union_loan_user_info_view`, `user_deleted_detail`, `login_status_cache`, `login_biometric_credential`, `loan_account`

## constraints
- 注销后订单/KYC 等仍可能挂在原 userId
- 物理清理必须单独设计，不能套用 doDeleteUser

## evidence
- Cursor×ec · transcript `8793003c`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
