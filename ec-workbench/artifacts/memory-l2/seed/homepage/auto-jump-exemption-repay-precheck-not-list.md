# 复贷自动跳豁免：还款意图收窄为「立即支付 preCheck」

## decision
跳转豁免实验（重开 13946 / Key 形如 `reloan_order_26h1-not_withdraw-abroad-renew-noautojump_reopen_v1`）的还款意图信号 **R2** 应对齐 1.0 最初口径：点击账单页底部「立即支付（Bayar Sekarang）」触发的**还款前预检查**（含随后合并还款引导），**不要**再用「打开账单列表」`/api/cashloan/instalmentList`（偏宽）。
与 Push 预还款信号按印尼 WIB **自然日当日有过即命中**（1.0 日维），本期是跳转豁免不是发券，不适用防结清 holdout 前后置规则。实验组不跳、对照 fail-open 照跳。

## workspace
- `TAPD-375653-reloan-auto-jump-exemption-v2`（对话亦覆盖 1.0/`TAPD-365607` 收窄讨论）

## mouths
- 自动跳 / 跳转豁免
- 还款意图

## anchors
- symbols: `JumpExemptionService`, `AutoJumpExemptionDailyLoader`
- routes: 还款前预检查（非 `instalmentList` 打开）
- experiment: `13946` / `…noautojump_reopen_v1`

## constraints
- 勿把「打开账单列表」继续算作 R2
- 端外渠道默认 Out of Scope，除非产品显式要求 WEB 回归

## evidence
- Cursor×ec · transcript `d4d31a23`

## status
active

## updated
2026-09-28
