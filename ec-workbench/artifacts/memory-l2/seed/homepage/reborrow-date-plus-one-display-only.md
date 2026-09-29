# 可再借日历日 +1：只改展示，不改风控判定

## decision
临时拒贷静默期首页「可再申请日期」实验组按风控原始可重提时刻 **+1 个印尼时区自然日**展示；弹窗是否出现、静默期是否结束、能否重提授信，仍用**原始时刻 vs 当前时刻**，对照组/空白/未入组与线上一致。
入组以实验 Key `technology-not_withdraw-abroad-loan_all-loancalander` 为准（technology 泳道 + `loan_all` 全贷种，含首贷），不要按 PRD 旧文「复贷下单泳道」理解。副文案若展示的是**账单日/最近应还日**而非可再借日，**不要 +1**。通知侧本期只动首页卡片相关展示。

## workspace
- `TAPD-1371851-reborrow-date-plus-one`

## mouths
- 首页大卡 / 可再借日期
- 临时拒绝 / 静默期

## anchors
- symbols: `RejectCardProcessor`（及首页可再申请日期展示链）
- experiment: `technology-not_withdraw-abroad-loan_all-loancalander`

## constraints
- 勿把 +1 写进资格判定或剩余天数口径
- 分流失败静默回落现网展示，勿因实验导致首页缺元素

## evidence
- Cursor×ec-01 · transcript `5d1092b5`

## status
active

## updated
2026-09-28
