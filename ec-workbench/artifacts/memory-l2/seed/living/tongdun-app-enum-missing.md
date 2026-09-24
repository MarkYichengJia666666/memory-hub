# LivingSource：TONGDUN_APP 不在枚举里

## decision
ORDER_CHECK / 完件链路若 Mongo 用户资料 `livingInfo.loanUserLivingSource = "TONGDUN_APP"`，而后端枚举 `LoanUserLivingSource` **不包含** `TONGDUN_APP`，Jackson 反序列化 `LoanAccountDetailsPojo` 会失败，流程中断。
排障时先对「库里的 livingSource 字符串 ⊆ 代码枚举」。

## mouths
- kafka-consumer / ORDER_CHECK
- living / 完件

## anchors
- symbols: `LoanUserLivingSource`, `LoanAccountDetailsPojo`, `BizCheckEventProcessor`
- bad value example: `TONGDUN_APP`

## constraints
- 新增活体来源必须先加枚举再放量写 Mongo
- 不要只看业务日志，要看反序列化/枚举异常

## evidence
- Cursor×ec · transcript `32e95c80`

## status
active

## updated
2026-09-24
