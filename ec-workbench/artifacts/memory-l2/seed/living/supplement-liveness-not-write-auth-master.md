# 下单补件活体：PRIVY 信任端上结果，勿写完件 LivingInfo 主档

## decision
下单补件走 `POST /api/secure/livenessDetection` 的 `PRIVY` 分支：与完件一样**信任客户端、不做服务端二次复核**；校验上报 → 处理影像 → 返回 `LivenessDetectionResult`。
**不要**调用完件用的 `updateLivingInfo`（会写 `loan_account_details.LivingInfo` / union，污染完件鉴权主档）。补件应落通用影像存储；注意 `living_image_data_details` 若是「每供应商一行」去重表，同供应商第二次写入可能被覆盖——FacePP 补件用 append 表才两次留痕，Privy 选型要对齐场景。完件接口是 `/api/loan/v8/uploadLivingInfo`；服务端**没有**单独的 Privy「活体比对」HTTP。

## workspace
- `TAPD-373401-privy-liveness-app-api`

## mouths
- 下单补件活体 / livenessDetection
- Privy vs Face++

## anchors
- routes: `/api/secure/livenessDetection`, `/api/loan/v8/uploadLivingInfo`
- symbols: `LoanUserLivingSource.PRIVY`
- anti-pattern: 补件调用 `updateLivingInfo`

## constraints
- 原 373401 若仅完件，扩补件须单独定存储模型
- 勿假设存在服务端 Privy 比对接口

## evidence
- Claude×ec-01 · session `45e09cda`
- Claude×ec-01 · session `53572b99`

## status
active

## updated
2026-09-28
