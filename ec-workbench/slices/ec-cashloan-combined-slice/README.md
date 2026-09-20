# EC cashloan 合并切片（一仓一图）

本机唯一 CodeGraph：`cg-zp42c34n`（KS `http://127.0.0.1:8424`，`x-tdai-service-id: default`）。

规模约 712 files / 23629 nodes / 38576 edges（以 Hub status 为准）。

## 怎么来的

由下列业务切片**源码并集**组成（路径保持 EC 相对路径）：

| 原切片 | 业务 |
|---|---|
| `exp-ligation-slice` | 发券 / 开屏 / 资源位 |
| `cashloan-order-slice` | 首页 + 下单 |
| `cashloan-repay-slice` | 还款 |
| `cashloan-risk-slice` | 风控 |
| `cashloan-bindcard-slice` | 绑卡 |

原五张图已删；查询统一用本图。原切片仓可继续当「按嘴补文件」的素材，改完再 rsync/拷进本仓并 `sync`。

## 更新流程

```text
EC 有改动 → 拷进对应业务源切片（可选）→ 合并进本仓 commit
  → POST /v3/code-graph/sync { code_graph_id: cg-zp42c34n }
  → ready 后再查
```
