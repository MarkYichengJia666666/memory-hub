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

- **不含** TLS 私钥（`.certs/*.pem` 已省略）。
- **不含** 实验平台带白名单用户 ID 的 live 缓存；需要时本地用 MCP 重拉。
- 图索引 `.codegraph/` 可再生成，未纳入。
- 切片含公司业务代码片段：若仓库为 **Public**，请尽快改为 **Private** 或移除 `slices/`。

## 本机复现要点

1. 起 Memory Hub KS（如 `:8424`）
2. 用 HTTPS git 服务暴露 `slices/`（见 `exp-ligation-slice/.certs/git_https_server.py`）
3. create/sync CodeGraph → `cg-zp42c34n`
4. Capillary：`cd slices/exp-ligation-slice/tools/capillary && python3 run_scan.py`
