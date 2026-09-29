# Graph Stitch Overlay（反射/枚举补丁边）

CodeGraph 看不见 `SpringUtils.getInstance(getProcessorClass())`、枚举 Factory、Aviator。  
**不改引擎**：人维护 yaml 边，查询与 Capillary 与图结果合并。

## 路径

| 文件 | 作用 |
|---|---|
| `artifacts/graph-stitch/overlay.yaml` | 补丁边清单（证据链） |
| `bin/callers-with-stitch <符号>` | 图 callers ∪ overlay |
| `bin/extract-stitch-candidates` | **B**：扫 Java；`--merge-safe` 写入固定写法 |
| Capillary `scan.py` | 分类前读同一份；stitch 命中且 callers 空 → `manual_only` |

## 用法

```bash
./ec-workbench/bin/callers-with-stitch AppStartUpGrantCouponProcessor
./ec-workbench/bin/extract-stitch-candidates
./ec-workbench/bin/extract-stitch-candidates --root "$EC_REPO" --merge-safe
# 报告：artifacts/graph-stitch/candidates-*.md
```

B 规则（新 commit 由 `on-ec-baseline-change` 扫完整 `EC_REPO`）：

| pattern | 写法 | merge-safe |
|---|---|---|
| Kafka `getProcessorClass` + pool | `return Xxx.class` | 是 |
| `getRuleType` | 资源位枚举 | 是 |
| 首页 `get*ProcessorType` | Processor / Alert / UserInfo / Notice / Point / Product | 是 |
| `get_any_class_literal` | 其它 `get*Class()` `return Xxx.class` | 是 |
| `generic_enum_dispatch` | Processor 上其它 `get*()` 返回枚举 | 仅 `from` 已是 Factory |
| `spring_class_literal` | `getInstance(Foo.class)` | 仅 `*Processor`/`*Service` |
| `class_for_name` / 动态 `getInstance` | 字符串 / 表达式参数 | **否** |

基线：`SKIP_STITCH=1` 跳过；`STITCH_NO_MERGE=1` 只报告不写 overlay。
