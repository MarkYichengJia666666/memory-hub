# 鉴权：@ECSecuredApi（不是 @SecureAPI）

## decision
项目里没有 `@SecureAPI`，实际闸门是 `@ECSecuredApi`。  
账号找回链路：登录前接口（发码/验码等）**不应乱加**登录注解；部分接口只认 `sessionToken` 不认 `userToken`。

## mouths
- ec-api

## anchors
- annotations: `@ECSecuredApi`
- routes（找回相关）:
  - `GET /api/generalConfig`（白名单可免登录）
  - `POST /api/account-recovery/send-phone-otp`
  - `POST /api/account-recovery/verify-phone`
  - `POST /api/account-recovery/match`（只认 sessionToken）

## constraints
- 不要搜/加 `@SecureAPI`
- 登录前步骤加 `@ECSecuredApi` 会断流程
- 改找回鉴权前先对整条链路（含复用现网接口）复核

## evidence
- Cursor×ec-1 · transcript `e326c978-79bd-4daf-9b51-026401ef1caa`

## status
active

## updated
2026-09-23
