# 到期还款提醒：LRD Selector 链路

## decision
贷款到期还款提醒通常由 `ec-scheduler` 的 `TargetTaskProducerJob` 驱动：按后台 Selector 配置筛用户 → 写 notif 任务 → Consumer 填模板发送。
文案带具体账单日参数 `INSTALMENT_BILLING_DATE` 时，最常见对应 **`LoanRepaymentDueSelector`（queryType=`LRD`）**；人维度则可能是 `RepaymentReminderInPersonDimensionSelector`（`RRIPD`）。查配置看 Admin「通知用户 Selector」或表 `notif_user_selector`。

## mouths
- notif / 还款提醒
- scheduler

## anchors
- symbols: `TargetTaskProducerJob`, `LoanRepaymentDueSelector`, `NotifUserSelectorFactory`
- queryType: `LRD`, `RRIPD`
- param: `INSTALMENT_BILLING_DATE`

## constraints
- 排「谁发的到期提醒」先对 Selector/queryType，不要只搜短信通道
- LRD vs RRIPD 看模板参数名

## evidence
- Cursor×ec · transcript `b8cdc13d`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
