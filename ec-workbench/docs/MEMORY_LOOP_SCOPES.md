# Memory Loop 隔离：myself × team

| Loop | 目录 | 身份文件 | Hub team / agent |
|---|---|---|---|
| **myself** | `artifacts/memory-l2/seed-myself/` | `.p0-memory-identity-myself.json` | `ec-memory-myself` / `ec-chat-memory-myself` |
| **team** | `artifacts/memory-l2/seed/` | `.p0-memory-identity.json` | `ec-memory-p0` / `ec-chat-memory` |

隔离靠 **不同 team_id|agent_id 的 scene_blocks 目录**，不是靠 path 前缀。

```bash
# 个人
./bin/seed-l2-auto-import --scope myself seed-myself/ops/xxx.md
./bin/claude-via-memory --scope myself

# 团队（默认）
./bin/seed-l2-auto-import --scope team path/in/seed/...
./bin/on-seed-change          # 扫两档变更
./bin/claude-via-memory       # 默认 team
```

## 会话 → 草稿 → 晋升

```bash
# 从粘贴/文件/transcript 抽 L2 草稿（默认 LLM 精修，人闸）
pbpaste | ./bin/extract-seed-from-session --scope myself
./bin/extract-seed-from-session --transcript path/to/chat.jsonl --tail 40
./bin/extract-seed-from-session --text "…" --no-llm --dry-run

# 人核对后导入
./bin/seed-l2-auto-import --scope myself path/in/seed-myself/...

# 个人试写成熟 → 合入 team（myself 默认标 superseded）
./bin/promote-myself-to-team ops/foo.md --import
```

## 保鲜 miss 队列

```bash
./bin/freshness-check --limit 20          # 结束会刷新队列
./bin/freshness-miss-queue                # 或单独从最新 batch 重建
# → artifacts/memory-l2/queues/freshness-miss-latest.md
```

## 问法变体召回 / 有用性

```bash
./bin/recall-variant-proof
./bin/usefulness-batch-proof          # 8/8 Proxy+L2
# → artifacts/memory-l2/proof/recall-variant-latest.md
# → artifacts/memory-l2/usefulness-batch-latest.md
```

回执：`receipts/myself/latest.md` · `receipts/team/latest.md` · 总览 `receipts/latest.md`
