# 仅给自己看的试写：Memory Loop myself 隔离

## decision
这条判决只进 **Loop for myself**（`ec-memory-myself`），不进 team 共享库存。用来冒烟：team 身份 `scenario/read` 应读不到同 path。

## scope
myself

## mouths
- memory-isolation
- loop-myself

## anchors
- tools: `seed-l2-auto-import --scope myself`
- identity: `.p0-memory-identity-myself.json`

## constraints
- 勿把个人草稿写进 `seed/`（那是 team）
- 团队可复用结论应放 `seed/` 并 `--scope team`

## status
active

## updated
2026-09-30
