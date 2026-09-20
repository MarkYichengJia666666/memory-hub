# 现金贷还款切片（CodeGraph）

还款主链：拿还款账户 / 算金额 / 发起还款 / 代扣与第三方扫单 / 还款事件。

图：`cg-locevwhf`（本机 KS `http://127.0.0.1:8424`，header `x-tdai-service-id: default`）。

其它图：发券开屏 `cg-pl6vfmyi`；首页下单 `cg-b0xmzi3v`；风控 `cg-1z1tgq4n`。别混。

## 门

```
C 端  CashLoanRepaymentController / JbpRepaymentController
内部  EcCashLoanRepaymentController（/ecInternalApi/repayment）
      ApiChannelRepaymentController（渠道）
      PaymentReceiptAccountCallbackController（账户回写）
运营  RepaymentManageController / RepaymentRouteController
Kafka DownstreamOrderRepaySyncProcessor / FundRepayEventProcessor / EcCombinedRepayEventProcessor
Job   DirectDebitInit/Execute*Repayment* / CashLoanRepayStrategyStatusJob / ThirdPartyRepaymentScanJob
```

核心：`UnionRepaymentService`、`CashLoanRepaymentService`、`RepaymentAccountService`、`RepaymentAccountFactory`、`DirectDebitRepaymentService`。

## 没拷进来的（问了会空，不是没有）

- jOOQ `generated/` 表
- `CashLoanService` / `LoanAccountService` 等冻结大类（链会在边界断）
- 各支付 Provider、首页 `*Repayment*Processor` UI 卡片族
- 还款实验小枝、once 回填 Job

图只答谁连着谁。调度 cron、topic 配置、线上是否还在跑，另问。
