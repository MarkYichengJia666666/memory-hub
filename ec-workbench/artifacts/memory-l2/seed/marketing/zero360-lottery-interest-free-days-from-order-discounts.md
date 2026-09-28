# 0息360 转盘免息天数：用 OrderDiscounts 总免息天，勿用折扣÷日息 floor

## decision
转盘/抽奖展示免息天数时，应用 `OrderDiscounts#getTotalInterestFreeDays()`（或同口径聚合），**不要**用「券折扣 ÷ 标准日息再 floor」。后者在折扣不足一日息时会 floor 成 0，落到 `LUCKY_UNKNOWN`，用户侧像「没抽中/未知奖」而实际是计算口径错了。额度/试算正常但领不到券时，另查配置 `loan.zero360_lottery.coupon_rule_config_id` 是否指向正确的贷前降息券规则（环境数据不一致会导致工具券配错）。

## workspace
- `TAPD-378150-zero-interest-360-lottery`

## mouths
- 0息360 / 转盘
- 免息天数

## anchors
- symbols: `OrderDiscounts#getTotalInterestFreeDays`, `Zero360LotteryService`
- config: `loan.zero360_lottery.coupon_rule_config_id`
- symptoms: `LUCKY_UNKNOWN`

## constraints
- 排查「转盘未知奖」先核对免息天计算公式，再查 MC 奖品
- 券规则 ID 以贷前 100% 降息券工具为准，勿沿用过期环境 ID

## evidence
- Cursor×ec · transcript `62632d23`

## status
active

## updated
2026-09-28
