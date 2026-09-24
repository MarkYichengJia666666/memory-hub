# api-channel：context.channel 不可靠

## decision
在 `CommonApiChannelService` 一类链路里，`context.channel` **基本不可靠，甚至常为空**。
更稳妥顺序：先用 `loanAccountDetailsNew.channel`；为空/查不到再用 `ChannelUtils.getChannel(context.sourceType, context.channel, context.sdkType)` 做 fallback。
不要默认「context 里一定有当前渠道」。

## mouths
- api-channel

## anchors
- symbols: `CommonApiChannelService`, `ChannelUtils#getChannel`, `ApiChannelCreditController`
- fields: `loanAccountDetailsNew.channel`, `context.sourceType`, `context.sdkType`

## constraints
- 与端内 `viewerContext.channel` 场景区分：端内请求上下文可信；本链路不可照搬
- 写渠道相关逻辑前先确认读的是哪一层 channel

## evidence
- Cursor×ec · transcript `0c7d576c`

## status
active

## updated
2026-09-24
