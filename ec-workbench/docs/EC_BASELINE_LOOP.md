# EC 基线差分 Loop（本机）

接上「新 commit」：发现 EC `HEAD` 相对上次基线有变化 → **切片 refresh + 邻域扩拷** → CodeGraph sync（尽力）→ Memory 保鲜抽检（只报告）→ Stitch B 扫反射/枚举（`--merge-safe`）→ 写下新基线。

**不自动改 Hub L2**；miss 需人审后 `superseded` + `seed-l2-to-hub`。

## 手动

```bash
# HEAD 相对 state/last-ec-baseline 有变才跑
./ec-workbench/bin/on-ec-baseline-change

# 强制跑（发版日 / 验证）
./ec-workbench/bin/on-ec-baseline-change --force

# 只保鲜、不 sync 图
SKIP_SYNC=1 ./ec-workbench/bin/on-ec-baseline-change --force

# 不扩拷切片
SKIP_SLICE=1 ./ec-workbench/bin/on-ec-baseline-change --force

# 不扫 stitch
SKIP_STITCH=1 ./ec-workbench/bin/on-ec-baseline-change --force

# 扫 stitch 但只出候选、不写 overlay
STITCH_NO_MERGE=1 ./ec-workbench/bin/on-ec-baseline-change --force

# 单独保鲜
./ec-workbench/bin/freshness-check --limit 20

# 单独切片同步（dry-run）
./ec-workbench/bin/sync-slice-from-ec --dry-run --from-sha <prev> --to-sha HEAD
```

环境变量：`EC_REPO`（默认 `/Users/lipeng/IdeaProjects/ec`）、`SLICE`、`CG_ID`、`KS`、`FRESHNESS_LIMIT`、`STITCH_ROOT`（默认同 `EC_REPO`）。

## 切片扩拷规则（`bin/sync-slice-from-ec`）

| 动作 | 规则 |
|---|---|
| refresh | 切片已有 `*.java`，EC 对应路径内容不同 → 覆盖 |
| expand | 基线..HEAD 内新增/修改的 Java，**父目录已在切片** → 拷入 |
| skipped_new | 新路径无邻域 → 只报告，不自动进图 |
| missing / deleted | 切片有 EC 无、或区间内删除 → 只报告，不删切片文件 |

报告：`artifacts/slice-sync/slice-sync-*.md`

## 定时（launchd，每天 10:30）

```bash
./ec-workbench/bin/install-ec-baseline-launchd
# 卸载
./ec-workbench/bin/install-ec-baseline-launchd --uninstall
```

日志：`ec-workbench/state/logs/`  
基线：`ec-workbench/state/last-ec-baseline`

## 注意

1. **切片**先本地扩，再 **sync** 重建 `cg-zp42c34n`；无邻域的新业务嘴仍需人手拷进切片。  
2. **保鲜** 对照的是本机完整 `EC_REPO` 树——这才是 Memory 防过期的闸。  
3. cron 等价：`30 10 * * * /path/to/ec-workbench/bin/on-ec-baseline-change`
