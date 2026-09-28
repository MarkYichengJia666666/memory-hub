# 下单页 Back 加码：本期只做发券，月供弹窗剔除

## decision
`TAPD-368547` 范围收窄为**下单页 Back 加码发券**；原 PRD 标题里的「月供弹窗 / Double Bonus 划线价弹窗」承接已剔除。Out of Scope 含：新增独立发券 HTTP、改现网 Back 弹窗样式/优先级、首页轻触达发券、与 T1/还款计划加码互斥、离线灌券入组与券面、加码次数/日预算上限。
落地符号族：`OrderBackBoostService` / `OrderBackBoostFilterRuleProcessor` / `OrderBackBoostResolveService` / `OrderBackBoostGrantCallbackHandler`。客群若以利率实验标签划分，以 `interest_rate_experiment_tag` 约定取值为准（勿与另一段 af_rank+flag_will 描述混用而不加标注）。

## workspace
- `TAPD-368547-orderback-coupon-installment`

## mouths
- Back 加码 / order back boost
- 下单页发券

## anchors
- symbols: `OrderBackBoostService`, `OrderBackBoostFilterRuleProcessor`, `OrderBackBoostResolveService`, `OrderBackBoostGrantCallbackHandler`

## constraints
- checklist 标题若仍含「月供弹窗」视为文档遗留，不以标题扩 scope
- 联调证据以当前泳道日志为准，过期 `test-data.md` 旧泳道记录勿当现状

## evidence
- Cursor×ec · transcript `dfe0de9c`
- Cursor×ec · transcript `5fc5cb8d`

## status
active

## updated
2026-09-28
