# Privy 请求日志：fcToken/image1/image2 必须 HideInLog

## decision
`UploadLivingInfoPrivyRequest` 的 `fcToken`、`image1`、`image2` 须加 `@HideInLogStrategy`（或等价），否则接入日志会打出超大 Base64/data URI（数百 KB）并泄露活体图与凭证。落库侧 `toStorableJson` / `PrivyLivingStorableData` 已剔除三者不够——**请求接入日志是另一道口**。脱敏不改变反序列化与业务字段。

## workspace
- `TAPD-373401-privy-liveness-app-api`

## mouths
- 活体请求日志 / 脱敏
- Privy

## anchors
- symbols: `UploadLivingInfoPrivyRequest`
- annotation: `@HideInLogStrategy`

## constraints
- 勿只靠落库剔除认为「不会进日志」
- 排查「请求体异常大」先看是否未隐藏 image 字段

## evidence
- Claude×ec-01 · session `0bf9076c`

## status
active

## updated
2026-09-28
