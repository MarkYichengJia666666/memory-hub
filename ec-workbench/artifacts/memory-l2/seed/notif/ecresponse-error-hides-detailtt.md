# EcResponse：ERROR 级不吐 detailTT

## decision
`EcResponse.errStatus` **只看异常枚举自身的 `logLevel`**：`ERROR` 级一律换成通用 `FRONT_END_TOAST_GENERAL_ERROR_MESSAGE`，丢弃 `detailTT`；与调用处 `warn()` / `error()` 无关。
例：`LOAN_ACCOUNT_NOT_FOUND(80001, ERROR)` 错误码对，但业务文案不会给前端。要透出文案须改枚举 logLevel 或换非 ERROR 类型（影响面要评估）。

## mouths
- notif / 前端文案
- 异常

## anchors
- symbols: `EcResponse#errStatus`, `EcExceptionLogLevel.ERROR`, `detailTT`
- example: `LOAN_ACCOUNT_NOT_FOUND` / `80001`

## constraints
- 「有没有 toast」先看 logLevel，不要只看抛异常方式
- 改 logLevel 影响该枚举所有调用方

## evidence
- Cursor×ec-1 · transcript `e326c978`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
