package com.miyou.controllers.cashloan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.sql.enums.AvailabilityStatus;
import com.yqg.core.service.coupongrantrule.CouponGrantRuleService;
import com.yqg.core.service.coupongrantrule.CouponGrantService;
import com.yqg.core.service.coupongrantrule.config.BaseRuleGrantConfig;
import com.yqg.core.service.coupongrantrule.enums.CouponGrantRulePlatformType;
import com.yqg.core.service.coupongrantrule.rules.BaseCouponGrantRule;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantRuleSearchCondition;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantRuleVO;
import com.yqg.core.service.financing.coupon.FinancingCouponConfigService;
import com.yqg.core.service.financing.vo.FinancingCouponConfigVO;
import com.yqg.core.service.loan.coupon.LoanCouponConfigService;
import com.yqg.core.service.loan.coupon.vo.LoanCouponConfigVO;
import com.yqg.core.service.notification.vo.NotifParamVO;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.enums.ICouponUsageType;
import com.yqg.ec.common.enums.NotifCouponGrantTaskSourceType;
import com.yqg.ec.common.enums.PlatformType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.request.*;
import com.yqg.ec.common.spring.response.*;
import com.yqg.mc.common.enums.NotifParamType;
import com.yqg.mc.common.vo.NotifRawParamVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fudongyi
 * @date 2023/1/3
 */
@Slf4j
@RestController
public class CouponGrantRuleController {
  @Autowired
  private CouponGrantRuleService couponGrantRuleService;
  @Autowired
  private CouponGrantService couponGrantService;
  @Autowired
  private UserService userService;
  @Autowired
  private LoanCouponConfigService loanCouponConfigService;
  @Autowired
  private FinancingCouponConfigService financingCouponConfigService;

  private static final Set<NotifCouponGrantTaskSourceType> SUPPORTED_SOURCE_SET = Sets.immutableEnumSet(NotifCouponGrantTaskSourceType.TELESALES, NotifCouponGrantTaskSourceType.CUSTOMER_SERVICE, NotifCouponGrantTaskSourceType.NOTIF);

  @PostMapping("/ecInternalApi/couponGrantRule/list")
  public EcCouponGrantRuleListResponse listCouponGrantRulesByIds(@NotNull @Valid @RequestBody EcCouponGrantRuleListRequest request) {
    List<CouponGrantRuleVO> grantRuleVOList = couponGrantRuleService.listByIds(request.ids);
    return grantRuleToResponse(grantRuleVOList);
  }

  @PostMapping("/ecInternalApi/couponGrantRule/grantCoupon")
  public EcCouponGrantResponse grantCoupon(@NotNull @Valid @RequestBody EcCouponGrantRequest request) {
    if (BooleanUtils.isFalse(SUPPORTED_SOURCE_SET.contains(request.sourceType))) {
      log.error("不支持的发券SourceType, requestBody:{}", JsonUtils.toString(request));
      return EcCouponGrantResponse.fail();
    }
    Long userId = userService.fetchUserIdByNormalizedMobileNumberAndSDKOrNull(request.normalizedMobileNumber, request.sdkType);
    if (Objects.isNull(userId)) {
      log.error("尚未查询到用户信息, requestBody:{}", JsonUtils.toString(request));
      return EcCouponGrantResponse.fail();
    }
    return doGrantCoupon(request, userId);
  }

  @PostMapping("/ecInternalApi/couponGrantRule/grantCouponByUserId")
  public EcCouponGrantResponse grantCoupon(@NotNull @Valid @RequestBody EcCouponGrantUserRequest request) {
    return doGrantCoupon(request, request.userId);
  }

  @PostMapping("/ecInternalApi/couponGrantRule/resolveCouponParams")
  public EcCouponGenerateParamResponse generateCouponParams(@NotNull @Valid @RequestBody EcCouponGrantRuleListRequest request) {
    Map<String, NotifParamVO> notifParamVOMap = couponGrantRuleService.generateMediumConfigMetadata(request.ids);
    return toParamResponse(notifParamVOMap, null);
  }

  @PostMapping("/ecInternalApi/couponGrantRule/resolveCouponParamsForUserId")
  public EcCouponGenerateParamResponse generateCouponParamsForUserId(@NotNull @Valid @RequestBody EcCouponGenerateParamForUserRequest request) {
    Map<Long, CouponGrantRuleVO> ruleIndex = couponGrantRuleService.listByIds(request.ids)
        .stream()
        .collect(Collectors.toMap(e -> e.id, e -> e));
    return doResolveParamForUserId(request, ruleIndex);
  }

  @PostMapping("/ecInternalApi/couponGrantRule/batchResolveCouponParamsForUserIds")
  public List<EcCouponGenerateParamResponse> batchResolveCouponParamsForUserId(@NotNull @Valid @RequestBody List<EcCouponGenerateParamForUserRequest> request) {
    List<Long> ruleIds = request.stream().flatMap(e -> e.ids.stream()).distinct().collect(Collectors.toList());
    Map<Long, CouponGrantRuleVO> ruleIndex = couponGrantRuleService.listByIds(ruleIds)
        .stream()
        .collect(Collectors.toMap(e -> e.id, e -> e));
    return request.parallelStream()
        .map(request1 -> doResolveParamForUserId(request1, ruleIndex))
        .collect(Collectors.toList());
  }

  @PostMapping("/ecInternalApi/couponGrantRule/searchCouponGrantRules")
  public EcCouponGrantRuleListResponse searchCouponGrantRules(@Valid @RequestBody EcCouponGrantRuleSearchRequest request) {
    CouponGrantRuleSearchCondition condition = new CouponGrantRuleSearchCondition();
    if (request.platformType != null) {
      condition.platformType = CouponGrantRulePlatformType.fromPlatform(request.platformType);
    }
    condition.ids = request.ids;
    if (request.valid != null) {
      condition.status = AvailabilityStatus.fromValidBoolean(request.valid.bool);
    }
    List<CouponGrantRuleVO> vos = couponGrantRuleService.listOrderedByConditionAndLimitAndPage(condition, 1000, 1);
    return grantRuleToResponse(vos);
  }

  private EcCouponGenerateParamResponse doResolveParamForUserId(EcCouponGenerateParamForUserRequest request, Map<Long, CouponGrantRuleVO> ruleIndex) {
    Map<String, NotifParamVO> mediumConfigMetadata = Maps.newHashMap();
    request.ids.forEach(id -> {
      CouponGrantRuleVO rule = ruleIndex.get(id);
      BaseCouponGrantRule<BaseRuleGrantConfig> baseRule = couponGrantRuleService.getRuleOrThrow(rule.ruleConfig.configType);
      mediumConfigMetadata.putAll(baseRule.generateMediumConfigUserMetadata(rule.ruleConfig.config, request.userId));
    });
    return toParamResponse(mediumConfigMetadata, request.userId);
  }

  private EcCouponGrantRuleListResponse grantRuleToResponse(List<CouponGrantRuleVO> grantRuleVOList) {
    if (CollectionUtils.isEmpty(grantRuleVOList)) {
      return EcCouponGrantRuleListResponse.empty();
    }
    List<Long> loanConfigIds = grantRuleVOList.stream().filter(CouponGrantRuleVO::isLoanRule).flatMap(e -> e.ruleConfig.config.fetchRelatedCouponConfigIds().stream()).distinct().collect(Collectors.toList());
    List<Long> financingConfigIds = grantRuleVOList.stream().filter(CouponGrantRuleVO::isFinancingRule).flatMap(e -> e.ruleConfig.config.fetchRelatedCouponConfigIds().stream()).distinct().collect(Collectors.toList());
    Map<Long, LoanCouponConfigVO> loanCouponConfigMap = loanCouponConfigService.mapByIds(loanConfigIds);
    Map<Long, FinancingCouponConfigVO> financingCouponConfigMap = financingCouponConfigService.mapByIds(financingConfigIds);
    List<EcCouponGrantRuleResponse> responseList = grantRuleVOList.stream().map(e -> convertToResponse(e, couponGrantRuleService.getRuleOrThrow(e.ruleConfigType), loanCouponConfigMap, financingCouponConfigMap)).collect(Collectors.toList());
    return EcCouponGrantRuleListResponse.from(responseList);
  }

  private EcCouponGenerateParamResponse toParamResponse(Map<String, NotifParamVO> notifParamVOMap, Long userId) {
    Map<String, NotifRawParamVO> map = notifParamVOMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> NotifRawParamVO.from(e.getValue().rawValue, e.getValue().formattedParamValue, NotifParamType.valueOf(e.getValue().paramType.fetchType().name()))));
    return new EcCouponGenerateParamResponse(map, userId);
  }

  private EcCouponGrantResponse doGrantCoupon(EcCouponGrantBaseRequest request, Long userId) {
    String batchNo = genBatchNo(request, userId);
    String extraData = StringUtils.isBlank(request.extraData) ? StringUtils.SPACE : request.extraData;
    try {
      List<Long> couponIds = couponGrantService.grantCouponByRuleId(
          request.ruleId,
          request.relatedId,
          request.sourceType,
          null,
          CouponGrantRulePlatformType.valueOf(request.platformType.name()),
          userId,
          batchNo,
          extraData
      );
      if (CollectionUtils.isEmpty(couponIds)) {
        log.info("发提额券命中营销发券实验, 发券失败，requestBody:{}", JsonUtils.toString(request));
        return EcCouponGrantResponse.fail();
      }
      return EcCouponGrantResponse.success(couponIds);
    } catch (Throwable e) {
      log.error("执行发券失败, requestBody:{}", JsonUtils.toString(request), e);
      return EcCouponGrantResponse.fail();
    }
  }

  private EcCouponGrantRuleResponse convertToResponse(
      CouponGrantRuleVO vo,
      BaseCouponGrantRule<BaseRuleGrantConfig> couponGrantRule,
      Map<Long, LoanCouponConfigVO> loanCouponConfigMap,
      Map<Long, FinancingCouponConfigVO> financingCouponConfigMap
  ) {
    EcCouponGrantRuleResponse response = new EcCouponGrantRuleResponse();
    response.id = vo.id;
    response.ruleConfigType = vo.ruleConfigType.name();
    response.supportedSourceTypeList = Lists.newArrayList(couponGrantRule.supportedSourceType()).stream().map(Enum::name).collect(Collectors.toList());
    response.config = JsonUtils.convertOrException(vo.ruleConfig.config, new TypeReference<Map<String, Object>>() {});
    response.ruleName = vo.ruleName;
    response.desc = vo.desc;
    response.valid = vo.status == AvailabilityStatus.ENABLED ? BooleanType.TRUE : BooleanType.FALSE;
    response.platformType = PlatformType.valueOf(vo.platformType.name());
    response.operator = vo.operator;
    response.timeCreated = vo.timeCreated;
    response.timeUpdated = vo.timeUpdated;
    response.supportedUsageTypeList = couponGrantRule.supportedUsageType().stream().map(ICouponUsageType::name).collect(Collectors.toList());
    switch (response.platformType) {
      case LOAN:
        response.supportedSdkList = vo.ruleConfig.config.configIdsToSDKTypeList(e -> loanCouponConfigMap.get(e).sdkType).stream().map(Enum::name).collect(Collectors.toList());
        response.relatedConfigIdToCouponName = vo.ruleConfig.config.fetchRelatedCouponConfigIds().stream().collect(Collectors.toMap(e -> e, e -> loanCouponConfigMap.get(e).name));
        response.relatedUnresolvedCouponDescription = vo.ruleConfig.config.fetchRelatedCouponConfigIds().stream().collect(Collectors.toMap(e -> e, e -> StringUtils.isBlank(loanCouponConfigMap.get(e).description) ? StringUtils.EMPTY : loanCouponConfigMap.get(e).description));
        break;
      case FINANCING:
        response.supportedSdkList = vo.ruleConfig.config.configIdsToSDKTypeList(e -> financingCouponConfigMap.get(e).sdkType).stream().map(Enum::name).collect(Collectors.toList());
        response.relatedConfigIdToCouponName = vo.ruleConfig.config.fetchRelatedCouponConfigIds().stream().collect(Collectors.toMap(e -> e, e -> financingCouponConfigMap.get(e).name));
        response.relatedUnresolvedCouponDescription = vo.ruleConfig.config.fetchRelatedCouponConfigIds().stream().collect(Collectors.toMap(e -> e, e -> StringUtils.isBlank(financingCouponConfigMap.get(e).description) ? StringUtils.EMPTY : financingCouponConfigMap.get(e).description));
        break;
      default:
        throw EcException.error("Unsupported platform type. {}", response.platformType);
    }
    response.supportedParams = couponGrantRuleService.fetchSupportParamList(Collections.singletonList(vo.id)).stream().map(e -> new EcNotifParamField(e.getParamFieldName(), e.getParamDesc(), e.fetchType())).collect(Collectors.toList());
    response.necessaryParams = couponGrantRuleService.fetchNecessaryParams(Collections.singletonList(vo.id)).stream().map(e -> new EcNotifParamField(e.getParamFieldName(), e.getParamDesc(), e.fetchType())).collect(Collectors.toList());
    return response;
  }

  private String genBatchNo(EcCouponGrantBaseRequest request, Long userId) {
    if (StringUtils.isNotBlank(request.batchNo)) {
      return request.batchNo;
    }
    return Clock.now() + "," + userId + "_" + request.platformType.name() + "_" + request.sourceType.name();
  }
}
