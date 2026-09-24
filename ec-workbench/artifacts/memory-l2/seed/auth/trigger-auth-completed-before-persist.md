# 端内完件：triggerAuthCompleted 早于落库

## decision
端内链路里，`triggerAuthCompleted` **发生在完件落库之前**。
此时查 `loan_account_details_new.channel` 可能是旧值/空值/非本次请求语义。
对端内「当前这次请求」更贴近现场的是 `context.channel` / `viewerContext.channel`（从请求来），不要默认读主档 channel。

## mouths
- ec-api / 完件
- auth

## anchors
- symbols: `triggerAuthCompleted`, `viewerContext.channel`, `context.channel`
- table: `loan_account_details_new.channel`（落库后才可靠）

## constraints
- 完件过程中的渠道判断优先请求上下文，不要过早信主档
- 端外/异步链路另论，不要和端内混用同一假设

## evidence
- Cursor×ec · transcript `0c7d576c`

## status
active

## updated
2026-09-24
