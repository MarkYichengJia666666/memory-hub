# user_kyc_encrypt_info：状态表不是事件流

## decision
`user_kyc_encrypt_info` 有 `UNIQUE KEY (USER_ID)`，是**用户档案/状态表**（一用户一行，会 UPDATE），不是事件流表。
大表降本时：**事件流可按时间清理；状态表不能删**——原因与「可无限追加的日志表」完全不同。

## mouths
- db / 降本
- kyc / api-channel

## anchors
- table: `user_kyc_encrypt_info`
- key: `uindex__user_id` / `USER_ID`
- related services: `ApiChannelAuthService`, `AuthIdentityInfoService`

## constraints
- 降本 SOP 先判定「事件流 vs 状态表」再谈清理
- 不要把对事件表的删除方案套到 KYC 档案表

## evidence
- Cursor×ec · transcript `a1c8081e`

## status
active

## updated
2026-09-24
