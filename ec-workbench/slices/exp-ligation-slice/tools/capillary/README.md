# 毛细血管 · 定时实验死代码体检

定时 Agent（不是 Loop）：到点查实验平台 + CodeGraph，写出「为啥建议删」的报告；
你点头后，再切分支提 diff。

## 和 Memory Hub 的关系

- Hub 提供图（CodeGraph API）
- 实验死活问实验 MCP / 缓存
- 本目录是挂在上面的清理工具，不负责改 Hub 源码

## 嘴（MVP）· 共用一张合并图

图 id：`cg-zp42c34n`（`ec-cashloan-combined-slice`，五业务并集）

| 嘴 | 说明 |
|---|---|
| homepage | 首页；枚举分发 callers 常空 → 只报告不自动删 processor |
| order | 下单 |
| coupon_kafka | 开屏灌券 Kafka |
| bindcard | 绑卡；含 Superbank 全量固化候选 |

## 怎么跑

```bash
cd /Users/lipeng/IdeaProjects/exp-ligation-slice/tools/capillary

# 1) 先刷新实验缓存（本机有 Cursor MCP 时，用 refresh 脚本；或手工把 MCP 结果塞进 state/）
python3 refresh_experiments.py          # 若配置了 HTTP；否则见下

# 没有 HTTP 时：用 Cursor 里查到的结果写入
python3 refresh_experiments.py --from-json state/experiment_live.json

# 2) 体检出报告
python3 run_scan.py

# 3) 看报告
ls -lt reports/ | head

# 4) 批准某条候选后，预览 / 提 diff（默认 dry-run，不改 EC）
#    candidate_bury → 删除计划；candidate_solidify → 全量固化计划
python3 propose_diff.py --report reports/最新.md --vessel-id <id> --dry-run
python3 propose_diff.py --report reports/最新.md --vessel-id <id> --apply   # 真切分支
```

## 定时（云机 crontab 示例）

见 `crontab.example`。体检可无人；`--apply` 提 diff 建议人工确认后再跑。

## 分级

- `live` — 还在跑 / 实验组有流量（非全量）→ 不管
- `candidate_solidify` — **已全量**：去门闸、留赢家行为的修订候选
- `candidate_bury` — 关量/结束，且能说清连着哪 → 可进删除候选
- `manual_only` — 图穿不过（枚举/反射）或配置保护 → 不自动改
- `unknown` — 查不到实验 → 标未知
