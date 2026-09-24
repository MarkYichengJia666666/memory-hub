# 训练第三批（第二扫描轮）· 2026-09-24

新增 6 条；业务 L2 合计约 **21**。

| path | 一句话 |
|---|---|
| `ec/living/tongdun-app-enum-missing.md` | TONGDUN_APP 不在枚举 → 反序列化挂 ORDER_CHECK |
| `ec/auth/trigger-auth-completed-before-persist.md` | triggerAuthCompleted 早于落库，用 context.channel |
| `ec/db/kyc-encrypt-is-status-table.md` | KYC 加密表是状态表，不能当事件流删 |
| `ec/homepage/display-strategy-enum-crash.md` | HomeDisplayStrategy 缺枚举崩首页 |
| `ec/gopay/contact-phone-no-validate-on-finish.md` | GoPay 完件联系人手机号无校验 |
| `ec/coupon/orthogonal-anti-settle-exp.md` | 下单页样式可能来自正交防结清实验 |

均已 write + L0；镜像在 `seed/`。
