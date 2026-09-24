# 首页免息卡实验触发时机

## decision
免息卡相关实验/弹层规则：**每次进入首页（请求资源位）都会触发**，不是「只有第一次」。  
落点：`HomeInterestFreeCardHomePopupFilterRuleProcessor.hitRule`（每次请求首页资源位配置时执行）。

## mouths
- ec-api
- homepage / 资源位

## anchors
- symbols:
  - `HomeInterestFreeCardHomePopupFilterRuleProcessor`
  - `HomeInterestFreeCardService.isEligibleUser`

## constraints
- 排查「为啥又进组/又弹」不要按「仅首访」假设
- 群体/版本门槛在 `isEligibleUser` 内收敛

## evidence
- Cursor×ec-1 · transcript `43e21908-83ba-4ff8-b453-09672c85eea2`

## status
active

## updated
2026-09-23
