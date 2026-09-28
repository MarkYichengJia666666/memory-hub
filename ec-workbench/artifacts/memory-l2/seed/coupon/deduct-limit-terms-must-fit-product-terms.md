# deductLimitTerms 与 productLimitTerms 语义不同，可用性要双校验

## decision
`productLimitTerms` = 哪些**产品期数**可用该券；`deductLimitTerms` = 只抵扣哪些**期序号**的利息。二者独立。
仅命中 `productLimitTerms` 不够：若配置了 `deductLimitTerms`，且用户所选 `currentProductTerms` 比所有减免期都小（产品期内碰不到任一减免期），券应对该产品 **DISABLED**（可复用 `CUT_INTEREST_PRODUCT_TERMS_LIMIT` 原因，除非产品要单独文案）。
示例：`deductLimitTerms=[5]`、`currentProductTerms=3` → 不可用；`currentProductTerms=6` → 第 5 期存在，仍可用。
按天折扣类 GrantConfig（如 `ProportionPercentDayCutInterestRuleGrantConfig` / `FixedDayPercentCutInterestRuleGrantConfig`）**不应**支持 `deductLimitTerms`。

## workspace
- （跨需求通用券能力；无单一 TAPD 目录时以券 VO 为准）

## mouths
- 降息券 / LoanUserCoupon
- 期数限制

## anchors
- symbols: `LoanCutInterestCouponVO#checkCouponAvailability`, `LoanCutInterestCouponVO#getCouponDisabledReasons`, `CheckCouponParam#currentProductTerms`
- fields: `deductLimitTerms`, `productLimitTerms`
- enums: `LoanCouponDisabledReason.CUT_INTEREST_PRODUCT_TERMS_LIMIT`

## constraints
- 勿用 `productLimitTerms` 单独解释「选了短产品却仍显示可用」
- 排查「有券无减免」先对一下减免期是否落在当前产品期数内

## evidence
- Cursor×ec · transcript `738439e6`

## status
active

## updated
2026-09-28
