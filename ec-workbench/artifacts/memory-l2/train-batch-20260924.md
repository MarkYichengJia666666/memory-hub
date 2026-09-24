# 从 ec / ec-1 历史对话训练 · 第二批（2026-09-24）

> 扫描：ec ~303 + ec-1 ~158 会话 → 候选池 258 → 人工精选 **6** 条写入  
> 此前已有 9 条（含 open-app-grant）→ 现业务 L2 约 **15** 条

## 本批新写入

| path | 一句话 |
|---|---|
| `ec/auth/h5-app-cross-login-order.md` | 跨端登录看开关+sourceType；下单看渠道单状态与 INDOSAT_CL2 |
| `ec/experiment/blank-group-null-grouptype.md` | 扩全后仍要验 groupType=null 空白组语义 |
| `ec/notif/tt-gen-full-sentence-key.md` | 站内信勿把含日期整句当翻译 key |
| `ec/kafka/consumer-enum-missing-crash.md` | topic_config 枚举 ⊃ 代码 → Consumer 起不来 |
| `ec/apichannel/collision-upgrade-fallback.md` | 撞库升级已有 executeUpgradeWithFallback |
| `ec/gopay/credit-data-global-table.md` | getCreditData 读全局 loan_user_credits_info |

均已：`scenario/write` + Panel L0 import。本地镜像：`artifacts/memory-l2/seed/`。

## 刻意没写进 Memory 的

过程态（AICR/revert/文档 warn）、Memory 方案讨论、单次排障无符号锚点、易过期实验态——继续过滤。

## 验证

```bash
cd /tmp/mem-proof && .../ec-workbench/bin/claude-via-memory
# 例：跨端下单渠道单未还清能不能在端内再借？先 curl Memory。
```
