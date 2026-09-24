# 下游同步：INIT/RESERVE 不是 READY

## decision
`CASH_LOAN_EVENT_DOWNSTREAM_SYNC` 技术上可在 `onOrderInitial` / `onOrderReserved` 挂回调（Kafka 已通）。真正风险在**业务语义与下游契约**：
1. 不是每笔单都走齐 RESERVE→INIT→READY；
2. 早推后若 `REJECT` 不推，下游会留「未放款却存在」脏数据；
3. **禁止**复用 `doSyncOrderReadyAsync` 却仍写 `eventType=ORDER_READY`——下游会按放款成功理解；
4. 同单可能连推 2～3 次，依赖下游按主键 upsert。

## mouths
- order / 下游同步
- kafka

## anchors
- topic/event: `CASH_LOAN_EVENT_DOWNSTREAM_SYNC`, `ORDER_INITIALIZED`, `ORDER_RESERVED`, `ORDER_READY`
- symbols: `BaseCashLoanEventProcessor`, `onOrderInitial`, `onOrderReady`, `doSyncOrderReadyAsync`

## constraints
- 早推必须新 eventType，并与下游确认未放款语义
- 有 INIT 推就应考虑 REJECT 同步，否则脏数据

## evidence
- Cursor×ec · transcript `e64eec9f`

## status
active

## updated
2026-09-24
