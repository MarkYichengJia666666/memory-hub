package com.miyou.controllers.cashloan.newhomepage.alert.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.response.v5.alert.AlertResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.alert.AlertDisplayTrackingLoader;
import com.yqg.core.service.cashloan.alert.FraudAlertService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;

/**
 * 还款引导实验处理器
 * 实验条件：有在贷订单且最新放款1-8天内
 * 实验组：展示banner24H，并在/api/cashloan/instalmentList内设置fraudAlert
 * 对照组：不做处理
 */
@Component
@Slf4j
public class RepaymentGuidanceAlertProcessor extends AbstractAlertInfoProcessor {

  private static final String EXPERIMENT_KEY = "technology-repayment-abroad-loan_all-repayment_prompt_new";

  @Autowired
  private FraudAlertService fraudAlertService;

  @Autowired
  private HomepageV5Config homepageV5Config;

  @Override
  protected AlertProcessorType getAlertProcessorType() {
    return AlertProcessorType.REPAYMENT_GUIDANCE_ALERT_PROCESSOR;
  }

  @Override
  public void process(AlertResponse alertResponse, HomePageContext homePageContext) {
    try {
      Long userId = homePageContext.getUserId();
      if (userId == null) {
        return;
      }
      if(!homepageV5Config.getHomePageFraudAlertSwitch()){
        return;
      }
      // 有在贷订单且最新放款1-8天内
      if (!fraudAlertService.checkExperimentConditionWithin1To8Days(userId) && !homepageV5Config.getHomePageFraudAlertWhiteList().contains(homePageContext.getUserId())) {
        return;
      }
      if (!fraudAlertService.getExpResultAndDiversion(EXPERIMENT_KEY)) {
        return;
      }
      Long orderId = fraudAlertService.canShowAlertBannerAndRecord(homePageContext.getUserId(), EXPERIMENT_KEY);
      if (Objects.nonNull(orderId)) {
        alertResponse.setTitle(TT.gen("防诈骗警示告知"));
        alertResponse.setContent(fraudAlertService.generateAlertMessage());
        alertResponse.setExperimentKey(EXPERIMENT_KEY);
        alertResponse.setShowPopup(true);
        alertResponse.setShowAlert(true);
      }
    } catch (Exception e) {
      log.error("RepaymentGuidanceAlertProcessor error: userId={}", homePageContext.getUserId(), e);
    }
  }

}