# 腾讯活体：国际版 H5，不是国内 Compare/Recognition

## decision
ec-api 生产**没有**直接调用腾讯云国内版 `LivenessCompare` / `LivenessRecognition`。
用的是国际版 SDK（`tencentcloud-sdk-java-intl-en`）的 **H5 Web Verification**：`ApplyWebVerificationBizTokenIntl` + `GetWebVerificationResultIntl`（`livingSource=TENCENT_H5`）。
入口含 `/api/liveness/detection/token`、`uploadLivingInfo`、`livenessDetection`、`faceComparison` 等。

## mouths
- living / 活体
- tencent / faceid

## anchors
- symbols: `ApplyWebVerificationBizTokenIntl`, `GetWebVerificationResultIntl`, `TencentAiService`
- not used: `LivenessCompare`, `LivenessRecognition`
- source: `TENCENT_H5`

## constraints
- 排障/合规问「有没有调国内活体比对 API」时，答案是否定的
- 不要按国内版 API 名去搜生产调用

## evidence
- Cursor×ec · transcript `1935f678`

## status
active

## updated
2026-09-24
