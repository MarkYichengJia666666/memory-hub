# 站内信翻译错误：整句当 TT 原文

## decision
部分站内信报「翻译错误」，常见根因不是日期文案本身，而是：**入库时把整句（含具体日期）当成要去翻译平台查的原文**，平台没有这条 key → 用户打开消息列表时 `TT.gen` 查翻译失败 → 接口报错。
站内信要区分「可翻译模板 key」与「已拼好的最终文案」两条链路。

## mouths
- notification / 站内信
- ec-api

## anchors
- symbols: `TT.gen`, `needTranslate`
- 现象：消息列表接口因翻译失败报错

## constraints
- 带动态日期/变量的句子不要整句当翻译 key 入库
- 排查翻译错误先分清：模板翻译链路 vs 已渲染文案直出链路

## evidence
- Cursor×ec · transcript `f57df63c`

## status
active

## updated
2026-09-24
