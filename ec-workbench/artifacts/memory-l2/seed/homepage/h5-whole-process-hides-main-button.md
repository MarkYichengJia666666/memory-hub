# H5 全流程：MAIN_BUTTON 被 showDownloadButton 关掉

## decision
续借相关首页状态（测算中/可申请/审核中等）多数走 `PageCardV3` 元素卡；**MAIN_BUTTON 是否展示统一受 `HomepageWholeProcessTool#showDownloadButton()` 控制**——H5 全流程（`isWholeProcess=true`）直接返回 false，不展示按钮；App / 非全流程 H5 才展示。
例外：`MULTI_LOAN_INIT`（可续借测算）仍走老的 `HomePageMainCardInfo`，不是 PageCardV3。

## mouths
- homepage / H5 全流程
- 续借按钮

## anchors
- symbols: `HomepageWholeProcessTool#showDownloadButton`, `MultiLoanProductProcessor`, `OrderProgressStepProcessor`, `PageCardV3`
- flag: `RequestClientType#isWholeProcess`

## constraints
- H5 全流程「看不到立即申请」先查 showDownloadButton，不要先怪产品 Processor
- 测算额度阶段是另一条老主卡链路

## evidence
- Cursor×ec · transcript `ed671c6f`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
