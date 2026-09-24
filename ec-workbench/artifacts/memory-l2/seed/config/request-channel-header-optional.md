# 请求 CHANNEL 头：缺失时 channel=null 正常

## decision
Monitor/打点里的 `channel` 来自请求头 `CHANNEL`（`RequestParser.getChannelOrNull`）。头缺失则为 `null`（打点常写成字符串 `"null"`），**不是故障**。
App 内 WebView 协议页常见 `clienttype=INSIDE_WEB_*` + `platformtype=WEB` 且**无 CHANNEL**；不要据此误判端外或归因丢失。

## mouths
- monitor / 打点
- WEB / WebView

## anchors
- symbols: `RequestParser#getChannelOrNull`
- header: `CHANNEL`

## constraints
- channel=null 先查是否带头，再查业务逻辑
- platformtype=WEB ≠ 一定有 CHANNEL

## evidence
- Cursor×ec · transcript `53e0dc5e`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
