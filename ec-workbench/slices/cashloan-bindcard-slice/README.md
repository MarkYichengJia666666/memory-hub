# 现金贷绑卡切片（CodeGraph）

现金贷绑卡 / 银行卡账户主链：C 端绑卡门、internal 绑卡请求、核心 `LoanBankAccountService`、鉴权步 `BindCardProcessor`、实验 `ExperimentNameSpace.SUPERBANK_CHANNEL_USER_BINDING_CARD`。

原独立图已并入合并图 `cg-zp42c34n`（仓 `ec-cashloan-combined-slice`）。本目录仍可作绑卡补文件素材。

## 门

```
C 端     LoanBankAccountController（ec-api）
内部     LoanBankAccountController / BindCardRequestController（ec-internal-api）
鉴权步   BindCardProcessor / AuthBindCardService / BindCardDelayService
核心     LoanBankAccountService / BindCardRequestService
实验     ExperimentNameSpace.SUPERBANK_CHANNEL_USER_BINDING_CARD
         → ExpFacade.fetchResultFallBackWithDefaultScene("technology-auth-abroad-loan-superbank_rename_0908", …)
Job      LoanBankAccountStatusUpdateJob
```

## 没拷进来的（问了会空）

- 理财 FinancingBankAccount* 大枝
- Admin 改名审核 / 全量支付 Provider
- jOOQ generated、超大依赖类（LoanAccountService 等）会在边界断链
- `com.yqg.common.util.type.BooleanType` 等外部包

图只答谁连着谁。实验是否关量另问平台。
