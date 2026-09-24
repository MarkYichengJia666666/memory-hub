# 精准营销撞库失败：3 种文案原因

## decision
精准营销批量撞库（`channelBatchCheckExistUser` / `ApiMarketingUserCheckResultService`）失败 Case **不落库** `api_channel_check_mobile_result`（成功才 insert），统一打数仓日志 `check_exist_user_failed_log`。
失败原因是 **String**，不是 Java 枚举，代码里固定 **3** 种：`未命中人群`、`黑名单`、`已分案锁单`（后者当前主要是撞库频控上限；`setPhoneToLocked` 全仓无调用属预留）。

## mouths
- apichannel / 精准营销
- 撞库失败

## anchors
- symbols: `ApiMarketingUserCheckResultService`, `channelBatchCheckExistUser`, `ApiMarketingBlackListChecker`
- log: `LogBusinessType.CHECK_EXIST_USER_FAILED_LOG`, `CheckFailedUserVO.failureReason`
- reasons: `未命中人群`, `黑名单`, `已分案锁单`

## constraints
- 失败分析查数仓日志，不要只查 mobile_result 表
- 不要按枚举去搜 failureReason

## evidence
- Cursor×ec-1 · transcript `6e6dc097`

## status
active

## updated
2026-09-24
