# API_CHANNEL_USER_REJECTED：Gopay 已映射 1001

## decision
`API_CHANNEL_USER_REJECTED` 对前端应映射为 `COMMON_SERVER_ERROR(1001)`，避免英文 `detailTT` 直出。
**现码复核**：已在 `GopayController`（含 pollAuthStatus 相关 catch）落地：命中该异常类型则 `throw EcException.error(COMMON_SERVER_ERROR, …)`。`EcControllerExceptionAdvice` 全局转换**尚未**见到。排障/改文案先看 GopayController；若要全渠道覆盖再考虑 Advice。

## mouths
- gopay / apichannel
- 异常 / 前端文案

## anchors
- symbols: `GopayController`, `EcExceptionType.API_CHANNEL_USER_REJECTED`, `EcExceptionType.COMMON_SERVER_ERROR`
- codes: `1001`
- not yet: Advice 全局 normalize

## constraints
- 其它渠道若仍直出 REJECTED 英文，不要假设全局已映射
- ERROR 级还会被 `EcResponse.errStatus` 再盖成通用 toast

## evidence
- Cursor×ec · transcript `b39db070`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
