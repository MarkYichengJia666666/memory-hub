# 找回/发码：注销校验复用端内同一套

## decision
账号找回发 OTP 等已接到端内同一套注销校验，**没有抄一份新逻辑**。  
复用：`UserService.checkIdnDeletedUserRegister`（端内 REGISTER / REGISTER_OR_LOGIN 发 OTP 的 migrateCheck 最终也是它）。

## mouths
- ec-api

## anchors
- symbols:
  - `UserService.checkIdnDeletedUserRegister`
  - `AccountRecoveryController#sendPhoneOtp`
  - `AccountRecoveryService#assertReusableShellUserOrThrow`
  - `UserController#accountRecoveryVerifyPhone`

## constraints
- 不要新建平行注销校验
- 发码与验码都要拦，避免只拦发码
- 已注销但允许迁回：找回新号仍不能当空号；真正迁回走端内注册

## evidence
- Cursor×ec-1 · transcript `e326c978-79bd-4daf-9b51-026401ef1caa`

## status
active

## updated
2026-09-23
