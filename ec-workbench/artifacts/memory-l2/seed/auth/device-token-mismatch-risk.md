# 登录：deviceToken 不一致 → 风险设备

## decision
当前登录的 `deviceToken` 与 LoginStatusCache 里「上一条」记录不一致时，**会**被视作风险设备；在配置与登录方式满足时会触发安全校验（如登录 OTP）。
「上一条」不是简单按时间前一条，而是 `LoginStatusCacheModel.findLastNoSecondValidByUserIdNotInDeviceList` 一类规则选出的记录（需结合 `AFTER_SECOND_VALID_STATUSES` / INVALID / EXPIRED 等状态）。

## mouths
- auth / 登录
- risk-device

## anchors
- symbols: `UserSecureService.isRiskDevice`, `LoginStatusCacheModel.findLastNoSecondValidByUserIdNotInDeviceList`
- field: `deviceToken`

## constraints
- 排障「为何弹 OTP」先对比当前 token 与 cache 上一条
- 不要把「上一条」理解成时间序前一条

## evidence
- Cursor×ec · transcript `9f6976b2`

## status
active

## updated
2026-09-24
