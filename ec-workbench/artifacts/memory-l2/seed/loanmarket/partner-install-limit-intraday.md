# 贷超合作方：install_limit 日内要实时下线

## decision
合作方 `install_limit` 配置本身可能是对的，但若**安装事件只计数、状态只靠每日 0 点 Job 改**，则日内限量基本拦不住（会出现远超额度的安装量）。
修复方向：安装事件计数达标后**立即**触发限量下线（Kafka consumer 链路），不能只依赖日切 Job。
发布时 **ec-kafka-consumer 必须发**，否则实时逻辑不生效。

## mouths
- loan-market / partner
- kafka-consumer

## anchors
- config: `install_limit`, `online_status`
- symbols: `LoanMarketPartnerEventObserver`, `LoanMarketPartnerStatusJob`, `LoanMarketPartnerNotificationService`
- topic: `loan_market_partner_event`

## constraints
- 排障「超限量」先分：配置错 vs 日内未实时下线
- 只发 ec-api 不够，实时限量在 consumer

## evidence
- Cursor×ec · transcript `6adffc43`

## status
active

## updated
2026-09-24
