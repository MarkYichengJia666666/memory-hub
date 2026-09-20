package com.yqg.core.service.general.pageconfig.filterstrategy.processor;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.experiment.NationalDay817ExpService;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.payload.EmptyFilterRulePayload;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 印尼 817 国庆端内氛围 — 首页活动 banner 展示规则
 */
@Slf4j
@Service
public class NationalDay817HomeBannerFilterRuleProcessor
    extends GeneralPageConfigBaseFilterRuleProcessor<EmptyFilterRulePayload> {

  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private NationalDay817ExpService nationalDay817ExpService;

  @Override
  public GeneralPageConfigFilterRuleType getRuleType() {
    return GeneralPageConfigFilterRuleType.NATIONAL_DAY_817_HOME_BANNER;
  }

  @Override
  public void checkPayload(EmptyFilterRulePayload payload) {
    // 无 MC 侧 payload 参数
  }

  @Override
  public boolean hitRule(EmptyFilterRulePayload payload, GeneralPageConfigParam param) {
    if (Objects.isNull(param) || Objects.isNull(param.userId)) {
      log.info("[NationalDay817HomeBanner] hitRule false: userId is null");
      return false;
    }
    // 渠道 / 开关 / 活动期门控必须先于入组
    if (!nationalDay817ExpService.passesGate(param.userId)) {
      return false;
    }
    IDNHomepageLoanStatusV5 statusV5 = param.idnHomepageLoanStatusV5 != null
        ? param.idnHomepageLoanStatusV5 : homepageStatusTool.getStatusByUserId(param.userId, param.build, param.sdkType);
    if (statusV5 == null || !statusV5.canCreateOrder()) {
      log.info("[NationalDay817HomeBanner] hitRule false: cannot create order, userId={}, status={}",
          param.userId, statusV5);
      return false;
    }
    boolean hit = nationalDay817ExpService.isAtmosphereGroup(param.userId);
    log.info("[NationalDay817HomeBanner] hitRule {}: userId={}", hit, param.userId);
    return hit;
  }
}
