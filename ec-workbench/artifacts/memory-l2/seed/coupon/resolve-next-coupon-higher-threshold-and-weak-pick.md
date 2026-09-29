# resolveNextCoupon：更高门槛引导 + 手选弱券兜底

## decision
`CouponWelcomeBannerService#resolveNextCoupon` 的「下一张引导券」主口径是：**门槛严格大于当前借款金额**，在更高门槛候选里按试算补贴比较；多张更优时取门槛更高者。比较基准要用「当前生效券在**候选券门槛金额**下的减免」，不是只用「当前金额下的减免」。
主路径**不会**把「门槛已满足、未选中的更优券」当成 next（那是「借更多解锁」，不是「已可用却手选更弱」）。手选弱券时另有兜底：更高门槛候选走完后，若未选中的 `optimalCoupon` 门槛已满足且当前金额下补贴严格更大，可返回该最优券作引导（横条从 `MAX_DISCOUNT_REACHED` 转为可引导）。`MAX_DISCOUNT_REACHED` 只表示没有更高门槛引导券，**不保证**用户已用手选金额下最强券。

## workspace
- `TAPD-369579-coupon-bottom-banner`（与 EC `specs/<workspace>` 同名；仓内亦见 `2026-08-08-coupon-bottom-banner`）

## mouths
- 优惠券横条 / welcome banner
- resolveNextCoupon / 更优券

## anchors
- symbols: `CouponWelcomeBannerService#resolveNextCoupon`, `CouponWelcomeBannerService#collectCutInterestCouponPool`, `trialCandidateAtThreshold`
- states: `MAX_DISCOUNT_REACHED` / `MAX_DISCOUNT_NOT_REACHED`

## constraints
- 勿把「查不到更高门槛候选」误判为「最优券没进池」——先看门槛过滤
- 勿假定 `MAX_DISCOUNT_REACHED` = 已用最强券
- 产品若要「强拧成 optimal」需另开规则，与「跟手选」US 冲突时先对齐产品

## evidence
- Cursor×ec-01 · transcript `2a93c077`
- Cursor×ec · transcript `43ca44a1`

## status
active

## updated
2026-09-28
