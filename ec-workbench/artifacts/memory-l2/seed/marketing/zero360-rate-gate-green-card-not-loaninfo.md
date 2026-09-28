# 0息360 日息门禁：绿卡/承接要，还款计划旁标签不要

## decision
完件后 **绿卡 / 优惠券区 360 文案**、MC 转盘/banner/挽留：要过日息门禁（与 `isZeroInterest360Hit` / `matchesZeroInterestRateRule` 同口径）；日息不过 → 不展示，且绿卡路径不入组。
**还款计划旁标签**（`LoanInfoDisplayDataBuilder`）：只要免息标签过了就可 `abTest`，**不接日息门禁**（spec NFR-011 / plan Decision 8；T025「也加日息」已取消）。
因此会出现「绿卡 null、但还款计划旁仍有 ZERO_INTEREST」——是设计不是漏判。`applyZeroInterest360CouponAreaLabelIfHit` 仅供单测，生产走 `applyZeroInterestGreenCardCornerMark`。

## workspace
- `TAPD-378150-zero-interest-360-lottery`

## mouths
- 0息360 / 绿卡
- 还款计划旁标签

## anchors
- symbols: `DiscountDisplayDataBuilder#applyZeroInterestGreenCardCornerMark`, `DiscountDisplayDataBuilder#isZeroInterest360Hit`, `LoanInfoDisplayDataBuilder`
- MC: `ZI360ACQ`

## constraints
- 勿要求「有 ZERO_INTEREST 标就必须处处过日息」——改 NFR 才动 LoanInfo
- 绿卡两条内容源都空时角标为 null，先查日息门禁再查文案配置

## evidence
- Cursor×ec · transcript `4d9b2d78`

## status
active

## updated
2026-09-28
