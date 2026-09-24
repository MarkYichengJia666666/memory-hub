# GoPay 完件：紧急联系人手机号无校验

## decision
完件信息里出现格式错误/过短的紧急联系人手机号（如 `011****48`），根因通常不是展示层格式化，而是 **GoPay 完件链路把上游回传号码写入完件 Mongo，且未做手机号合法性校验**。
能被「解析转换」的字符串就可能落到 `allContactInfo.immediateContact.mobilePhoneNo`，完件页再原样展示。

## mouths
- gopay / 完件
- mongo

## anchors
- field: `allContactInfo.immediateContact.mobilePhoneNo`
- related: `user_immediate_contact`, `loan_account_details_new.details_object_id`
- flows: `LOAN_MARKET_BASE` / `LOAN_MARKET_SUPPLEMENT`

## constraints
- 修展示不够，要在完件写入侧加校验或过滤
- 排查先对 Mongo 原文，不要先怪前端 mask

## evidence
- Cursor×ec · transcript `c041e371`

## status
active

## updated
2026-09-24
