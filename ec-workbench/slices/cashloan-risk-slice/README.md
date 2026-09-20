# 现金贷风控切片（CodeGraph）

授信提交 / 撞库 / riskprocessor 主干 / 订单风控步进。含 antifraud 入口，不含质检全家桶。

图：`cg-1z1tgq4n`（本机 KS `http://127.0.0.1:8424`，header `x-tdai-service-id: default`）。

其它图：发券开屏 `cg-pl6vfmyi`；首页下单 `cg-b0xmzi3v`；还款 `cg-locevwhf`。别混。

## 门

```
C 端  CheckRegisteredController（撞库）
      RiskAccountController / RiskReuploadController
      （submitRisk 在 LoanAccountController，本切片未整拷该冻结大类）
内部  RiskController（/ecInternalApi/risk）
      ApiChannelCreditController / ApiChannelCollisionController
      AntiFraudController
运营  RiskFlowController / RiskPreCheckController / BatchTriggerRiskController
Kafka EasycashRiskProcessor / LoanAccountRiskProcessor / RiskCashLoanEventProcessor
Job   RiskAutoSubmitCalcCreditsJob / RiskBatchTrigger* / RiskIncreaseCreditsJob
      TriggerPayoutOrRepaymentMultiLoanRiskTypeJob（还款→风控桥）
```

核心：`RiskFacadeService`、`RiskApplicationSubmitService`、`RiskProcessorFactory` / `BaseRiskProcessor`、`CollisionOrchestrator`、`LoanUserRiskTraceService`、`OrderRiskControlStepProcessor`。

## 没拷进来的（问了会空，不是没有）

- `LoanAccountController` 全文（只记符号 `submitRisk`）
- jOOQ generated、外部 Mesh / riskflow 客户端
- FraudAggregation 十余个 ReviewCase、antifraudquality、实验分流小枝
- 额度落地旁路（`CashLoanCreditsService` 等）未全拷

枚举找 processor、`callers` 空 ≠ 死代码。图只答谁连着谁。
