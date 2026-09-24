# 下单页样式：正交防结清实验仍命中

## decision
用户下单页仍出「首期样式」等标志时，**不要默认怪当前灌券实验（n1/n2）**。
已证实案例：正交样式实验仍命中实验组——
`AntiSettleCicilanPromoExpService.isExperimentGroup` → `hitsReloanNormalExp` → 实验 `...anti_loss_cicilan_promotion_upgrade_normal` → `EXPERIMENT_GROUP`。
排障要列出**所有**会改下单页样式的实验，而不是只盯本需求灌券 key。

## mouths
- coupon / 下单页
- experiment

## anchors
- symbols: `AntiSettleCicilanPromoExpService#isExperimentGroup`, `hitsReloanNormalExp`
- related: `hitsHeadMidGrantCouponV1Exp`

## constraints
- 样式/标志类问题先做实验正交排查
- 关灌券实验不等于样式一定消失

## evidence
- Cursor×ec · transcript `b872ad7a`

## status
active

## updated
2026-09-24
