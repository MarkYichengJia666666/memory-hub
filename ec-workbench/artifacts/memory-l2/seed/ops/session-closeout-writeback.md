# 会话收尾：新判决半自动回写（人闸）

## decision
用 Memory 答完或挖出**新业务判决**后，必须走「起草 seed → 人确认 → 导入 Hub」，禁止静默直写生产记忆。
下一轮空会话应能按 path 召回；未过闸的草稿只留在 `artifacts/memory-l2/seed/`。

## mouths
- memory / ops
- chat-memory-ec

## anchors
- tools: `ec-workbench/bin/seed-l2-to-hub`, `ec-workbench/bin/claude-via-memory`
- skill: `ec-workbench/skills/chat-memory-ec/SKILL.md`
- path_pattern: `ec/<business>/<slug>.md`
- hub_api: `POST /v3/scenario/write`（不能新建，需先落盘）

## constraints
- 不改 EC 主仓；不改 Memory Hub 产品代码
- 打脸旧判决时：旧条 `status: superseded`，新条另开 path
- Proxy 自动写入未做；本条定义的是旁路半自动流程

## evidence
- P0 缺口：能用不会自己记（2026-09-24）
- 导入脚本与收尾清单落地同日

## status
active

## updated
2026-09-24
