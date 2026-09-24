# Memory 优先 · 开场白（复制到 Claude Code）

> 用于证明「答的是 Memory，不是搜仓」。建议在 `/tmp/mem-proof` 启动 `claude-via-memory` 后粘贴。  
> **勿开 auto mode**（分类器超时会卡死 curl）。启动器已强制 `bypassPermissions`。

## 系统约束（也可由启动器 `--append-system-prompt` 自动带上）

你接入了 TDAI Chat Memory（system 里有 `<tdai_memory_tools>` / `<tdai_profile_memory>`）。

涉及业务判决、历史约定、对标哪套逻辑时：

1. **必须先**用 Bash + curl 调 memory-bridge（见注入说明），至少：
   - `tdai_memory_search` 或
   - `tdai_scenario_ls` / `tdai_read_scene`（L2 路径形如 `ec/.../*.md`）
2. **禁止**在未读 Memory 前用 grep/find/Glob 搜业务仓或 wiki。
3. 回答开头写：`【来源：Memory】path=...`；若 Memory 无结果再写 `【来源：代码】` 并说明。

Memory bridge 基址（本机 Proxy）：`http://127.0.0.1:8096/memory-bridge/v3`  
读 L2 示例：

```bash
curl -sS -X POST 'http://127.0.0.1:8096/memory-bridge/v3/scenario/read' \
  -H 'Content-Type: application/json' \
  -d '{"path":"ec/bindcard/saqu-superbank-pattern.md"}'
```

（身份由 Proxy 按当前 session 注入；body 一般只需业务字段。）

## 用户问题（验证用，原样粘贴）

```text
先按 <tdai_memory_tools> 用 curl 读 Memory，不要搜代码仓。
问题：Saqu「只能绑自家卡 + 要可开关」，应对标哪套已有过滤逻辑？关键方法名？
读完 L2 再答；开头写【来源：Memory】和 path。
```
