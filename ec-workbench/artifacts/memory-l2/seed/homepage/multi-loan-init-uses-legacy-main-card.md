# 续借测算：MULTI_LOAN_INIT 仍走老主卡

## decision
续借状态机里，多数状态已迁 `PageCardV3`，但 **`MULTI_LOAN_INIT`（可续借测算）仍走老的 `HomePageMainCardInfo`**（如 `getReCalcCreditsMainCardInfo`），主按钮文案「测算额度」。
不要假设所有续借态都是 PageCardV3 + `showDownloadButton` 同一套。

## mouths
- homepage / 续借测算

## anchors
- status: `MULTI_LOAN_INIT`
- symbols: `HomePageMainCardInfo`, `HomePageMainCardInfoTool#getReCalcCreditsMainCardInfo`
- contrast: `PageCardV3` / `MultiLoanProductProcessor`

## constraints
- 改测算入口按钮先找老主卡工具，不要只改 ProductProcessor

## evidence
- Cursor×ec · transcript `ed671c6f`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
