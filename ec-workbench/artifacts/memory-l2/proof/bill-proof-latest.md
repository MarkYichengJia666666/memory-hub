# Memory ON vs OFF · 对话上下文 / Proxy 账单 · 20260930-111101

mode=`live` · encoder=`approx/len÷4` · model=`deepseek-v4-flash-0731`

Memory ON=system+scenario/read(L2)+问；Memory OFF=system+入口类 Java 全文+问。计量：Proxy usage + 墙钟（prepare_ms + llm_ms）。上游不可用时仅 context token。

| 题 | Mem tok | 搜仓 tok | Mem ms | 搜仓 ms | 省 ms | live Mem | live 搜仓 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Q1-saqu | 252 | 26348 | 3429 | 5280 | 1851 | 4972 | 30069 |
| Q2-merge | 323 | 27595 | 2122 | 4376 | 2254 | 4981 | 33949 |
| Q3-h5-btn | 333 | 4962 | 3479 | 4200 | 721 | 5101 | 10267 |
| Q4-startup | 334 | 2710 | 2947 | 4284 | 1337 | 5072 | 7244 |

**合计上下文** Memory **1242** vs 搜仓 **61615**（≈**2.0%**，省 **60373**）· live_ok=8
**合计 live usage** Memory **20126** vs 搜仓 **81529**（≈**24.7%**，省 **61403**）
**合计墙钟** Memory **11977 ms** vs 搜仓 **18140 ms**（≈**66.0%**，省 **6163 ms**，约 **1.51×**）
  · 其中 LLM round-trip：Mem 11952 ms / 搜仓 14753 ms

> 墙钟=本机 prepare（读 L2 / 读 Java）+ Proxy LLM round-trip；未计多轮 grep 试错。completion 两端同 max_tokens=256。live 依赖 all-in-one-ai；502 时 mode=context_only。

