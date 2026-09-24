# Lazada：额度变更推 API 渠道 ≠ 所有额度变更

## decision
Lazada「额度变更推送到 API 渠道」**不是都会推**。  
当前代码里，只有走**风控结果推送链路**才会落到 `LazadaBuyerApiChannelPushProvider#pushCreditModificationResultToApiChannel`。

## mouths
- kafka / observer
- api-channel

## anchors
- symbols:
  - `LoanMarketObserver#processRiskEvent`
  - `ChannelPushService#pushRiskResult` / `pushCalcCreditRiskResult` / `pushOrderRiskResult`
  - `LazadaBuyerApiChannelPushProvider#pushCreditModificationResultToApiChannel`
  - `ApiChannelCallBackService#callBackCreditModificationToApiChannel`

## constraints
- 排查「为啥没推 Lazada」先看是否走过风控结果推送，不要假设所有额度变更都回调渠道

## evidence
- Cursor×ec-1 · transcript `dedc92fa-e126-4017-b5ce-36429001c5fb`

## status
active

## updated
2026-09-23
