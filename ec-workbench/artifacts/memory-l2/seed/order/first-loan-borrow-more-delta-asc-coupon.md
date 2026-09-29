# 首贷加借弹窗：C/E 分口径，券须 PercentCutInterest + ASC

## decision
加借差额算在 `FirstLoanBorrowMorePopupService#computeDelta`：**C** = `remainCreditsVO.realRemainingCreditsForVirtual`（剩余可借，可能再与全场最高档取小），**E** = 当前 `productId` 的 `productVO.maxCredits`，二者不是同一额度；公式含 `L×N×A÷I` 时 I 为用券前总利息。
入组还要求可用券为 **`PercentCutInterestCouponVO` 且 `deductType=ASC`（正序）**，并能盖住首期利息；否则 `[BorrowMorePopup] skip`。实验 **`abTest` 在资格通过后**再做，避免非目标人群污染样本。排障「该弹不弹」先查 `usage_rule_map.deductType` 是否 ASC。

## workspace
- `TAPD-367267-first-loan-recommend-popup`（对话亦见 `TAPD-371982-first-loan-limit-popup` 命名）

## mouths
- 首贷加借 / 推荐弹窗
- 额度差额

## anchors
- symbols: `FirstLoanBorrowMorePopupService#computeDelta`, `PercentCutInterestCouponVO`
- fields: `deductType=ASC`, `remainCreditsVO.realRemainingCreditsForVirtual`, `productVO.maxCredits`

## constraints
- 勿把 C 与 E 写成同一个「用户额度」
- 协议页档位异常（如 1001）优先核对 E 是否被错误当成账户剩余

## evidence
- Cursor×ec · transcript `1cebd840`

## status
active

## updated
2026-09-28
