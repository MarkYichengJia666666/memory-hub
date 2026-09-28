# 打开 App 发券自测：泳道与 Normal 共用 Kafka 消费组会「假不发」

## decision
打开 App 异步灌券（如 `AppStartUpGrantCouponProcessor` / secondOrderDiscount）在泳道自测时，若 **Normal 与泳道 consumer 共用消费组**，消息按 partition **概率**落到 master 旧包或泳道新包：落到 Normal → 新逻辑不执行，表现为「怎么都不发」；落到泳道 → 全链路可一次发成功。排障勿先定 ZK ruleId / 「更优才发」抑制，先确认消费落在泳道 pod 且有对应 DEBUG/业务日志。
另：若 `abTestByUserId`（写入组）排在 `ruleId==null` 校验之前，生产 ZK 未挂 rule 会出现「全员入组、零发券」污染实验——更稳妥是 ruleId 就绪校验先于 abTest。

## workspace
- `TAPD-374680-reloan-discount-coupon`

## mouths
- 打开 App 发券 / Kafka
- 泳道自测

## anchors
- symbols: `OpenAppGrantCouponService`, `AppStartUpGrantCouponProcessor`, `ReloanSecondOrderDiscountCouponService`
- config: `reloan_second_order_discount_coupon_rule_id`（ZK）

## constraints
- 正例账号须能验证：payout=1、实验组、消息进泳道 consumer
- 勿把「兄弟券更优抑制」当成 feature-wide 零发放的第一根因

## evidence
- Claude×ec · session `f0d3df11`

## status
active

## updated
2026-09-28
