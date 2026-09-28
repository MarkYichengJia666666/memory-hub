# 0息360 优惠券区文案走绿卡统一入口，非独立 Builder 方法

## decision
下单页 productDetail「优惠券区 / 绿卡」0息360 文案（如 `t0InterestFreeDaysDesc` / `Bunga 0% N hari`）应在 `DiscountDisplayDataBuilder#applyZeroInterestGreenCardCornerMark` 内与 0息 n 期做 T0/T1+ 选型，命中时覆盖 `SeaDiscountDetail` 并清空冲突的 `zeroInterestTermsCornerMark`。
`applyZeroInterest360CouponAreaLabelIfHit` 仅保留给单测直验口径，**生产 build 路径不调用**；大按钮角标（`largeButtonCornerLabelContent`）本期 Out of Scope。
命中 360 后日息门禁（券后日息与 `Zero360AcquisitionFilterRuleProcessor` 同口径）在 `isZeroInterest360Hit` 内校验，素材标签+实验入组 alone 不够。

## workspace
- `TAPD-378150-zero-interest-360-lottery`（与 EC `specs/<workspace>` 同名）

## mouths
- 0息360 / productDetail
- DiscountDisplayData / 绿卡

## anchors
- symbols: `DiscountDisplayDataBuilder#applyZeroInterestGreenCardCornerMark`, `DiscountDisplayDataBuilder#isZeroInterest360Hit`, `Zero360LotteryService#resolveCurrentDisplayDays`, `LoanInfoDisplayDataBuilder`（还款计划区同天数口径）

## constraints
- 勿再挂已删除的按钮角标 / `productDetailDisplayStyle.ZERO_INTEREST_360`
- 排查「有 360 实验但优惠券区没文案」先看是否走了绿卡统一入口而非孤立方法

## evidence
- Cursor×ec · transcript `cb302df1`
- Cursor×ec · transcript `356bacef`

## status
active

## updated
2026-09-28
