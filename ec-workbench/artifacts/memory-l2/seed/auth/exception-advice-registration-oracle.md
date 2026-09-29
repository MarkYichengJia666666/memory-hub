# 全局异常 Advice：code 差异可成「是否注册」探测口

## decision
`EcControllerExceptionAdvice` 对 WARNING 级异常直出 code+message，对 ERROR 级常换通用文案但**仍暴露 code**。像 `USER_DOES_NOT_EXIST` / 自查还款相关「用户不存在」等，未登录打公开接口时，已注册与未注册若 code/后续行为可区分，即构成**手机号是否注册的判别 oracle**。
治理重点不是「藏住枚举名」 alone，而是让已注册/未注册**整体响应尽量不可区分**，或加限流/验证码抬高探测成本；客户端必然有 code 映射，反编译后数字 code 同样可枚举。

## workspace
- （安全/账户接口横切；非单一 TAPD）

## mouths
- 全局异常 / EcException
- 注册探测

## anchors
- symbols: `EcControllerExceptionAdvice`
- symptoms: 未登录固定 `code=3003` 等与已注册路径分叉

## constraints
- 勿以为「ERROR 只改 message」就消除枚举风险
- 改响应前评估客户端依赖的业务 code

## evidence
- Claude×ec-01 · session `b7220538`

## status
active

## updated
2026-09-28
