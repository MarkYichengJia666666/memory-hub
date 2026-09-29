# 0息360 Filter：scene 按完件态互斥，勿靠 MC 碰运气

## decision
`Zero360AcquisitionFilterRuleProcessor` 必须按 payload `scene` 与完件/可下单态互斥，**不是** MC 配错的第一怀疑对象：
- `UNFINISHED`：必须未完件（标签+实验通过后才能出完件前位）；
- `CAN_ORDER_FIRST_LOAN`：必须可下单（首贷），再叠业务门控；勿用「无授信日当 T0」让未完件用户误出完件后 banner。
下单挽留位应对 `ORDER_RETAIN`（或完件后专用策略），**不要**把挽留素材挂在 `UNFINISHED` 策略上——已完件用户会永远不出。完件前/后 banner 若同响应双出，优先查 Processor scene 校验是否缺失，而不是只改素材 priority。

## workspace
- `TAPD-378150-zero-interest-360-lottery`

## mouths
- 0息360 / Zero360Acquisition
- 资源位 scene / 完件

## anchors
- symbols: `Zero360AcquisitionFilterRuleProcessor`, `Zero360LotteryService#isT0`
- scenes: `UNFINISHED`, `CAN_ORDER_FIRST_LOAN`, `ORDER_RETAIN`
- logs: `[Zero360Acquisition] hitRule false: material tag not zero interest`

## constraints
- 联调 0 息**不要**写入 `marketing.qian1_mock_device_whitelist`（会伪造千一、抢走 0 息标签）
- 已完件账号测 `UNFINISHED` 位会与页面进入条件打架，先换未完件号或改测完件后 scene

## evidence
- Cursor×ec · transcript `62632d23`

## status
active

## updated
2026-09-28
