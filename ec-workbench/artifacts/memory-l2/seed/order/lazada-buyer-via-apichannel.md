# Lazada Buyer：走 apichannel 三方链路

## decision
`LAZADA_BUYER` 是 **indo-apichannel 主导的三方 API 链路**，不是 GoPay 那种 H5 入口。
主序：Lazada → `indo-apichannel /api/loanmarket/lazada/**` → 验签转换 → EC internal-api → 撞库/授信/建单… → callback task 回 Lazada。
改撞库规则时至少分清命中的是 `LAZADA_PRE_SELECT`、`LAZADA_SECOND_PRE_SELECT` 还是 `LAZADA_CREATE_ORDER`，不要只盯一个 checker。

## mouths
- lazada / apichannel
- 三方 API

## anchors
- channel: `LAZADA_BUYER`
- routes: `/api/loanmarket/lazada/**`
- stages: `LAZADA_PRE_SELECT`, `LAZADA_SECOND_PRE_SELECT`, `LAZADA_CREATE_ORDER`
- callback: `LazadaBuyerCallbackBiz`

## constraints
- 不要按 GoPay H5 心智排 Lazada
- 撞库/授信改动要按阶段枚举对号入座

## evidence
- Cursor×ec-1 · transcript `9864b891`

## status
active

## updated
2026-09-24
