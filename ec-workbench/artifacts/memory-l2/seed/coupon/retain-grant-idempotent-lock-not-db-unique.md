# 挽留/回端发券幂等：分布式锁 + existing 短路，不靠 DB 唯一约束

## decision
`LoanUserCouponService#grantCouponOrGetExist`（及同类回端/挽留发券）幂等口径是 **`userId + ruleConfigId` 分布式锁 + 已存在券直接返回 existing**，**不依赖** DB 唯一约束抛 `DuplicateKeyException`。callback 侧无需再 catch 唯一键冲突当主路径。文档/验收矩阵若仍写「DB 唯一约束幂等」视为过期，以 plan Decision 与实现为准。

## workspace
- `TAPD-365113-reloan-retain-pop-v2`

## mouths
- 发券幂等 / grantCouponOrGetExist
- 挽留弹窗发券

## anchors
- symbols: `LoanUserCouponService#grantCouponOrGetExist`
- anti-pattern: callback `catch DuplicateKeyException` 当唯一幂等

## constraints
- 勿在 NFR/矩阵把「锁+existing」改回「唯一约束」而不改代码
- 并发双发优先查锁 key 与 existing 短路是否生效

## evidence
- Cursor×ec · transcript `9d27eb51`

## status
active

## updated
2026-09-28
