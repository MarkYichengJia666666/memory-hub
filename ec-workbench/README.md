# EC × Memory Hub 工作台资料

本目录是 Memory Hub CodeGraph / Capillary / 切片建图调研的**工作资料备份**，与上层 Memory Hub 产品代码一起放在本仓库。

## 目录

| 路径 | 说明 |
| --- | --- |
| `slices/` | EC 业务切片与合并切片（Java 源码子集，非全仓） |
| `slices/ec-cashloan-combined-slice/` | 合并切片，对应图 `cg-zp42c34n` |
| `slices/exp-ligation-slice/tools/capillary/` | Capillary Agent（体检+人闸） |
| `docs/` | 能力看板 canvas、成果说明 |
| `artifacts/` | token 对照、capillary 报告与计划 |

## 注意

- **已纳入** TLS 自签证书（`slices/exp-ligation-slice/.certs/*.pem`）——仅供本机 git HTTPS 复现，勿用于生产。
- **已纳入** 实验缓存（`tools/capillary/state/experiment_*.json` 与 `artifacts/experiment-cache/`），可能含白名单用户 ID。
- **已纳入** 合并图索引 `cg-zp42c34n` 的 `.codegraph/`（约 70MB；`artifacts/codegraph-indexes/` 与合并切片下各一份）。
- 切片含公司业务代码片段：仓库若为 **Public**，请尽快改为 **Private**。

## 本机复现要点

1. 起 Memory Hub KS（如 `:8424`）
2. 用 HTTPS git 服务暴露 `slices/`（见 `exp-ligation-slice/.certs/git_https_server.py`）
3. create/sync CodeGraph → `cg-zp42c34n`
4. Capillary：`cd slices/exp-ligation-slice/tools/capillary && python3 run_scan.py`
