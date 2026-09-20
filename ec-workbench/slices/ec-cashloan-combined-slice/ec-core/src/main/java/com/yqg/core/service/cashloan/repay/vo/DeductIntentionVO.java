package com.yqg.core.service.cashloan.repay.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.model.generated.tables.records.DeductIntentionLogRecord;
import com.yqg.core.service.cashloan.repay.enums.DeductIntentionStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.ec.common.serialization.JsonUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeductIntentionVO {
  public Long id;
  public Long userId;
  public List<UnionRepaymentPlanVO> deductPlan;
  public DeductIntentionStatus deductIntentionStatus;
  public Long timeCreated;
  public Long timeUpdated;

  public static DeductIntentionVO from(DeductIntentionLogRecord record) {
    if (Objects.isNull(record)) {
      return null;
    }
    DeductIntentionVO vo = new DeductIntentionVO();
    vo.id = record.getId();
    vo.userId = record.getUserId();
    vo.deductPlan = parseAndSortRepaymentPlans(record.getDeductPlan());
    vo.deductIntentionStatus = DeductIntentionStatus.valueOf(record.getDeductStatus());
    vo.timeCreated = record.getTimeCreated();
    vo.timeUpdated = record.getTimeUpdated();
    return vo;
  }

  // 辅助方法1: 解析并排序还款计划
  private static List<UnionRepaymentPlanVO> parseAndSortRepaymentPlans(String deductPlan) {
    return JsonUtils.fromOrException(deductPlan,
            new TypeReference<List<UnionRepaymentPlanVO>>() {
            })
        .stream()
        .sorted(Comparator.comparingInt(UnionRepaymentPlanVO::getWeight).reversed())
        .collect(Collectors.toList());
  }

  public static boolean existJbpPlan(String deductPlan) {
    return parseAndSortRepaymentPlans(deductPlan).stream()
        .anyMatch(vo -> vo.unionRepaymentType == UnionRepaymentType.JBP);
  }

  public static boolean existInitPlan(DeductIntentionVO deductIntentionVO) {
    return Objects.nonNull(deductIntentionVO) && deductIntentionVO.deductIntentionStatus ==  DeductIntentionStatus.INIT;
  }
}