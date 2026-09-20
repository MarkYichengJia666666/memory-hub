package com.yqg.core.service.coupongrantrule;

import com.google.common.collect.Lists;
import com.yqg.core.model.sql.enums.AvailabilityStatus;
import com.yqg.core.service.coupongrantrule.enums.CouponGrantRulePlatformType;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantRuleVO;
import com.yqg.core.service.financing.coupon.FinancingCouponConfigService;
import com.yqg.core.service.financing.vo.FinancingCouponConfigVO;
import com.yqg.core.service.loan.coupon.LoanCouponConfigService;
import com.yqg.core.service.loan.coupon.vo.LoanCouponConfigVO;
import com.yqg.core.service.notif.CouponGrantConfig;
import com.yqg.core.service.notif.NotifCouponGrantTaskService;
import com.yqg.core.service.notif.vo.CouponExperimentResultVO;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskVO;
import com.yqg.ec.common.enums.NotifCouponGrantTaskSourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.utils.EcAsserts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author fudongyi
 * @date 2022/8/23
 */
@Slf4j
@Service
public class CouponGrantService {
  @Autowired
  private LoanCouponConfigService loanCouponConfigService;
  @Autowired
  private CouponGrantRuleService couponGrantRuleService;
  @Autowired
  private NotifCouponGrantTaskService notifCouponGrantTaskService;
  @Autowired
  private FinancingCouponConfigService financingCouponConfigService;
  @Autowired
  private CouponGrantConfig couponGrantConfig;

  /**
   * userIds 都只传入一个用户
   * @param couponConfigId
   * @param userIds
   * @param sourceId
   * @param sourceType
   * @param platformType
   * @param batchNo
   * @param desc
   * @return
   */
  public List<Long> grantCouponByCouponConfigId(Long couponConfigId, Collection<Long> userIds, Long sourceId, NotifCouponGrantTaskSourceType sourceType, CouponGrantRulePlatformType platformType, String batchNo, String desc) {
    List<Long> couponIds = Lists.newArrayList();
    CouponGrantRuleVO grantRuleVO = couponGrantRuleService.getByName(fetchConfigName(couponConfigId, platformType));
    EcAsserts.assertNotNull(grantRuleVO, "Coupon grant rule cannot be null, with couponConfigId = {}, platformType = {}", couponConfigId, platformType);
    EcAsserts.assertTrue(grantRuleVO.platformType == platformType, "PlatformType is incorrect, with couponConfigId = {}, couponGrantVO.platformType = {}, given platformType = {}", couponConfigId, grantRuleVO.platformType, platformType);
    EcAsserts.assertTrue(grantRuleVO.status == AvailabilityStatus.ENABLED, "Coupon grant rule is disabled, with couponConfigId = {}, platformType = {}", couponConfigId, platformType);
    userIds.forEach(userId -> couponIds.addAll(grantCouponByRuleId(grantRuleVO.id, sourceId, sourceType, null, platformType, userId, batchNo, desc)));
    return couponIds;
  }

  public List<Long> grantCouponByRuleId(Long ruleConfigId, Long sourceId, NotifCouponGrantTaskSourceType sourceType, Long mediumId, CouponGrantRulePlatformType platformType, Long userId, String batchNo, String extraData) {
    // 调用底层统一的长期实验判断方法，确保上下层逻辑一致
    CouponExperimentResultVO experimentResult = notifCouponGrantTaskService.checkCouponLongTermExperimentForUpper(
        ruleConfigId, userId, platformType, sourceType);
    boolean canGrantCoupon = experimentResult.isShouldGrantCoupon();

    // 开关控制：在上层执行长期实验判断
    if (couponGrantConfig.isLongTermExpCheckAtUpperLayer()) {
      if (!canGrantCoupon) {
        log.info("Coupon grant blocked by long term experiment at upper layer, ruleConfigId={}, userId={}, sourceType={}",
            ruleConfigId, userId, sourceType);
        return Lists.newArrayList();
      }
    }

    return notifCouponGrantTaskService.grantCouponInstant(ruleConfigId, sourceId, sourceType, mediumId, platformType, userId, batchNo, extraData, canGrantCoupon);
  }

  public boolean hasCreatedGrantedTask(Long ruleId, Long userId, Long sourceId, NotifCouponGrantTaskSourceType sourceType) {
    if (Objects.isNull(userId) || Objects.isNull(ruleId)) {
      return false;
    }
    List<NotifCouponGrantTaskVO> vos = notifCouponGrantTaskService.listByRuleIdAndUserIdAndSourceIdAndSourceType(ruleId, userId, sourceId, sourceType);
    return CollectionUtils.isNotEmpty(vos);
  }

  private String fetchConfigName(Long couponConfigId, CouponGrantRulePlatformType platformType) {
    switch (platformType) {
      case LOAN:
        LoanCouponConfigVO configVO = loanCouponConfigService.findByIdOrThrow(couponConfigId);
        return configVO.name;
      case FINANCING:
        FinancingCouponConfigVO financingCouponConfigVO = financingCouponConfigService.getCouponConfigById(couponConfigId);
        return financingCouponConfigVO.name;
      default:
        throw EcException.error("未知的发券方式.PlatformType:{}", platformType);
    }
  }

}
