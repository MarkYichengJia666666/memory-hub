package com.yqg.core.userflow.infrastructure.adapter.vo;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.service.abtest.enums.ExperimentPlatformExperimentStatus;
import com.yqg.experiment.common.domain.vo.ExperimentDataVO;
import com.yqg.experiment.common.enums.ExperimentGroupTypeEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * userflow 实验 adapter 内部映射：保留实验中台 {@link ExperimentDataVO} 原始字段；
 * 分组结果收敛由 {@link com.yqg.core.userflow.infrastructure.adapter.impl.ExperimentAdapter} 负责。
 */
public class ExperimentAdapterResultVO {

  public Integer errorCode;
  public String message;
  public String experimentName;
  public String experimentStatus;
  public String groupResult;
  public ExperimentGroupTypeEnum groupType;
  public Long groupId;
  public String experimentStatusDetail;
  public String fullOrZeroGroupResult;
  public String groupParam;
  public String groupDesc;
  public String groupData;
  public String experimentWhiteList;
  public String laneWhiteList;
  public String hitLaneBlankName;

  /**
   * 仅 {@link #experimentWhiteList}，不含 {@link #laneWhiteList}。
   */
  public boolean isExperimentWhiteList() {
    return BooleanType.TRUE.name().equals(experimentWhiteList);
  }

  /**
   * 实验是否已全量（占比态 = 已全量 FP）。
   *
   * <p>占比态以 {@link #experimentStatusDetail} 为准而非 {@link #experimentStatus}：线上已观测到
   * {@code experimentStatus="R"} 但 {@code experimentStatusDetail="ZP"} 的样本——前者是实验整体运营态
   * （R/C/P/L），后者才是当前真实占比态（R/FP/ZP/L/P/C）。
   */
  public boolean isExperimentFullPercentage() {
    return resolveStatusDetail() == ExperimentPlatformExperimentStatus.FULL_PERCENTAGE;
  }

  /**
   * 实验是否已关量或已关闭（占比态 = 已关量 ZP 或 已结束 C）。
   *
   * <p>占比态以 {@link #experimentStatusDetail} 为准而非 {@link #experimentStatus}，原因同
   * {@link #isExperimentFullPercentage()}。
   */
  public boolean isExperimentZeroOrClosed() {
    ExperimentPlatformExperimentStatus detail = resolveStatusDetail();
    return detail == ExperimentPlatformExperimentStatus.ZERO_PERCENTAGE
        || detail == ExperimentPlatformExperimentStatus.CLOSE;
  }

  private ExperimentPlatformExperimentStatus resolveStatusDetail() {

    if (StringUtils.isBlank(experimentStatusDetail)) {
      return null;
    }

    return ExperimentPlatformExperimentStatus.fromCodeOrNull(experimentStatusDetail);
  }

  public static ExperimentAdapterResultVO from(ExperimentDataVO vo) {
    if (vo == null) {
      return empty();
    }
    ExperimentAdapterResultVO result = new ExperimentAdapterResultVO();
    result.errorCode = vo.errorCode;
    result.message = vo.message;
    result.experimentName = vo.experimentName;
    result.experimentStatus = vo.experimentStatus;
    result.groupResult = vo.groupResult;
    result.groupType = vo.groupType;
    result.groupId = vo.groupId;
    result.experimentStatusDetail = vo.experimentStatusDetail;
    result.fullOrZeroGroupResult = vo.fullOrZeroGroupResult;
    result.groupParam = vo.groupParam;
    result.groupDesc = vo.groupDesc;
    result.groupData = vo.groupData;
    result.experimentWhiteList = vo.experimentWhiteList;
    result.laneWhiteList = vo.laneWhiteList;
    result.hitLaneBlankName = vo.hitLaneBlankName;
    return result;
  }

  public static ExperimentAdapterResultVO empty() {
    return new ExperimentAdapterResultVO();
  }
}
