# LoanCommonApiChannel：通用 Controller 已移除

## decision
历史判决：`LoanCommonApiChannelController` 生产零调用，可作下线证据。
**现码复核**：该类已不在仓；loanmarket 活流量在专属 Controller。本条归档。

## mouths
- loanmarket / apichannel

## anchors
- removed: `LoanCommonApiChannelController`
- live examples: `AkulakuController`, `IndosatH5Controller`

## constraints
- 仍须警惕遗留枚举/列表引用（如历史 `LAZADA_SELLER`）

## evidence
- Cursor×ec · transcript `d2bbee6d`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
superseded

## updated
2026-09-24
