package com.miyou.controllers.cashloan.newhomepage.alert.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.response.v5.alert.AlertResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.alert.AlertDisplayTrackingLoader;
import com.yqg.core.service.cashloan.alert.FraudAlertService;
import com.yqg.core.service.cashloan.homepage.config.HomePagePopupConfig;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.mc.MarketingCenterClientService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.FraudAlertParam;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;

/**
 * 借款引导实验处理器
 * 实验条件：有在贷订单且最新放款24小时内
 * 实验组：展示banner24H，并弹窗资源位1次
 * 对照组：不做处理
 */
@Component
@Slf4j
public class LendingGuidanceAlertProcessor extends AbstractAlertInfoProcessor {

  private static final String EXPERIMENT_KEY = "technology-lending-abroad-loan_all-loan_prompt_new";

  @Autowired
  private FraudAlertService fraudAlertService;

  @Autowired
  private MarketingCenterClientService marketingCenterClientService;

  @Autowired
  private HomepageV5Config homepageV5Config;

  @Override
  protected AlertProcessorType getAlertProcessorType() {
    return AlertProcessorType.LENDING_GUIDANCE_ALERT_PROCESSOR;
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
      if (Objects.isNull(homePageContext.getUserCashLoanOrderContext()) || CollectionUtils.isEmpty(homePageContext.getUserCashLoanOrderContext().getReadyOrderList())) {
        return;
      }

      if(!fraudAlertService.checkLendingExpCondition(homePageContext.getUserId(),homePageContext.getLoanAccountId()) && !homepageV5Config.getHomePageFraudAlertWhiteList().contains(homePageContext.getUserId())){
        return;
      }

      if (!fraudAlertService.getExpResultAndDiversion(EXPERIMENT_KEY)) {
        return;
      }

      Long orderId = fraudAlertService.canShowAlertBannerAndRecord(homePageContext.getUserId(), EXPERIMENT_KEY);
      // 检查是否可以显示警告提示（基于订单的24小时展示窗口）
      if (Objects.nonNull(orderId)) {
        alertResponse.setTitle(TT.gen("防诈骗警示告知"));
        alertResponse.setContent(fraudAlertService.generateAlertMessage());
        alertResponse.setExperimentKey(EXPERIMENT_KEY);
        alertResponse.setShowPopup(true);
        alertResponse.setShowAlert(true);

        marketingCenterClientService.publishSystemEvent(SystemNotifScene.FRAUD_ALERT,new FraudAlertParam(homePageContext.getSdkType(), homePageContext.getUserId(), orderId));
      }
    } catch (Exception e) {
      log.error("LendingGuidanceAlertProcessor error: userId={}", homePageContext.getUserId(), e);
    }

  }


}