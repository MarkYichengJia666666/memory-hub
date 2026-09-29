# 确认后加借弹窗：超额截断仍弹，利息 I 用券前总息

## decision
`TAPD-371433` 加借差额：当计算出的加借额 `D0` 超过上限 `D_max` 时，应 **`D1 = min(D0, D_max)` 后仍出弹窗**，不是 `empty` /「超过不弹」（以 PRD 为准；spec 曾误写成 Out of Scope）。
公式里分母 **I = 用券前总利息**（`oiPlan.order.interest`），不能用首期利息。入组「利息盖住」口径是**首期剩余利息为 0**，勿与总息 I 混用。频控：EC 不写限频；MC 素材配自然日 1 次。

## workspace
- `TAPD-371433-post-confirm-borrow-more`

## mouths
- 加借 / borrow more
- 下单确认后弹窗

## anchors
- symbols: （加借 delta / createOrderCheck extraParams 下发链；与 `FirstLoanBorrowMorePopupService#computeDelta` 同族公式可对照）
- config: MC 素材自然日频次

## constraints
- 勿再把 `D0 > D_max → empty` 当验收预期
- H5/API/iOS 负向与渠道门控共用，不必为每端单列兼容矩阵

## evidence
- Cursor×ec-01 · transcript `957dd7ed`
- Cursor×ec-01 · transcript `a0269daf`

## status
active

## updated
2026-09-28
