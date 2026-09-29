# KTP 扑脸/回退样式：L1 标签只解析一次再分派

## decision
`AbTestAndJoinActivityForUserIdProvider` 在 KTP 营销扑脸（`KTP_MARKETING_POPUP_DISPLAY`）与 `AUTH_RETURN_AC` 路径上，**不要**对千一/万一/0息各调一次 `*Eligibility#shouldShowPreAuth`（内部各跑一遍 L1：ads + 账户 + 完件态 + 未归因 ab）。
应：`materialTagResolver.resolveForPreAuth` **一次**，按 `MaterialTagHitVO.hitTag` 分派到 `isEligibleAfterMaterialHit`；未归因实验已在 L1 归因成万一时，`hit.isAttributedByUnattribExp()` 为 true，**不再要求现网万一实验组**。

## workspace
- `TAPD-378872-unattrib-wany-fallback`（与 EC `specs/<workspace>` 同名）

## mouths
- KTP 扑脸 / D3 落地页
- MaterialTag / 三利益点

## anchors
- symbols: `AbTestAndJoinActivityForUserIdProvider#isKtpAcquisitionHit`, `IMaterialTagResolver#resolveForPreAuth`, `Qian1Eligibility`, `WanyEligibility`, `ZeroInterestEligibility`

## constraints
- `hitTag` 单值互斥，三个并列 `shouldShowPreAuth` 属于重复 RPC/重复入组
- 与 `design-layers` L3「先 L1 再 L2」一致

## evidence
- Cursor×ec-01 · transcript `9358277a`

## status
active

## updated
2026-09-28
