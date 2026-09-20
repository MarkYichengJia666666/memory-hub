# 现金贷下单切片（CodeGraph 第二块）

首页主路径 + 下单主路径。首页 processor / selector 已整包收进（这层最复杂、explore 最烧 token）。不收十四个订单 Observer 实现。

第一块（发券 / 开屏 / 资源位）在旁边的 `exp-ligation-slice`，图 `cg-pl6vfmyi`。问开屏发券走那边。

## 门

```
C 端 GET /api/v5/cashloan/home     IDNHomepageV5Controller
C 端 POST /api/v2/cashloan/createOrder   CashLoanController.createOrderV2
内部 POST .../createOrder          ApiChannelOrderController（端外/渠道）
```

首页：安全检查 → 实验 → 批量查库 → 确认状态 → processor 链。具体谁填哪块 UI，在 `newhomepage/` 里，图上能搜到类名。枚举找实现仍可能穿不过，标手工。

下单：`createOrder` → `CashLoanService.createOrder`（冻结大类）→ `OrderPreCheckService`。额度降低快下走 `CashLoanUserSupplementCreateOrderService`。

Kafka：`CashLoanEventService` 往外发订单事件。谁消费没拷全，消费拉起靠配置，标手工。

Job：wiki 写过快下过期 Job，当前仓里没找到对应 `*Job`，这张图记「没有」。

## 没拷进来的（问了会空，不是没有）

- `HomepageV5Config` 里再长出来的新功能（冻结，禁止往里面加）
- 十四个 `ICashLoanOrderObserver` 实现
- 还款整条链 → 旁仓 `cashloan-repay-slice`，图 `cg-locevwhf`
- 风控 / 撞库 → 旁仓 `cashloan-risk-slice`，图 `cg-1z1tgq4n`
- 资方、电签整条链（仍未切）
