# 会话收尾：新判决自动写 Hub（旁路回执）

## decision
用 Memory 答完或挖出**新业务判决**后：起草 seed → **自动** `seed-l2-auto-import` 写入 Hub → 把回执（path + 摘要）给用户看。不再等人点确认才写；纠错靠改 seed / `superseded` 后再导入。
下一轮空会话应能按 path 召回。

## mouths
- memory / ops
- chat-memory-ec

## anchors
- tools: `ec-workbench/bin/seed-l2-auto-import`, `ec-workbench/bin/on-seed-change`, `ec-workbench/bin/claude-via-memory`
- receipt: `ec-workbench/artifacts/memory-l2/receipts/latest.md`
- skill: `ec-workbench/skills/chat-memory-ec/SKILL.md`
- path_pattern: `ec/<business>/<slug>.md`
- hub_api: `POST /v3/scenario/write`（不能新建，需先落盘）

## constraints
- 不改 EC 主仓；不改 Memory Hub 产品代码
- 打脸旧判决时：旧条 `status: superseded`，新条另开 path
- 自动写必须出旁路回执；禁止完全静默、用户事后完全不知情
- Proxy 会话内静默写 L2 仍不做；入口是 seed 文件 + auto-import

## evidence
- 2026-09-24：半自动人闸流程
- 2026-09-29：改为自动写 + receipts/latest.md 旁路可见

## status
active

## updated
2026-09-29
