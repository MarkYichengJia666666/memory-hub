# Privy 活体：信任端上结果、仅借款完件 Android

## decision
Privy 活体本期：**信任客户端结果，服务端不做二次真人校验**；失败重试/次数与现网活体共用，不为 Privy 单设上限。范围仅**借款端完件鉴权 Android**；理财端、下单补件、比对、Web/iOS 供应商行为不变（回归须保证引入 PRIVY 不伤 Web）。
活体图按现网口径落库并可复用为最近一次图。完件链路 `LivingInfoAuthWithPrivyProcessor` 在 `rawStored=false` 时会**抛错拦住用户**（不是文档曾写的「只 warn」）；补件链路才是 best-effort——排障/写 spec 以代码为准。

## workspace
- `TAPD-373401-privy-liveness-app-api`
- `TAPD-1373384-living-provider-abstraction`

## mouths
- 活体 / Privy
- 完件鉴权

## anchors
- routes: `/api/loan/verifyLivenessMethod`, `/api/loan/v5/uploadLivingInfo`
- symbols: `LivingInfoAuthWithPrivyProcessor`

## constraints
- `verifyLivenessMethod` 里 PRIVY 路由白名单若归属其他 TAPD，勿在本需求硬塞
- 凭证不下发到端外无关渠道

## evidence
- Cursor×ec · transcript `945ad9ca`
- Cursor×ec-01 · transcript `a4ec9851`

## status
active

## updated
2026-09-28
