# Memory L2 seed 目录约定

## 双维度

| 维度 | 落在哪 | 作用 |
|------|--------|------|
| **业务口**（主路径） | `seed/<biz>/<slug>.md` → Memory `ec/<biz>/<slug>.md` | 跨需求复用、按口召回；与历史存量一致 |
| **SDD 工作区**（正文字段） | 每条里的 `## workspace` | 追溯判决所属 `specs/<TAPD-…>`，可一对多 |

业务口示例：`auth` / `homepage` / `order` / `marketing` / `bindcard` / `coupon` / `experiment` / `ops` …

```markdown
## workspace
- `TAPD-378150-zero-interest-360-lottery`（与 EC `specs/<workspace>` 同名）
```

一条判决只挂**一个**主业务口（路径）；若跨工作区，在 `## workspace` 列多个 specs。跨口通用流程放 `ops/`。

## 怎么从 SDD 拆到业务口

看判决的**主落点**（符号/路由/页面），不要用 TAPD 号当目录名：

| 判决落点 | 业务口 |
|----------|--------|
| 登录轮播 / 首页资源位 / KTP 扑脸 | `homepage` |
| 下单页 productDetail / 绿卡角标 | `order` |
| 万一/千一承接、MaterialTag、未归因实验 Key | `marketing` |
| 账户合并 / 完件 / 登录态 | `auth` |
| 实验平台通用分流（非某承接） | `experiment` |
| 绑卡资方 | `bindcard` |
| 会话回写 / Hub 运维 | `ops` |

不确定时：用 `## mouths` 里更稳定的那个词作目录名，并与同目录已有 seed 对齐。
