# Loop for myself · 个人判决 seed

路径：`seed-myself/<业务口>/<slug>.md` → Hub `ec/<业务口>/<slug>.md`  
身份：`.p0-memory-identity-myself.json`（team `ec-memory-myself`）

与 `seed/`（Loop for team）**物理隔离**：不同 team/agent，召回不会串台。

```bash
./ec-workbench/bin/seed-l2-auto-import --scope myself path/to.md
# 回执：artifacts/memory-l2/receipts/myself/latest.md
```
