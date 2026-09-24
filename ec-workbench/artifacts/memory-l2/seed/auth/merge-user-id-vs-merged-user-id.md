# 合并历史：MERGE_ 留存，MERGED_ 被删

## decision
`user_merge_history` 里：**`MERGE_USER_ID` = 合并后留存可用账号**；**`MERGED_USER_ID` = 被注销废弃账号**。
`UserMergeHistoryModel.insert(newUserId, oldUserId, …)` 参数名易误导：实际 `setMergeUserId(oldUserId)`、`setMergedUserId(newUserId)`——调用方传入的「被删」常落在第一参数。查合并关系先认字段语义，不要按参数名字面理解。

## mouths
- auth / 账户合并
- Flip / H5

## anchors
- symbols: `UserMergeHistoryModel#insert`, `UserMobileChangeService#doMergeAccount`
- columns: `MERGE_USER_ID`, `MERGED_USER_ID`
- reasons: `SELECTED`, `NOT_SELECTED`, …

## constraints
- SQL/排障用字段名，不用 insert 参数名脑补
- MERGE_* 始终留存侧，MERGED_* 始终被删侧

## evidence
- Cursor×ec · transcript `8a592838`
- verified vs EC `d103aeefa32 Merge branch 'release/20260922-5'` on 2026-09-24

## status
active

## updated
2026-09-24
