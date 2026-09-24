# Kafka Consumer 起不来：topic_config 枚举缺失

## decision
测试/生产 `indo-cashloan-ec-kafka-consumer` 起不来时，先查 ZK/配置 `kafka.topic_config` 是否出现了**镜像代码里没有的 `ConsumerGroup` 枚举值**。
已证实案例：配置出现 `NEWISH_HEAD_LIMIT_COUPON_GRANT`，但镜像 `ConsumerGroup` 无此枚举 → `ConsumerPoolService` 反序列化失败 → Spring Boot 直接挂掉。

## mouths
- kafka-consumer
- config / ZK

## anchors
- config: `kafka.topic_config`
- symbols: `ConsumerGroup`, `ConsumerPoolService`
- 案例枚举：`NEWISH_HEAD_LIMIT_COUPON_GRANT`

## constraints
- 先对齐「配置枚举 ⊆ 代码枚举」，再查业务逻辑
- 加新 ConsumerGroup 必须配置与发版镜像同步上线

## evidence
- Cursor×ec · transcript `b872ad7a`

## status
active

## updated
2026-09-24
