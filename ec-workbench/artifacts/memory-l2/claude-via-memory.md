# Claude Code × Memory Proxy（隔离接入）

> 不改 `~/.claude/settings.json`（你日常仍走公司网关）。

## 已就绪（2026-09-23 冒烟通过）

| 项 | 值 |
|---|---|
| Proxy | `http://127.0.0.1:8096`（FULL_STACK） |
| CC 路径 | `/claude-code/default` → `…/anthropic/v1`（拼 `/messages`） |
| OpenAI 路径 | `/proxy/default` → Qwen/DeepSeek（脚本验收用） |
| 隔离配置 | `ec-workbench/.claude-via-memory/settings.json` |
| 启动器 | `ec-workbench/bin/claude-via-memory` |
| Team / Agent | `ec-memory-p0` / `ec-chat-memory` |

冒烟：经 `/claude-code/.../v1/messages` 问「开屏灌券发券在哪」→ 答 Kafka + `AppStartUpGrantCouponProcessor`（已注入记忆）。

## 你怎么用（证明读的是 Memory）

```bash
# 1) 保证 proxy
cd /Users/lipeng/IdeaProjects/TencentDB-Agent-Memory/deploy/global-images
PROXY_FULL_STACK=1 ./start-proxy.sh

# 2) 空目录（避免搜到 EC 代码）
mkdir -p /tmp/mem-proof && cd /tmp/mem-proof

# 3) Memory 版 Claude Code（自动附加「先读 Memory」系统约束）
/Users/lipeng/IdeaProjects/TencentDB-Agent-Memory/ec-workbench/bin/claude-via-memory
```

若弹 Form：选 **ec-memory-p0** → **ec-chat-memory**。

粘贴开场白（或启动器打印的验证问法）：见 [`MEMORY-FIRST-PROMPT.md`](./MEMORY-FIRST-PROMPT.md)。

成功标准：过程里出现对 `127.0.0.1:8096/memory-bridge/v3/...` 的 curl，回答带 `【来源：Memory】`。

## 权限（训练必备）

根因：选了 **auto mode** 后，Bash 要过安全分类器；分类器走 `glm-5.3` 超时 → curl 被卡住，只能改读盘。

隔离配置已固定：
- `permissions.defaultMode = bypassPermissions`
- 预放行 `Bash(curl *)`
- `sandbox.enabled = false`（避免拦 localhost:8096）
- 启动器带 `--permission-mode bypassPermissions`

**不要再点「Yes, and switch to auto mode」。**

## 注意

- 日常直接 `claude` = 原公司网关，**不经 Memory**。
- `.env` 需有 `PROXY_CC_UPSTREAM_URL=https://all-in-one-ai.fintopia.tech/anthropic/v1`（必须带 `/v1`）。
- 重启 proxy 用 `PROXY_FULL_STACK=1`。
