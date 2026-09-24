# AB：isHitSubABTestForLoanUser 刷屏不是死循环

## decision
日志里 `isHitSubABTestForLoanUser` / `CouponCreditLimit` 被同一 trace 调用上千次，**通常不是 bug 死循环**，而是 **上游重复请求 + 策略内多规则重复计算** 叠加放大。
排障先拆：入口请求次数 × 单次策略规则数，再谈是否要缓存/短路。

## mouths
- abtest / strategy
- coupon

## anchors
- symbols: `isHitSubABTestForLoanUser`, `CouponCreditLimit`, `ExecuteAbTestService`, `EcStrategyService`
- related entries: `RealTimeEventController`, `OpenAppGrantCouponService`

## constraints
- 先量化「请求放大 vs 规则放大」，不要一上来改循环结构
- 实验批量 reopen 会导致每次分流扫全量人群规则（另见突增案例）

## evidence
- Cursor×ec · transcript `75ad7c10`

## status
active

## updated
2026-09-24
