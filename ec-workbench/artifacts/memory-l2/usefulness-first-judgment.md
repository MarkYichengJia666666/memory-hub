# Memory 有用性验收 · 第一条 L2

- 身份：team `team-25ax2ljxko` / agent `agt-25ay1hg9ya`
- 条目：`ec/coupon/open-app-grant.md`
- 问法：开屏灌券改发券逻辑应该动哪？
- 召回决策：是（Kafka + 勿只改 Controller + 符号锚点）
- 检查：{'提到Kafka': True, '提到勿只改Controller': True, '有符号锚点': True, 'status=active': True}

结论：换会话不重讲背景，也能从 Memory 拿到「发券在 Kafka」这条判决 → **第一条 Memory 证明有用**。
