# SAQU：无专用注册/戳额归因链路

## decision
仓库里**没有**「SAQU 用户注册/戳额」专用归因写入；现有 `SAQU` 多为资方/银行枚举。机构侧 `mediaSource` 来自投放（AppsFlyer 等）与下单旁路+channel 配置映射；**EC 内没有按 mediaSource 过滤资方的实现**。
（绑卡「只留自家卡」若要对标，仍看 SuperBank 实验过滤模式，与本条归因结论分开。）

## mouths
- saqu / 归因
- bindcard / 资方

## anchors
- symbols: `UserService#doRegister`, `AdvertisementService`, `RegisterMediaSourceAdRuleMethod`
- note: `SAQU` 作 Bank/资方枚举 ≠ 注册归因通道

## constraints
- 不要在 EC 里找「SAQU 专用归因表/写入」当前提
- 资方过滤与 mediaSource 归因是两件事

## evidence
- Cursor×ec · transcript `69975a84`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
