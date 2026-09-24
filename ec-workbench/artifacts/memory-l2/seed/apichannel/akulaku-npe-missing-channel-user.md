# Akulaku SYSTEM.00000：旧 NPE 路径已不在 ProductService

## decision
历史判决称 `ApiChannelProductService.checkUserCanOrder` 在缺 `api_channel_user` 时裸 `getUserId()` → NPE → `SYSTEM.00000`。
**现码复核**：`ApiChannelProductService` 只做产品/试算，**没有** `checkUserCanOrder`；注册侧 `ApiChannelRegisterService#checkUserCanOrder` 走 `checkApiChannelUserType`，不是该 NPE 形态。本条作废，勿再按旧路径排障。

## mouths
- akulaku / apichannel

## anchors
- current: `ApiChannelProductService#getProductAndTrialData`, `ApiChannelRegisterService#checkUserCanOrder`
- removed claim: `ApiChannelProductService#checkUserCanOrder` NPE

## constraints
- SYSTEM.00000 仍可能来自其它空指针/兜底，但不要绑定已删除的 ProductService NPE 故事

## evidence
- Cursor×ec-1 · transcript `976eef76` / `21731dba`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
superseded

## updated
2026-09-24
